package com.nuvio.app.features.downloads

import android.content.Context
import android.content.Intent
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
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
import java.io.InputStream
import java.io.OutputStream
import java.net.URI
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import android.util.Log as AndroidLog
import com.nuvio.app.core.logging.InAppLogger
import kotlin.coroutines.coroutineContext

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

    private fun resolveTarget(context: Context, uriString: String): DownloadTarget? {
        val uri = Uri.parse(uriString)
        return if (uri.scheme == "content") {
            val doc = DocumentFile.fromSingleUri(context, uri) ?: return null
            DocumentSingleTarget(context, doc)
        } else {
            val file = if (uriString.startsWith("file:")) {
                File(URI(uriString))
            } else {
                File(uriString)
            }
            FileDownloadTarget(file)
        }
    }

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    actual fun start(
        request: DownloadPlatformRequest,
        onProgress: (downloadedBytes: Long, totalBytes: Long?) -> Unit,
        onSuccess: (localFileUri: String, totalBytes: Long?, companion: HlsCompanionOutcome?) -> Unit,
        onFailure: (message: String) -> Unit,
        onWarning: ((message: String) -> Unit)?,
    ): DownloadsTaskHandle {
        val job = SupervisorJob()
        val scope = CoroutineScope(job + Dispatchers.IO)

        if (request.isHlsStream) {
            scope.launch {
                val context = appContext
                if (context == null) {
                    onFailure(runBlocking { getString(Res.string.downloads_error_not_initialized) })
                    return@launch
                }
                performHlsDownloadAndroid(
                    context = context,
                    request = request,
                    onProgress = onProgress,
                    onSuccess = onSuccess,
                    onFailure = onFailure,
                    onWarning = onWarning,
                )
            }
            return AndroidDownloadsTaskHandle(job)
        }

        var call: Call? = null

        scope.launch {
            val context = appContext
            if (context == null) {
                onFailure(runBlocking { getString(Res.string.downloads_error_not_initialized) })
                return@launch
            }

            DownloadsSettingsRepository.ensureLoaded()
            val customLocationUriString = DownloadsSettingsRepository.downloadLocationUri.value
            val customLocationUri = customLocationUriString?.let { Uri.parse(it) }

            val destination: DownloadTarget
            val tempFile: DownloadTarget

            if (customLocationUri != null && customLocationUri.scheme == "content") {
                val tree = DocumentFile.fromTreeUri(context, customLocationUri)
                if (tree == null || !tree.canWrite()) {
                    onFailure(runBlocking { getString(Res.string.downloads_error_cannot_write_location) })
                    return@launch
                }
                destination = DocumentDownloadTarget(context, tree, request.destinationFileName)
                tempFile = DocumentDownloadTarget(context, tree, "${request.destinationFileName}.part")
            } else {
                val downloadsDir = File(context.filesDir, "downloads").apply { mkdirs() }
                destination = FileDownloadTarget(File(downloadsDir, request.destinationFileName))
                tempFile = FileDownloadTarget(File(downloadsDir, "${request.destinationFileName}.part"))
            }

            try {
                var resumeFromBytes = if (tempFile.exists()) tempFile.length().coerceAtLeast(0L) else 0L

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
                        tempFile.openOutputStream(appendToTemp).use { output ->
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
                        tempFile.copyTo(destination)
                        tempFile.delete()
                    }

                    val finalSize = destination.length()
                    onSuccess(destination.toUriString(), totalBytes ?: finalSize, null)
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
        val context = appContext ?: return false
        val target = resolveTarget(context, localFileUri) ?: return false
        return target.delete()
    }

    actual fun removePartialFile(destinationFileName: String): Boolean {
        val context = appContext ?: return false
        DownloadsSettingsRepository.ensureLoaded()
        val customLocationUriString = DownloadsSettingsRepository.downloadLocationUri.value
        val customLocationUri = customLocationUriString?.let { Uri.parse(it) }

        val tempFile: DownloadTarget = if (customLocationUri != null && customLocationUri.scheme == "content") {
            val tree = DocumentFile.fromTreeUri(context, customLocationUri) ?: return false
            DocumentDownloadTarget(context, tree, "$destinationFileName.part")
        } else {
            val downloadsDir = File(context.filesDir, "downloads")
            FileDownloadTarget(File(downloadsDir, "$destinationFileName.part"))
        }

        if (!tempFile.exists()) return true
        return tempFile.delete()
    }

    actual fun resolveLocalFileUri(localFileUri: String?, destinationFileName: String): String? {
        val context = appContext ?: return null
        if (!localFileUri.isNullOrBlank()) {
            val target = resolveTarget(context, localFileUri)
            if (target?.exists() == true) {
                return target.toUriString()
            }
        }

        val fileName = destinationFileName.trim().takeIf { it.isNotBlank() }
            ?: localFileUri?.let { Uri.parse(it).lastPathSegment }
            ?: return null

        DownloadsSettingsRepository.ensureLoaded()
        val customLocationUriString = DownloadsSettingsRepository.downloadLocationUri.value
        val customLocationUri = customLocationUriString?.let { Uri.parse(it) }

        val localFileTarget: DownloadTarget = if (customLocationUri != null && customLocationUri.scheme == "content") {
            val tree = DocumentFile.fromTreeUri(context, customLocationUri) ?: return null
            DocumentDownloadTarget(context, tree, fileName)
        } else {
            val downloadsDir = File(context.filesDir, "downloads")
            FileDownloadTarget(File(downloadsDir, fileName))
        }

        return localFileTarget.takeIf { it.exists() }?.toUriString()
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

    actual fun openDownloadsDirectory(): Boolean {
        val context = appContext ?: return false
        val downloadsDir = File(context.filesDir, "downloads").apply { mkdirs() }
        val uri = runCatching {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                downloadsDir,
            )
        }.getOrNull() ?: return false

        val intents = listOf(
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "resource/folder")
            },
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "vnd.android.document/directory")
            },
            Intent(Intent.ACTION_VIEW).apply {
                data = uri
            },
        )

        return intents.any { intent ->
            intent.addCategory(Intent.CATEGORY_DEFAULT)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.addFlags(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)

            runCatching {
                context.startActivity(intent)
                true
            }.getOrDefault(false)
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

private interface DownloadTarget {
    fun exists(): Boolean
    fun length(): Long
    fun delete(): Boolean
    fun openOutputStream(append: Boolean): OutputStream
    fun toUriString(): String
    fun renameTo(other: DownloadTarget): Boolean
    fun copyTo(other: DownloadTarget)
}

private class FileDownloadTarget(private val file: File) : DownloadTarget {
    override fun exists(): Boolean = file.exists()
    override fun length(): Long = file.length()
    override fun delete(): Boolean = file.delete()
    override fun openOutputStream(append: Boolean): OutputStream = FileOutputStream(file, append)
    override fun toUriString(): String = file.toURI().toString()
    override fun renameTo(other: DownloadTarget): Boolean {
        if (other is FileDownloadTarget) {
            return file.renameTo(other.file)
        }
        return false
    }
    override fun copyTo(other: DownloadTarget) {
        if (other is FileDownloadTarget) {
            file.copyTo(other.file, overwrite = true)
        } else {
            file.inputStream().use { input ->
                other.openOutputStream(false).use { output ->
                    input.copyTo(output)
                }
            }
        }
    }
}

private class DocumentDownloadTarget(
    private val context: Context,
    private val tree: DocumentFile,
    private val fileName: String,
) : DownloadTarget {
    private fun getDoc(): DocumentFile? = tree.findFile(fileName)

    override fun exists(): Boolean = getDoc()?.exists() ?: false
    override fun length(): Long = getDoc()?.length() ?: 0L
    override fun delete(): Boolean = getDoc()?.delete() ?: false
    override fun openOutputStream(append: Boolean): OutputStream {
        val doc = getDoc() ?: tree.createFile("application/octet-stream", fileName)
            ?: error("Failed to create file $fileName")
        return context.contentResolver.openOutputStream(doc.uri, if (append) "wa" else "w")
            ?: error("Failed to open output stream for $fileName")
    }
    override fun toUriString(): String = getDoc()?.uri?.toString() ?: ""
    override fun renameTo(other: DownloadTarget): Boolean {
        val doc = getDoc() ?: return false
        if (other is DocumentDownloadTarget && other.tree.uri == tree.uri) {
            return doc.renameTo(other.fileName)
        }
        return false
    }
    override fun copyTo(other: DownloadTarget) {
        val doc = getDoc() ?: return
        context.contentResolver.openInputStream(doc.uri)?.use { input ->
            other.openOutputStream(false).use { output ->
                input.copyTo(output)
            }
        }
    }
}

private class DocumentSingleTarget(
    private val context: Context,
    private val doc: DocumentFile,
) : DownloadTarget {
    override fun exists(): Boolean = doc.exists()
    override fun length(): Long = doc.length()
    override fun delete(): Boolean = doc.delete()
    override fun openOutputStream(append: Boolean): OutputStream =
        context.contentResolver.openOutputStream(doc.uri, if (append) "wa" else "w")
            ?: error("Failed to open output stream")
    override fun toUriString(): String = doc.uri.toString()
    override fun renameTo(other: DownloadTarget): Boolean = false
    override fun copyTo(other: DownloadTarget) {
        context.contentResolver.openInputStream(doc.uri)?.use { input ->
            other.openOutputStream(false).use { output ->
                input.copyTo(output)
            }
        }
    }
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

private suspend fun performHlsDownloadAndroid(
    context: Context,
    request: DownloadPlatformRequest,
    onProgress: (downloadedBytes: Long, totalBytes: Long?) -> Unit,
    onSuccess: (localFileUri: String, totalBytes: Long?, companion: HlsCompanionOutcome?) -> Unit,
    onFailure: (message: String) -> Unit,
    onWarning: ((message: String) -> Unit)?,
) {
    DownloadsSettingsRepository.ensureLoaded()
    val customLocationUriString = DownloadsSettingsRepository.downloadLocationUri.value
    val customLocationUri = customLocationUriString?.let { Uri.parse(it) }

    val (videoTarget, videoTemp) = createHlsTargets(context, customLocationUri, request.destinationFileName)
    if (videoTemp.exists()) videoTemp.delete()

    val videoTempOut = videoTemp.openOutputStream(false)
    val videoChannel = (videoTempOut as? FileOutputStream)?.channel

    val ctx = coroutineContext
    try {
        val videoOutcome = downloadHlsToFile(
            sourceUrl = request.sourceUrl,
            httpGet = { url, range -> androidHttpGet(url, request.sourceHeaders, range) },
            appendBytes = { bytes ->
                videoTempOut.write(bytes)
                if (videoChannel != null) {
                    videoChannel.force(false)
                } else {
                    videoTempOut.flush()
                }
            },
            decryptAes128Cbc = ::aes128CbcDecryptAndroid,
            onProgress = onProgress,
            ensureActive = { ctx.ensureActive() },
        )

        videoTempOut.flush()
        videoTempOut.close()

        val finalVideoName = hlsOutputFileName(request.destinationFileName, videoOutcome.isFmp4)
        val videoFinal = resolveHlsTarget(context, customLocationUri, finalVideoName)
        if (videoFinal.exists()) videoFinal.delete()
        if (!videoTemp.renameTo(videoFinal)) {
            videoTemp.copyTo(videoFinal)
            videoTemp.delete()
        }

        val videoUri = videoFinal.toUriString()
        val audioUris = mutableListOf<String>()
        val subtitleUris = mutableListOf<String>()

        AndroidLog.i("Remux", "audio track count: ${request.hlsAudioUrls.size}, subtitle track count: ${request.hlsSubtitleUrls.size}")
        for ((audioIndex, audioUrl) in request.hlsAudioUrls.withIndex()) {
            if (audioUrl.isBlank()) continue
            try {
                val audioFinalName = hlsCompanionFileName(finalVideoName, "audio_$audioIndex", "mp4")
                val audioTempName = hlsCompanionFileName(request.destinationFileName, "audio_$audioIndex", "part")
                val uri = downloadSingleHlsTrackAndroid(
                    context = context,
                    customLocationUri = customLocationUri,
                    url = audioUrl,
                    headers = request.sourceHeaders,
                    finalName = audioFinalName,
                    tempName = audioTempName,
                    onProgress = onProgress,
                    ctx = ctx,
                )
                audioUris.add(uri)
            } catch (_: CancellationException) {
                throw CancellationException()
            } catch (e: Throwable) {
                val audioErrMsg = "audio[$audioIndex] download failed: ${e::class.simpleName} ${e.message}"
                AndroidLog.e("Remux", audioErrMsg)
                InAppLogger.error("Remux", audioErrMsg)
            }
        }

        for ((subIndex, subUrl) in request.hlsSubtitleUrls.withIndex()) {
            if (subUrl.isBlank()) continue
            try {
                val subFinalName = hlsCompanionFileName(finalVideoName, "subs_$subIndex", "vtt")
                val subTempName = hlsCompanionFileName(request.destinationFileName, "subs_$subIndex", "part")
                val uri = downloadSingleHlsTrackAndroid(
                    context = context,
                    customLocationUri = customLocationUri,
                    url = subUrl,
                    headers = request.sourceHeaders,
                    finalName = subFinalName,
                    tempName = subTempName,
                    onProgress = onProgress,
                    ctx = ctx,
                )
                subtitleUris.add(uri)
            } catch (_: CancellationException) {
                throw CancellationException()
            } catch (_: Throwable) {
                // skip silently
            }
        }

        val audioUri = audioUris.firstOrNull()
        val subtitleUri = subtitleUris.firstOrNull()

        AndroidLog.i("Remux", "hlsAudioUrl='${request.hlsAudioUrl}' audioUri=$audioUri subtitleUri=$subtitleUri isFmp4=${videoOutcome.isFmp4}")
        InAppLogger.info("Remux", "hlsAudioUrl='${request.hlsAudioUrl}' audioUri=$audioUri subtitleUri=$subtitleUri isFmp4=${videoOutcome.isFmp4}")
        val canRemux = videoUri.startsWith("file:")
        val remuxedUri = if (canRemux) {
            AndroidLog.i("Remux", "remux attempt videoUri=$videoUri audioUri=$audioUri")
            InAppLogger.info("Remux", "remux attempt videoUri=$videoUri audioUri=$audioUri")
            try {
                val videoPath = File(URI(videoUri)).absolutePath
                val audioPath = audioUri?.let { File(URI(it)).absolutePath }
                AndroidLog.i("Remux", "videoPath=$videoPath audioPath=$audioPath")
                InAppLogger.info("Remux", "videoPath=$videoPath audioPath=$audioPath")
                val videoFile = File(videoPath)
                if (!videoFile.exists()) InAppLogger.warn("Remux", "video FILE NOT FOUND")
                val destMp4Name = hlsOutputFileName(request.destinationFileName, isFmp4 = true)
                val destFile = if (customLocationUri != null && customLocationUri.scheme == "content") {
                    File(context.cacheDir, destMp4Name)
                } else {
                    File(context.filesDir, "downloads/$destMp4Name")
                }
                InAppLogger.info("Remux", "destMp4Name=$destMp4Name")
                val remuxTmpPath = destFile.absolutePath + ".remux_tmp"
                val remuxOk = remuxToMp4(context, videoPath, audioPath, remuxTmpPath)
                AndroidLog.i("Remux", "remuxToMp4 returned $remuxOk")
                InAppLogger.info("Remux", "remuxToMp4 returned $remuxOk")
                if (remuxOk) {
                    val videoDeleted = File(videoPath).delete()
                    val audioDeleted = audioPath?.let { File(it).delete() } ?: false
                    AndroidLog.i("Remux", "videoDeleted=$videoDeleted audioDeleted=$audioDeleted")
                    InAppLogger.info("Remux", "videoDeleted=$videoDeleted audioDeleted=$audioDeleted")
                    if (destFile.exists()) destFile.delete()
                    val renamed = File(remuxTmpPath).renameTo(destFile)
                    InAppLogger.info("Remux", "tmpRenamedToFinal=$renamed finalExists=${destFile.exists()}")
                    if (customLocationUri != null && customLocationUri.scheme == "content") {
                        val safTarget = resolveHlsTarget(context, customLocationUri, destMp4Name)
                        if (safTarget.exists()) safTarget.delete()
                        safTarget.openOutputStream(false).use { out ->
                            destFile.inputStream().use { inp -> inp.copyTo(out) }
                        }
                        destFile.delete()
                        safTarget.toUriString()
                    } else {
                        destFile.toURI().toString()
                    }
                } else {
                    File(remuxTmpPath).delete()
                    null
                }
            } catch (e: Exception) {
                InAppLogger.error("Remux", "remux block exception: ${e.message}")
                null
            }
        } else {
            val skipMsg = "skipped — canRemux=$canRemux videoUri.startsWith(file:)=${videoUri.startsWith("file:")} isFmp4=${videoOutcome.isFmp4}"
            AndroidLog.i("Remux", skipMsg)
            InAppLogger.info("Remux", skipMsg)
            null
        }

        val companionWarning = if (request.hlsAudioUrls.isNotEmpty() && audioUri == null) {
            "Audio track(s) not found"
        } else null
        if (remuxedUri != null) {
            onSuccess(remuxedUri, videoOutcome.totalBytes, HlsCompanionOutcome(null, subtitleUri, audioLocalFileUris = audioUris, subtitleLocalFileUris = subtitleUris, warningMessage = companionWarning))
        } else {
            onSuccess(videoUri, videoOutcome.totalBytes, HlsCompanionOutcome(audioUri, subtitleUri, audioUris, subtitleUris, companionWarning))
        }
    } catch (_: CancellationException) {
        videoTempOut.closeSafe()
        videoTemp.delete()
    } catch (error: Throwable) {
        videoTempOut.closeSafe()
        onFailure(error.message ?: runBlocking { getString(Res.string.download_failed) })
    }
}

private suspend fun downloadSingleHlsTrackAndroid(
    context: Context,
    customLocationUri: Uri?,
    url: String,
    headers: Map<String, String>,
    finalName: String,
    tempName: String,
    onProgress: (downloadedBytes: Long, totalBytes: Long?) -> Unit,
    ctx: kotlin.coroutines.CoroutineContext,
): String {
    val (finalTarget, tempTarget) = createHlsTargets(context, customLocationUri, tempName)
    if (tempTarget.exists()) tempTarget.delete()

    val out = tempTarget.openOutputStream(false)
    val channel = (out as? FileOutputStream)?.channel

    try {
        val outcome = downloadHlsToFile(
            sourceUrl = url,
            httpGet = { u, range -> androidHttpGet(u, headers, range) },
            appendBytes = { bytes ->
                out.write(bytes)
                if (channel != null) {
                    channel.force(false)
                } else {
                    out.flush()
                }
            },
            decryptAes128Cbc = ::aes128CbcDecryptAndroid,
            onProgress = { _, _ -> },
            ensureActive = { ctx.ensureActive() },
        )

        out.flush()
        out.close()

        val actualFinalName = hlsOutputFileName(finalName, outcome.isFmp4)
        val actualFinal = resolveHlsTarget(context, customLocationUri, actualFinalName)
        if (actualFinal.exists()) actualFinal.delete()
        if (!tempTarget.renameTo(actualFinal)) {
            tempTarget.copyTo(actualFinal)
            tempTarget.delete()
        }
        return actualFinal.toUriString()
    } catch (e: Throwable) {
        InAppLogger.error("Remux", "downloadSingleHlsTrackAndroid error: ${e::class.simpleName} ${e.message}")
        out.closeSafe()
        tempTarget.delete()
        throw e
    }
}

private fun createHlsTargets(
    context: Context,
    customLocationUri: Uri?,
    fileName: String,
): Pair<DownloadTarget, DownloadTarget> {
    return if (customLocationUri != null && customLocationUri.scheme == "content") {
        val tree = DocumentFile.fromTreeUri(context, customLocationUri)
            ?: error("Cannot access custom download location")
        val dest = DocumentDownloadTarget(context, tree, fileName)
        val temp = DocumentDownloadTarget(context, tree, "$fileName.part")
        dest to temp
    } else {
        val downloadsDir = File(context.filesDir, "downloads").apply { mkdirs() }
        val dest = FileDownloadTarget(File(downloadsDir, fileName))
        val temp = FileDownloadTarget(File(downloadsDir, "$fileName.part"))
        dest to temp
    }
}

private fun resolveHlsTarget(
    context: Context,
    customLocationUri: Uri?,
    fileName: String,
): DownloadTarget {
    return if (customLocationUri != null && customLocationUri.scheme == "content") {
        val tree = DocumentFile.fromTreeUri(context, customLocationUri)
            ?: error("download location tree disappeared")
        DocumentDownloadTarget(context, tree, fileName)
    } else {
        val downloadsDir = File(context.filesDir, "downloads")
        FileDownloadTarget(File(downloadsDir, fileName))
    }
}

private suspend fun androidHttpGet(
    url: String,
    headers: Map<String, String>,
    range: HlsByteRange?,
): HlsHttpResult {
    val requestBuilder = Request.Builder().url(url)
    headers.forEach { (key, value) ->
        requestBuilder.header(key, value)
    }
    if (range != null) {
        requestBuilder.header(
            "Range",
            "bytes=${range.offset}-${range.offset + range.length - 1}",
        )
    }

    val response = downloadHttpClient.newCall(requestBuilder.get().build()).execute()
    return response.use { resp ->
        HlsHttpResult(
            status = resp.code,
            body = resp.body?.bytes() ?: ByteArray(0),
            finalUrl = resp.request.url.toString(),
        )
    }
}

private fun aes128CbcDecryptAndroid(data: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
    if (data.isEmpty()) return ByteArray(0)
    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    val keySpec = SecretKeySpec(key, "AES")
    val ivSpec = IvParameterSpec(iv)
    cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
    return cipher.doFinal(data)
}

private fun remuxToMp4(
    context: Context,
    videoPath: String,
    audioPath: String?,
    outputPath: String,
): Boolean {
    return try {
        AndroidLog.i("Remux", "remuxToMp4 videoPath=$videoPath audioPath=$audioPath outputPath=$outputPath")
        InAppLogger.debug("Remux", "remuxToMp4 start")
        val videoExtractor = MediaExtractor()
        videoExtractor.setDataSource(videoPath)
        InAppLogger.debug("Remux", "videoExtractor trackCount=${videoExtractor.trackCount}")

        val audioExtractor: MediaExtractor? = if (audioPath != null) {
            MediaExtractor().apply { setDataSource(audioPath) }.also {
                InAppLogger.debug("Remux", "audioExtractor trackCount=${it.trackCount}")
            }
        } else null

        val videoTrackId = findMediaTrack(videoExtractor, "video/")
        val videoAudioTrackId = findMediaTrack(videoExtractor, "audio/")
        val audioTrackId = audioExtractor?.let { findMediaTrack(it, "audio/") } ?: -1
        val effectiveAudioTrackId = if (audioTrackId >= 0) audioTrackId else videoAudioTrackId
        AndroidLog.i("Remux", "videoTrackId=$videoTrackId videoAudioTrackId=$videoAudioTrackId audioTrackId=$audioTrackId effectiveAudioTrackId=$effectiveAudioTrackId")
        InAppLogger.debug("Remux", "videoTrackId=$videoTrackId videoAudioTrackId=$videoAudioTrackId audioTrackId=$audioTrackId effectiveAudioTrackId=$effectiveAudioTrackId")
        if (videoTrackId < 0) {
            InAppLogger.warn("Remux", "no video track found, aborting")
            videoExtractor.release()
            audioExtractor?.release()
            return false
        }

        videoExtractor.selectTrack(videoTrackId)
        val videoFormat = videoExtractor.getTrackFormat(videoTrackId)
        val videoMime = videoFormat.getString(MediaFormat.KEY_MIME)
        val videoCsd0 = videoFormat.getByteBuffer("csd-0")
        val videoCsd1 = videoFormat.getByteBuffer("csd-1")
        AndroidLog.i("Remux", "videoMime=$videoMime hasCsd0=${videoCsd0 != null} csd0Size=${videoCsd0?.remaining()} hasCsd1=${videoCsd1 != null}")
        InAppLogger.debug("Remux", "videoMime=$videoMime hasCsd0=${videoCsd0 != null} csd0Size=${videoCsd0?.remaining()} hasCsd1=${videoCsd1 != null}")

        val muxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        val videoMuxerTrack = muxer.addTrack(videoFormat)
        InAppLogger.debug("Remux", "added video track index=$videoMuxerTrack")
        var audioMuxerTrack = -1
        if (effectiveAudioTrackId >= 0) {
            val audioSource = if (audioTrackId >= 0) audioExtractor!! else videoExtractor
            audioSource.selectTrack(effectiveAudioTrackId)
            val audioFormat = audioSource.getTrackFormat(effectiveAudioTrackId)
            val audioMime = audioFormat.getString(MediaFormat.KEY_MIME)
            InAppLogger.debug("Remux", "audioMime=$audioMime")
            audioMuxerTrack = muxer.addTrack(audioFormat)
            InAppLogger.debug("Remux", "added audio track index=$audioMuxerTrack")
        }

        muxer.start()

        val buffer = ByteBuffer.allocateDirect(1024 * 1024)
        val info = MediaCodec.BufferInfo()

        AndroidLog.i("Remux", "copying video samples...")
        InAppLogger.debug("Remux", "copying video samples...")
        try {
            copyTrack(videoExtractor, muxer, videoMuxerTrack, buffer, info, tag = "video")
        } catch (e: Exception) {
            AndroidLog.e("Remux", "copyTrack[video] failed: ${e.message}")
            throw e
        }
        InAppLogger.debug("Remux", "video samples done")
        if (effectiveAudioTrackId >= 0) {
            val audioSource = if (audioTrackId >= 0) audioExtractor!! else videoExtractor
            InAppLogger.debug("Remux", "copying audio samples...")
            try {
                copyTrack(audioSource, muxer, audioMuxerTrack, buffer, info, tag = "audio")
            } catch (e: Exception) {
                AndroidLog.e("Remux", "copyTrack[audio] failed: ${e.message}")
                throw e
            }
            InAppLogger.debug("Remux", "audio samples done")
        }

        try {
            muxer.stop()
        } catch (e: Exception) {
            AndroidLog.e("Remux", "muxer.stop() failed: ${e.message}")
            InAppLogger.warn("Remux", "muxer.stop() failed: ${e.message}")
        }
        muxer.release()
        videoExtractor.release()
        audioExtractor?.release()
        AndroidLog.i("Remux", "remuxToMp4 SUCCEEDED")
        InAppLogger.debug("Remux", "remuxToMp4 SUCCEEDED")
        true
    } catch (e: Exception) {
        AndroidLog.e("Remux", "remuxToMp4 exception: ${e::class.simpleName} msg='${e.message}' ${e.stackTraceToString().take(500)}")
        InAppLogger.error("Remux", "remuxToMp4 exception: ${e::class.simpleName} ${e.message}")
        false
    }
}

private fun findMediaTrack(extractor: MediaExtractor, mimePrefix: String): Int {
    for (i in 0 until extractor.trackCount) {
        val mime = extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME) ?: ""
        if (mime.startsWith(mimePrefix)) return i
    }
    return -1
}

private fun copyTrack(
    extractor: MediaExtractor,
    muxer: MediaMuxer,
    muxerTrack: Int,
    buffer: ByteBuffer,
    info: MediaCodec.BufferInfo,
    tag: String = "",
) {
    var sampleCount = 0
    var prevTimeUs = -1L
    while (true) {
        buffer.clear()
        val size = extractor.readSampleData(buffer, 0)
        if (size < 0) break
        val flags = extractor.sampleFlags
        val timeUs = extractor.sampleTime
        if (flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
            extractor.advance()
            continue
        }
        if (sampleCount < 3 || timeUs < prevTimeUs) {
            AndroidLog.i("Remux", "copyTrack[$tag] sample #$sampleCount size=$size timeUs=$timeUs flags=$flags prevTimeUs=$prevTimeUs nonMonotonic=${timeUs < prevTimeUs}")
            prevTimeUs = timeUs
        }
        buffer.limit(size)
        buffer.position(0)
        val writeFlags = flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM.inv()
        info.set(0, size, timeUs, writeFlags)
        muxer.writeSampleData(muxerTrack, buffer, info)
        extractor.advance()
        sampleCount++
    }
    AndroidLog.i("Remux", "copyTrack[$tag] done, totalSamples=$sampleCount")
}

private fun OutputStream.closeSafe() {
    try { close() } catch (_: Exception) { }
}
