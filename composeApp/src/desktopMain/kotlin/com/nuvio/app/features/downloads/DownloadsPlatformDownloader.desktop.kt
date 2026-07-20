package com.nuvio.app.features.downloads

import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import kotlin.concurrent.thread

internal class DesktopDownloadsTaskHandle(private val thread: Thread) : DownloadsTaskHandle {
    override fun cancel() {
        thread.interrupt()
    }
}

internal actual object DownloadsPlatformDownloader {
    actual fun start(
        request: DownloadPlatformRequest,
        onProgress: (downloadedBytes: Long, totalBytes: Long?) -> Unit,
        onSuccess: (localFileUri: String, totalBytes: Long?, companion: HlsCompanionOutcome?) -> Unit,
        onFailure: (message: String) -> Unit,
        onWarning: ((message: String) -> Unit)?,
        onPhase: ((phase: String) -> Unit)?,
    ): DownloadsTaskHandle {
        if (request.isHlsStream) {
            onFailure("HLS download not supported on desktop yet.")
            return DesktopDownloadsTaskHandle(Thread.currentThread())
        }

        val t = thread(name = "download-${request.destinationFileName}", isDaemon = true) {
            try {
                val url = URL(request.sourceUrl)
                val conn = url.openConnection() as HttpURLConnection
                request.sourceHeaders.forEach { (k, v) -> conn.setRequestProperty(k, v) }
                conn.connect()
                val totalBytes = conn.contentLengthLong.let { if (it < 0) null else it }
                val destDir = File(System.getProperty("java.io.tmpdir") ?: "/tmp", "nuvio-downloads")
                destDir.mkdirs()
                val destFile = File(destDir, request.destinationFileName)
                conn.inputStream.use { input ->
                    destFile.outputStream().use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var downloaded: Long = 0L
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            if (Thread.currentThread().isInterrupted) {
                                onFailure("Download cancelled")
                                return@thread
                            }
                            output.write(buffer, 0, bytesRead)
                            downloaded += bytesRead
                            onProgress(downloaded, totalBytes)
                        }
                    }
                }
                onSuccess(destFile.toURI().toString(), totalBytes, null)
            } catch (e: Exception) {
                if (!Thread.currentThread().isInterrupted) {
                    onFailure(e.message ?: "Unknown error")
                }
            }
        }
        return DesktopDownloadsTaskHandle(t)
    }

    actual fun removeFile(localFileUri: String?): Boolean {
        if (localFileUri == null) return false
        return runCatching { File(URI(localFileUri)).delete() }.getOrElse { false }
    }

    actual fun removePartialFile(destinationFileName: String): Boolean {
        val file = File(System.getProperty("java.io.tmpdir") ?: "/tmp", "nuvio-downloads/$destinationFileName")
        return file.delete()
    }

    actual fun resolveLocalFileUri(localFileUri: String?, destinationFileName: String): String? {
        return localFileUri ?: let {
            val file = File(System.getProperty("java.io.tmpdir") ?: "/tmp", "nuvio-downloads/$destinationFileName")
            if (file.exists()) file.toURI().toString() else null
        }
    }

    actual fun openDownloadsDirectory(): Boolean {
        val dir = File(System.getProperty("java.io.tmpdir") ?: "/tmp", "nuvio-downloads")
        return dir.exists() || dir.mkdirs()
    }

    actual fun fetchUrlAsString(url: String, headers: Map<String, String>): String? {
        return try {
            val conn = URL(url).openConnection() as HttpURLConnection
            headers.forEach { (k, v) -> conn.setRequestProperty(k, v) }
            conn.connect()
            conn.inputStream.reader().readText()
        } catch (_: Exception) {
            null
        }
    }
}
