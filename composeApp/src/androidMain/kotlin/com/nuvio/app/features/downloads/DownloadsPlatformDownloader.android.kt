package com.nuvio.app.features.downloads

import android.content.Context
import android.content.Intent
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.muxer.BufferInfo
import androidx.media3.muxer.Mp4Muxer
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
        onPhase: ((phase: String) -> Unit)?,
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
                    onPhase = onPhase,
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
    onPhase: ((phase: String) -> Unit)? = null,
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
        InAppLogger.info("Remux", "onPhase?.invoke('remux') about to be called")
        onPhase?.invoke("remux")
        InAppLogger.info("Remux", "onPhase?.invoke('remux') completed")
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
                val remuxResult = remuxToMp4(context, videoPath, audioPath, remuxTmpPath)
                AndroidLog.i("Remux", "remuxToMp4 returned $remuxResult")
                InAppLogger.info("Remux", "remuxToMp4 returned $remuxResult")
                if (remuxResult != RemuxResult.FAILED) {
                    val videoDeleted = File(videoPath).delete()
                    val audioDeleted = if (remuxResult == RemuxResult.FULL) {
                        audioPath?.let { File(it).delete() } ?: false
                    } else {
                        AndroidLog.i("Remux", "keeping companion audio (video-only remux)")
                        false
                    }
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

private enum class RemuxResult { FULL, VIDEO_ONLY, FAILED }

private fun remuxToMp4(
    context: Context,
    videoPath: String,
    audioPath: String?,
    outputPath: String,
): RemuxResult {
    if (Mp4ParserRemux.remux(videoPath, audioPath, outputPath)) {
        AndroidLog.i("Remux", "mp4parser remux succeeded (full audio+video)")
        InAppLogger.info("Remux", "mp4parser remux succeeded (full audio+video)")
        return RemuxResult.FULL
    }
    File(outputPath).delete()
    if (remuxToMp4Impl(videoPath, audioPath, outputPath)) {
        AndroidLog.i("Remux", "MediaMuxer remux succeeded (full audio+video)")
        InAppLogger.info("Remux", "MediaMuxer remux succeeded (full audio+video)")
        return RemuxResult.FULL
    }
    if (audioPath != null) {
        File(outputPath).delete()
        InAppLogger.warn("Remux", "full remux failed, retrying video-only")
        if (remuxToMp4Impl(videoPath, null, outputPath)) {
            InAppLogger.warn("Remux", "video-only remux succeeded (audio companion kept separately)")
            return RemuxResult.VIDEO_ONLY
        }
    }
    return RemuxResult.FAILED
}

private fun remuxToMp4Impl(
    videoPath: String,
    audioPath: String?,
    outputPath: String,
): Boolean {
    var videoExtractor: MediaExtractor? = null
    var audioExtractor: MediaExtractor? = null
    var muxer: Mp4Muxer? = null
    var outputStream: FileOutputStream? = null
    return try {
        AndroidLog.i("Remux", "remuxToMp4 videoPath=$videoPath audioPath=$audioPath outputPath=$outputPath")
        InAppLogger.debug("Remux", "remuxToMp4 start")
        videoExtractor = MediaExtractor()
        videoExtractor.setDataSource(videoPath)
        InAppLogger.debug("Remux", "videoExtractor trackCount=${videoExtractor.trackCount}")

        if (audioPath != null) {
            audioExtractor = MediaExtractor().apply { setDataSource(audioPath) }.also {
                InAppLogger.debug("Remux", "audioExtractor trackCount=${it.trackCount}")
            }
        }

        val videoTrackId = findMediaTrack(videoExtractor, "video/")
        val videoAudioTrackId = findMediaTrack(videoExtractor, "audio/")
        val audioTrackId = audioExtractor?.let { findMediaTrack(it, "audio/") } ?: -1
        val effectiveAudioTrackId = if (audioTrackId >= 0) audioTrackId else videoAudioTrackId
        AndroidLog.i("Remux", "videoTrackId=$videoTrackId videoAudioTrackId=$videoAudioTrackId audioTrackId=$audioTrackId effectiveAudioTrackId=$effectiveAudioTrackId")
        InAppLogger.debug("Remux", "videoTrackId=$videoTrackId videoAudioTrackId=$videoAudioTrackId audioTrackId=$audioTrackId effectiveAudioTrackId=$effectiveAudioTrackId")
        if (videoTrackId < 0) {
            InAppLogger.warn("Remux", "no video track found, aborting")
            return false
        }

        videoExtractor.selectTrack(videoTrackId)
        val androidVideoFormat = videoExtractor.getTrackFormat(videoTrackId)
        val videoMime = androidVideoFormat.getString(MediaFormat.KEY_MIME) ?: "video/avc"

        val videoFormat = Format.Builder()
            .setSampleMimeType(videoMime)
            .setWidth(androidVideoFormat.getInteger(MediaFormat.KEY_WIDTH, 0))
            .setHeight(androidVideoFormat.getInteger(MediaFormat.KEY_HEIGHT, 0))
            .apply {
                val csdList = mutableListOf<ByteArray>()
                androidVideoFormat.getByteBuffer("csd-0")?.let { buf ->
                    val bytes = ByteArray(buf.remaining())
                    buf.duplicate().get(bytes)
                    csdList.add(bytes)
                }
                androidVideoFormat.getByteBuffer("csd-1")?.let { buf ->
                    val bytes = ByteArray(buf.remaining())
                    buf.duplicate().get(bytes)
                    csdList.add(bytes)
                }
                if (csdList.isNotEmpty()) {
                    setInitializationData(csdList)
                }
            }.build()

        AndroidLog.i("Remux", "videoMime=$videoMime width=${androidVideoFormat.getInteger(MediaFormat.KEY_WIDTH, 0)} height=${androidVideoFormat.getInteger(MediaFormat.KEY_HEIGHT, 0)}")

        outputStream = FileOutputStream(outputPath)
        muxer = Mp4Muxer.Builder(outputStream)
            .setSampleBatchingEnabled(true)
            .setSampleCopyingEnabled(true)
            .build()

        val videoMuxerTrack = muxer.addTrack(videoFormat)
        InAppLogger.debug("Remux", "added video track index=$videoMuxerTrack")
        var audioMuxerTrack = -1
        if (effectiveAudioTrackId >= 0) {
            val audioSource = if (audioTrackId >= 0) audioExtractor!! else videoExtractor
            audioSource.selectTrack(effectiveAudioTrackId)
            val androidAudioFormat = audioSource.getTrackFormat(effectiveAudioTrackId)
            val audioMime = androidAudioFormat.getString(MediaFormat.KEY_MIME) ?: "audio/mp4a-latm"
            val sampleRate = androidAudioFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE, 44100)
            val channelCount = androidAudioFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT, 2)
            AndroidLog.i("Remux", "audioMime=$audioMime sampleRate=$sampleRate channels=$channelCount")

            val audioFormat = Format.Builder()
                .setSampleMimeType(MimeTypes.AUDIO_AAC)
                .setSampleRate(sampleRate)
                .setChannelCount(channelCount)
                .apply {
                    androidAudioFormat.getByteBuffer("csd-0")?.let { buf ->
                        val csdBytes = ByteArray(buf.remaining())
                        buf.duplicate().get(csdBytes)
                        AndroidLog.i("Remux", "audio csd-0 bytes=${csdBytes.joinToString(" ") { "%02x".format(it) }}")
                        setInitializationData(listOf(csdBytes))
                    }
                }.build()

            audioMuxerTrack = muxer.addTrack(audioFormat)
            InAppLogger.debug("Remux", "added audio track index=$audioMuxerTrack")
        }

        val buffer = ByteBuffer.allocateDirect(1024 * 1024)

        val videoSamples: Int
        val audioSamples: Int
        if (audioTrackId >= 0) {
            InAppLogger.debug("Remux", "copying video samples (separate extractor)...")
            videoSamples = copyTrack(videoExtractor, muxer, videoMuxerTrack, buffer, trackIndex = videoTrackId, tag = "video")
            InAppLogger.debug("Remux", "video samples done: $videoSamples")
            InAppLogger.debug("Remux", "copying audio samples (separate extractor)...")
            audioSamples = copyTrack(audioExtractor!!, muxer, audioMuxerTrack, buffer, trackIndex = effectiveAudioTrackId, tag = "audio")
            InAppLogger.debug("Remux", "audio samples done: $audioSamples")
        } else if (effectiveAudioTrackId >= 0) {
            InAppLogger.debug("Remux", "copying all tracks (same extractor)...")
            val counts = copyAllTracks(videoExtractor, muxer, videoMuxerTrack, audioMuxerTrack, videoTrackId, effectiveAudioTrackId, buffer)
            videoSamples = counts.first
            audioSamples = counts.second
            InAppLogger.debug("Remux", "all tracks done: videoSamples=$videoSamples audioSamples=$audioSamples")
        } else {
            InAppLogger.debug("Remux", "copying video samples (no audio)...")
            videoSamples = copyTrack(videoExtractor, muxer, videoMuxerTrack, buffer, trackIndex = videoTrackId, tag = "video")
            audioSamples = 0
            InAppLogger.debug("Remux", "video samples done: $videoSamples")
        }

        if (videoSamples == 0 && audioSamples == 0) {
            InAppLogger.error("Remux", "no samples written to any track, aborting")
            return false
        }

        try {
            muxer.close()
        } catch (e: Exception) {
            AndroidLog.e("Remux", "muxer.close() FAILED: ${e::class.simpleName} msg='${e.message}'")
            InAppLogger.error("Remux", "muxer.close() FAILED: ${e::class.simpleName} ${e.message}")
            throw e
        }
        muxer = null
        outputStream?.closeSafe()
        outputStream = null
        videoExtractor.release()
        videoExtractor = null
        audioExtractor?.release()
        audioExtractor = null

        if (!isValidMp4File(outputPath)) {
            InAppLogger.error("Remux", "output file is not a valid MP4 (missing moov)")
            File(outputPath).delete()
            return false
        }

        AndroidLog.i("Remux", "remuxToMp4 SUCCEEDED (videoSamples=$videoSamples audioSamples=$audioSamples)")
        InAppLogger.debug("Remux", "remuxToMp4 SUCCEEDED")
        true
    } catch (e: Exception) {
        AndroidLog.e("Remux", "remuxToMp4 exception: ${e::class.simpleName} msg='${e.message}' ${e.stackTraceToString().take(500)}")
        InAppLogger.error("Remux", "remuxToMp4 exception: ${e::class.simpleName} ${e.message}")
        try { muxer?.close() } catch (_: Exception) {}
        try { outputStream?.closeSafe() } catch (_: Exception) {}
        try { videoExtractor?.release() } catch (_: Exception) {}
        try { audioExtractor?.release() } catch (_: Exception) {}
        try { File(outputPath).delete() } catch (_: Exception) {}
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
    muxer: Mp4Muxer,
    muxerTrack: Int,
    buffer: ByteBuffer,
    trackIndex: Int = -1,
    tag: String = "",
): Int {
    var sampleCount = 0
    var prevTimeUs = -1L
    while (true) {
        buffer.clear()
        val size = extractor.readSampleData(buffer, 0)
        if (size < 0) break
        val sampleTrack = extractor.sampleTrackIndex
        if (trackIndex >= 0 && sampleTrack != trackIndex) {
            extractor.advance()
            continue
        }
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
        muxer.writeSampleData(muxerTrack, buffer, BufferInfo(timeUs, size, writeFlags))
        extractor.advance()
        sampleCount++
    }
    AndroidLog.i("Remux", "copyTrack[$tag] done, totalSamples=$sampleCount")
    return sampleCount
}

