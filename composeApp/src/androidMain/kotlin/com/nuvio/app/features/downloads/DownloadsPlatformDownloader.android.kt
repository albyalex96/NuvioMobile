package com.nuvio.app.features.downloads

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaMuxer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.getString
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
        context: HlsDownloadContext,
        onProgress: (downloadedBytes: Long, totalBytes: Long?) -> Unit,
        onSuccess: (localFileUri: String, totalBytes: Long?) -> Unit,
        onFailure: (message: String) -> Unit,
    ): DownloadsTaskHandle {
        val job = SupervisorJob()
        val scope = CoroutineScope(job + Dispatchers.IO)

        scope.launch {
            val appCtx = appContext
            if (appCtx == null) {
                onFailure(runBlocking { getString(Res.string.downloads_error_not_initialized) })
                return@launch
            }

            val downloadsDir = File(appCtx.filesDir, "downloads").apply { mkdirs() }
            val destination = File(downloadsDir, context.destinationFileName)
            val tsTemp = File(downloadsDir, "${context.destinationFileName}.hls.part")
            var totalDownloaded = 0L

            try {
                if (tsTemp.exists()) tsTemp.delete()
                tsTemp.createNewFile()

                FileOutputStream(tsTemp, false).use { output ->
                    if (context.mapInitSegment != null) {
                        output.write(context.mapInitSegment)
                        totalDownloaded += context.mapInitSegment.size
                        onProgress(totalDownloaded, null)
                    }

                    for ((index, spec) in context.segments.withIndex()) {
                        ensureActive()

                        val segData = fetchUrlAsBytes(spec.url, context.sourceHeaders)
                            ?: error(runBlocking { getString(Res.string.downloads_error_empty_body) })

                        val decrypted = if (spec.keyIndex != null && spec.keyIndex < context.keyDataList.size) {
                            val key = context.keyDataList[spec.keyIndex]
                            val iv = context.keyIvList.getOrNull(spec.keyIndex)
                                ?: deriveIvFromIndex(index)
                            segData.aes128CbcDecrypt(key, iv)
                        } else {
                            segData
                        }

                        output.write(decrypted)
                        output.flush()
                        totalDownloaded += decrypted.size
                        onProgress(totalDownloaded, null)
                    }
                }

                val isFmp4 = context.mapInitSegment != null
                val finalFile: File = if (!isFmp4) {
                    val mp4File = File(downloadsDir, context.destinationFileName)
                    if (remuxTsToMp4(tsTemp.absolutePath, mp4File.absolutePath)) {
                        tsTemp.delete()
                        mp4File
                    } else {
                        val tsName = context.destinationFileName.removeSuffix(".mp4") + ".ts"
                        val tsFile = File(downloadsDir, tsName)
                        if (tsFile.exists()) tsFile.delete()
                        if (!tsTemp.renameTo(tsFile)) {
                            tsTemp.copyTo(tsFile, overwrite = true)
                            tsTemp.delete()
                        }
                        tsFile
                    }
                } else {
                    if (destination.exists()) destination.delete()
                    if (!tsTemp.renameTo(destination)) {
                        tsTemp.copyTo(destination, overwrite = true)
                        tsTemp.delete()
                    }
                    destination
                }

                val finalSize = finalFile.length()
                onSuccess(finalFile.toURI().toString(), finalSize)
            } catch (error: Throwable) {
                onFailure(error.message ?: runBlocking { getString(Res.string.download_failed) })
            }
        }

        job.invokeOnCompletion {
            // cleanup handled by the coroutine
        }

        return AndroidDownloadsTaskHandle(job)
    }
}

internal actual fun ByteArray.aes128CbcDecrypt(key: ByteArray, iv: ByteArray): ByteArray {
    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
    return cipher.doFinal(this)
}

internal actual fun remuxTsToMp4(inputPath: String, outputPath: String): Boolean {
    return try {
        val extractor = MediaExtractor()
        extractor.setDataSource(inputPath)
        val muxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

        val trackMap = mutableListOf<Pair<Int, Int>>() // extractor track → muxer track
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val muxerTrack = muxer.addTrack(format)
            trackMap.add(i to muxerTrack)
        }

        muxer.start()
        val buffer = ByteBuffer.allocate(1024 * 1024)
        val info = MediaCodec.BufferInfo()

        for ((extractorTrack, muxerTrack) in trackMap) {
            extractor.selectTrack(extractorTrack)
            while (true) {
                buffer.rewind()
                info.set(0, 0, 0, 0)
                val sampleSize = extractor.readSampleData(buffer, 0)
                if (sampleSize < 0) break
                info.set(0, sampleSize, extractor.sampleTime, extractor.sampleFlags)
                buffer.limit(sampleSize)
                muxer.writeSampleData(muxerTrack, buffer, info)
                extractor.advance()
            }
            extractor.unselectTrack(extractorTrack)
        }

        muxer.stop()
        muxer.release()
        extractor.release()
        true
    } catch (_: Exception) {
        false
    }
}

private fun deriveIvFromIndex(index: Int): ByteArray {
    val iv = ByteArray(16)
    var idx = index
    for (i in 15 downTo 0) {
        iv[i] = (idx and 0xFF).toByte()
        idx = idx ushr 8
    }
    return iv
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
