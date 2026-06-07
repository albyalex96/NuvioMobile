package com.nuvio.app.features.downloads

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.getString
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.net.URI
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

private val downloadHttpClient = OkHttpClient.Builder()
    .dns(com.nuvio.app.core.network.AndroidDnsProvider)
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(60, TimeUnit.SECONDS)
    .writeTimeout(60, TimeUnit.SECONDS)
    .followRedirects(true)
    .followSslRedirects(true)
    .build()

internal actual object DownloadsPlatformDownloader {
    private var appContext: Context? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    actual fun start(
        request: DownloadPlatformRequest,
        onProgress: (downloadedBytes: Long, totalBytes: Long?) -> Unit,
        onSuccess: (localFileUri: String, totalBytes: Long?) -> Unit,
        onFailure: (message: String) -> Unit,
    ): DownloadsTaskHandle {
        val job = SupervisorJob()
        val scope = CoroutineScope(job + Dispatchers.IO)
        var call: Call? = null

        scope.launch {
            val context = appContext
            if (context == null) {
                onFailure(runBlocking { getString(Res.string.downloads_error_not_initialized) })
                return@launch
            }

            val downloadsDir = File(context.filesDir, "downloads").apply { mkdirs() }
            val destination = File(downloadsDir, request.destinationFileName)
            val tempFile = File(downloadsDir, "${request.destinationFileName}.part")

            try {
                var resumeFromBytes = tempFile.takeIf { it.exists() }?.length()?.coerceAtLeast(0L) ?: 0L

                fun buildRequest(rangeStart: Long?): Request {
                    val requestBuilder = Request.Builder().url(request.sourceUrl)
                    request.sourceHeaders.forEach { (key, value) ->
                        requestBuilder.header(key, value)
                    }
                    if (rangeStart != null && rangeStart > 0L) {
                        requestBuilder.header("Range", "bytes=$rangeStart-")
                    }
                    return requestBuilder.get().build()
                }

                var attemptedRangeRequest = resumeFromBytes > 0L
                var httpRequest = buildRequest(if (attemptedRangeRequest) resumeFromBytes else null)
                call = downloadHttpClient.newCall(httpRequest)
                var response = call?.execute() ?: error(
                    runBlocking { getString(Res.string.downloads_error_request_failed) },
                )

                if (attemptedRangeRequest && response.code == 416) {
                    response.close()
                    tempFile.delete()
                    resumeFromBytes = 0L
                    attemptedRangeRequest = false
                    httpRequest = buildRequest(null)
                    call = downloadHttpClient.newCall(httpRequest)
                    response = call?.execute() ?: error(
                        runBlocking { getString(Res.string.downloads_error_request_failed) },
                    )
                }

                response.use { response ->
                    if (!response.isSuccessful) {
                        error(
                            runBlocking {
                                getString(Res.string.downloads_error_http_failed, response.code)
                            },
                        )
                    }

                    val isPartialResume = attemptedRangeRequest && response.code == 206 && resumeFromBytes > 0L
                    val appendToTemp = isPartialResume
                    val startingBytes = if (appendToTemp) resumeFromBytes else 0L

                    if (!appendToTemp && tempFile.exists()) {
                        tempFile.delete()
                    }

                    val body = response.body ?: error(
                        runBlocking { getString(Res.string.downloads_error_empty_body) },
                    )
                    val totalBytes = resolveTotalBytes(
                        startingBytes = startingBytes,
                        isPartialResume = isPartialResume,
                        contentRangeHeader = response.header("Content-Range"),
                        contentLength = body.contentLength().takeIf { it > 0L },
                    )
                    var downloadedBytes = startingBytes
                    onProgress(downloadedBytes, totalBytes)

                    body.byteStream().use { input ->
                        FileOutputStream(tempFile, appendToTemp).use { output ->
                            val buffer = ByteArray(16 * 1024)
                            while (true) {
                                ensureActive()
                                val read = input.read(buffer)
                                if (read <= 0) break
                                output.write(buffer, 0, read)
                                downloadedBytes += read.toLong()
                                onProgress(downloadedBytes, totalBytes)
                            }
                            output.flush()
                        }
                    }

                    if (destination.exists()) {
                        destination.delete()
                    }
                    if (!tempFile.renameTo(destination)) {
                        tempFile.copyTo(destination, overwrite = true)
                        tempFile.delete()
                    }

                    val finalSize = destination.length()
                    onSuccess(destination.toURI().toString(), totalBytes ?: finalSize)
                }
            } catch (error: Throwable) {
                onFailure(error.message ?: runBlocking { getString(Res.string.download_failed) })
            }
        }

        job.invokeOnCompletion {
            call?.cancel()
        }

        return AndroidDownloadsTaskHandle(job)
    }

    actual fun removeFile(localFileUri: String?): Boolean {
        if (localFileUri.isNullOrBlank()) return false
        val file = localFileUri.toLocalFileOrNull() ?: return false
        return runCatching { file.delete() }.getOrDefault(false)
    }

    actual fun removePartialFile(destinationFileName: String): Boolean {
        val context = appContext ?: return false
        val downloadsDir = File(context.filesDir, "downloads")
        val tempFile = File(downloadsDir, "$destinationFileName.part")
        if (!tempFile.exists()) return true
        return runCatching { tempFile.delete() }.getOrDefault(false)
    }

    actual fun resolveLocalFileUri(localFileUri: String?, destinationFileName: String): String? {
        localFileUri
            ?.toLocalFileOrNull()
            ?.takeIf { it.exists() }
            ?.let { return it.toURI().toString() }

        val context = appContext ?: return null
        val fileName = destinationFileName.trim().takeIf { it.isNotBlank() }
            ?: localFileUri
                ?.toLocalFileOrNull()
                ?.name
                ?.takeIf { it.isNotBlank() }
            ?: return null
        val downloadsDir = File(context.filesDir, "downloads")
        val localFile = File(downloadsDir, fileName)
        return localFile.takeIf { it.exists() }?.toURI()?.toString()
    }

    actual fun fetchUrlAsString(url: String, headers: Map<String, String>): String? {
        return try {
            val requestBuilder = Request.Builder().url(url)
            headers.forEach { (key, value) ->
                requestBuilder.header(key, value)
            }
            val response = downloadHttpClient.newCall(requestBuilder.get().build()).execute()
            response.use { resp ->
                if (resp.isSuccessful) {
                    resp.body?.string()
                } else null
            }
        } catch (_: Exception) {
            null
        }
    }

    actual fun probeHlsContentType(url: String, headers: Map<String, String>): Boolean {
        return try {
            val requestBuilder = Request.Builder().url(url).head()
            headers.forEach { (key, value) ->
                requestBuilder.header(key, value)
            }
            val response = downloadHttpClient.newCall(requestBuilder.build()).execute()
            response.use { resp ->
                if (resp.isSuccessful) {
                    val contentType = resp.header("Content-Type")
                    HlsPlaylistParser.isHlsContentType(contentType)
                } else false
            }
        } catch (_: Exception) {
            false
        }
    }

    actual fun downloadHlsSegments(
        segments: List<HlsSegment>,
        keyCache: Map<String, ByteArray>,
        sourceHeaders: Map<String, String>,
        destinationFileName: String,
        onProgress: (downloadedBytes: Long, totalBytes: Long?) -> Unit,
        onSuccess: (localFileUri: String, totalBytes: Long?) -> Unit,
        onFailure: (message: String) -> Unit,
    ): DownloadsTaskHandle {
        val job = SupervisorJob()
        val scope = CoroutineScope(job + Dispatchers.IO)

        scope.launch {
            val context = appContext
            if (context == null) {
                onFailure(runBlocking { getString(Res.string.downloads_error_not_initialized) })
                return@launch
            }

            val downloadsDir = File(context.filesDir, "downloads").apply { mkdirs() }
            val destination = File(downloadsDir, destinationFileName)
            val tempFile = File(downloadsDir, "${destinationFileName}.hls.part")
            var totalDownloaded = 0L

            try {
                if (tempFile.exists()) tempFile.delete()
                tempFile.createNewFile()

                FileOutputStream(tempFile, true).use { output ->
                    for (batch in segments.chunked(4)) {
                        val deferreds = batch.map { segment ->
                            scope.async(Dispatchers.IO) {
                                try {
                                    val requestBuilder = Request.Builder().url(segment.url)
                                    sourceHeaders.forEach { (key, value) ->
                                        requestBuilder.header(key, value)
                                    }
                                    val response = downloadHttpClient.newCall(requestBuilder.get().build()).execute()
                                    response.use { resp ->
                                        if (!resp.isSuccessful) return@use null
                                        val body = resp.body ?: return@use null
                                        val segmentBytes = body.bytes()
                                        if (segment.key != null) {
                                            val keyBytes = keyCache[segment.key.uri]
                                            val ivBytes = segment.key.toIvBytes()
                                            if (keyBytes != null && ivBytes != null) {
                                                decryptAes128Cbc(segmentBytes, keyBytes, ivBytes)
                                            } else segmentBytes
                                        } else segmentBytes
                                    }
                                } catch (_: Exception) { null }
                            }
                        }
                        for (deferred in deferreds) {
                            val bytes = deferred.await()
                            if (bytes != null) {
                                output.write(bytes)
                                totalDownloaded += bytes.size.toLong()
                                onProgress(totalDownloaded, null)
                                output.flush()
                            }
                        }
                    }
                }

                if (destination.exists()) destination.delete()
                if (!tempFile.renameTo(destination)) {
                    tempFile.copyTo(destination, overwrite = true)
                    tempFile.delete()
                }

                val finalSize = destination.length()
                onSuccess(destination.toURI().toString(), finalSize)
            } catch (error: Throwable) {
                onFailure(error.message ?: runBlocking { getString(Res.string.download_failed) })
            }
        }

        job.invokeOnCompletion {
            // cleanup handled by the coroutine
        }

        return AndroidDownloadsTaskHandle(job)
    }

    actual fun fetchUrlAsBytes(url: String, headers: Map<String, String>): ByteArray? {
        return try {
            val requestBuilder = Request.Builder().url(url)
            headers.forEach { (key, value) ->
                requestBuilder.header(key, value)
            }
            val response = downloadHttpClient.newCall(requestBuilder.get().build()).execute()
            response.use { resp ->
                if (resp.isSuccessful) {
                    resp.body?.bytes()
                } else null
            }
        } catch (_: Exception) {
            null
        }
    }

    actual fun downloadSegmentsToFile(
        segments: List<HlsSegment>,
        keyCache: Map<String, ByteArray>,
        headers: Map<String, String>,
        fileName: String,
    ): String? {
        val context = appContext ?: return null
        return try {
            val downloadsDir = File(context.filesDir, "downloads").apply { mkdirs() }
            val file = File(downloadsDir, fileName)
            val tempFile = File(downloadsDir, "$fileName.track.part")
            if (tempFile.exists()) tempFile.delete()

            runBlocking(Dispatchers.IO) {
                FileOutputStream(tempFile, false).use { output ->
                    for (batch in segments.chunked(4)) {
                        val deferreds = batch.map { segment ->
                            async(Dispatchers.IO) {
                                try {
                                    val requestBuilder = Request.Builder().url(segment.url)
                                    headers.forEach { (key, value) ->
                                        requestBuilder.header(key, value)
                                    }
                                    val response = downloadHttpClient.newCall(requestBuilder.get().build()).execute()
                                    response.use { resp ->
                                        if (!resp.isSuccessful) return@use null
                                        val body = resp.body ?: return@use null
                                        val segmentBytes = body.bytes()
                                        if (segment.key != null) {
                                            val keyBytes = keyCache[segment.key.uri]
                                            val ivBytes = segment.key.toIvBytes()
                                            if (keyBytes != null && ivBytes != null) {
                                                decryptAes128Cbc(segmentBytes, keyBytes, ivBytes)
                                            } else segmentBytes
                                        } else segmentBytes
                                    }
                                } catch (_: Exception) { null }
                            }
                        }
                        for (deferred in deferreds) {
                            deferred.await()?.let { output.write(it) }
                        }
                        output.flush()
                    }
                }
            }

            if (file.exists()) file.delete()
            if (!tempFile.renameTo(file)) {
                tempFile.copyTo(file, overwrite = true)
                tempFile.delete()
            }
            file.toURI().toString()
        } catch (_: Exception) {
            null
        }
    }

    actual fun remuxToMp4(videoUri: String, audioUri: String?, outputFileName: String): String? {
        val context = appContext ?: return null
        if (audioUri == null) return videoUri

        return try {
            val downloadsDir = File(context.filesDir, "downloads").apply { mkdirs() }
            val outputFile = File(downloadsDir, outputFileName)
            if (outputFile.exists()) outputFile.delete()

            val videoExtractor = MediaExtractor()
            videoExtractor.setDataSource(context, Uri.parse(videoUri), null)

            val audioExtractor = MediaExtractor()
            audioExtractor.setDataSource(context, Uri.parse(audioUri), null)

            var videoTrackIndex = -1
            var videoTrackFormat: MediaFormat? = null
            for (i in 0 until videoExtractor.trackCount) {
                val format = videoExtractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("video/")) {
                    videoTrackIndex = i
                    videoTrackFormat = format
                    break
                }
            }

            var audioTrackIndex = -1
            var audioTrackFormat: MediaFormat? = null
            for (i in 0 until audioExtractor.trackCount) {
                val format = audioExtractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    audioTrackFormat = format
                    break
                }
            }

            if (videoTrackIndex < 0) {
                videoExtractor.release()
                audioExtractor.release()
                return null
            }

            if (audioTrackIndex < 0) {
                videoExtractor.release()
                audioExtractor.release()
                return null
            }

            videoExtractor.selectTrack(videoTrackIndex)
            audioExtractor.selectTrack(audioTrackIndex)

            val muxer = MediaMuxer(
                outputFile.absolutePath,
                MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4,
            )

            val muxedVideoTrack = muxer.addTrack(videoTrackFormat!!)
            val muxedAudioTrack = muxer.addTrack(audioTrackFormat!!)

            muxer.start()

            val buffer = ByteBuffer.allocate(1_048_576)
            val info = MediaCodec.BufferInfo()

            while (true) {
                val readSize = videoExtractor.readSampleData(buffer, 0)
                if (readSize < 0) break
                info.set(0, readSize, videoExtractor.sampleTime, videoExtractor.sampleFlags)
                muxer.writeSampleData(muxedVideoTrack, buffer, info)
                videoExtractor.advance()
            }

            while (true) {
                val readSize = audioExtractor.readSampleData(buffer, 0)
                if (readSize < 0) break
                info.set(0, readSize, audioExtractor.sampleTime, audioExtractor.sampleFlags)
                muxer.writeSampleData(muxedAudioTrack, buffer, info)
                audioExtractor.advance()
            }

            muxer.stop()
            muxer.release()
            videoExtractor.release()
            audioExtractor.release()

            outputFile.toURI().toString()
        } catch (_: Exception) {
            null
        }
    }
}

