package com.nuvio.app.features.downloads

internal actual object DownloadsPlatformDownloader {
    actual fun start(
        request: DownloadPlatformRequest,
        onProgress: (downloadedBytes: Long, totalBytes: Long?) -> Unit,
        onSuccess: (localFileUri: String, totalBytes: Long?) -> Unit,
        onFailure: (message: String) -> Unit,
    ): DownloadsTaskHandle {
        onFailure("Downloads not supported on web")
        return object : DownloadsTaskHandle {
            override fun cancel() {}
        }
    }

    actual fun removeFile(localFileUri: String?): Boolean = false
    actual fun removePartialFile(destinationFileName: String): Boolean = false
    actual fun resolveLocalFileUri(localFileUri: String?, destinationFileName: String): String? = null
    actual fun fetchUrlAsString(url: String, headers: Map<String, String>): String? = null
    actual fun probeHlsContentType(url: String, headers: Map<String, String>): Boolean = false

    actual fun downloadHlsSegments(
        segmentUrls: List<String>,
        sourceHeaders: Map<String, String>,
        destinationFileName: String,
        onProgress: (downloadedBytes: Long, totalBytes: Long?) -> Unit,
        onSuccess: (localFileUri: String, totalBytes: Long?) -> Unit,
        onFailure: (message: String) -> Unit,
    ): DownloadsTaskHandle {
        onFailure("HLS downloads not supported on web")
        return object : DownloadsTaskHandle {
            override fun cancel() {}
        }
    }
}