private fun copyAllTracks(
    extractor: MediaExtractor,
    muxer: Mp4Muxer,
    videoMuxerTrack: Int,
    audioMuxerTrack: Int,
    videoExtractorTrack: Int,
    audioExtractorTrack: Int,
    buffer: ByteBuffer,
): Pair<Int, Int> {
    var videoSamples = 0
    var audioSamples = 0
    while (true) {
        buffer.clear()
        val size = extractor.readSampleData(buffer, 0)
        if (size < 0) break
        val trackIndex = extractor.sampleTrackIndex
        val flags = extractor.sampleFlags
        val timeUs = extractor.sampleTime
        if (flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
            extractor.advance()
            continue
        }
        val muxerTrack = when (trackIndex) {
            videoExtractorTrack -> videoMuxerTrack
            audioExtractorTrack -> audioMuxerTrack
            else -> {
                extractor.advance()
                continue
            }
        }
        buffer.limit(size)
        buffer.position(0)
        val writeFlags = flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM.inv()
        muxer.writeSampleData(muxerTrack, buffer, BufferInfo(timeUs, size, writeFlags))
        extractor.advance()
        if (trackIndex == videoExtractorTrack) videoSamples++ else audioSamples++
    }
    AndroidLog.i("Remux", "copyAllTracks done: videoSamples=$videoSamples audioSamples=$audioSamples")
    return videoSamples to audioSamples
}