private class AndroidDownloadsTaskHandle(
    private val job: Job,
) : DownloadsTaskHandle {
    override fun cancel() {
        job.cancel()
    }
}

private fun String.toLocalFileOrNull(): File? {
    return runCatching {
        if (startsWith("file:")) {
            File(URI(this))
        } else {
            File(this)
        }
    }.getOrNull()
}

private fun resolveTotalBytes(
    startingBytes: Long,
    isPartialResume: Boolean,
    contentRangeHeader: String?,
    contentLength: Long?,
): Long? {
    parseContentRangeTotal(contentRangeHeader)?.let { return it }
    val normalizedLength = contentLength?.takeIf { it > 0L } ?: return null
    return if (isPartialResume && startingBytes > 0L) {
        startingBytes + normalizedLength
    } else {
        normalizedLength
    }
}

private fun parseContentRangeTotal(headerValue: String?): Long? {
    val value = headerValue?.trim().orEmpty()
    if (value.isBlank()) return null
    val slashIndex = value.lastIndexOf('/')
    if (slashIndex == -1 || slashIndex == value.lastIndex) return null
    val totalPart = value.substring(slashIndex + 1).trim()
    if (totalPart == "*") return null
    return totalPart.toLongOrNull()?.takeIf { it > 0L }
}

private fun decryptAes128Cbc(data: ByteArray, keyBytes: ByteArray, ivBytes: ByteArray): ByteArray {
    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    val keySpec = SecretKeySpec(keyBytes, "AES")
    val ivSpec = IvParameterSpec(ivBytes)
    cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
    return cipher.doFinal(data)
}