private fun isValidMp4File(path: String): Boolean {
    return try {
        val file = File(path)
        AndroidLog.i("Remux", "isValidMp4File fileSize=${file.length()} path=$path")
        InAppLogger.debug("Remux", "isValidMp4File fileSize=${file.length()}")
        if (!file.exists() || file.length() < 100) return false
        java.io.RandomAccessFile(file, "r").use { raf ->
            val hdr = ByteArray(8)
            if (raf.read(hdr) < 8) return@use false
            val ftyp = (hdr[4].toInt() shl 24) or (hdr[5].toInt() shl 16) or (hdr[6].toInt() shl 8) or hdr[7].toInt()
            if (ftyp != 0x66747970) return@use false
            val searchLen = minOf(raf.length(), 1048576L)
            raf.seek(raf.length() - searchLen)
            val tail = ByteArray(searchLen.toInt())
            raf.readFully(tail)
            val hasMoov = tail.decodeToString().contains("moov")
            if (!hasMoov) {
                AndroidLog.w("Remux", "moov not found in last $searchLen bytes, scanning full file...")
                InAppLogger.warn("Remux", "moov not found in last $searchLen bytes, scanning full file...")
                raf.seek(0)
                val full = ByteArray(raf.length().toInt().coerceAtMost(5_242_880))
                raf.read(full)
                val fullHasMoov = full.decodeToString().contains("moov")
                AndroidLog.i("Remux", "full scan hasMoov=$fullHasMoov")
                InAppLogger.debug("Remux", "full scan hasMoov=$fullHasMoov")
                fullHasMoov
            } else {
                true
            }
        }
    } catch (_: Exception) {
        false
    }
}

private fun OutputStream.closeSafe() {
    try { close() } catch (_: Exception) { }
}


