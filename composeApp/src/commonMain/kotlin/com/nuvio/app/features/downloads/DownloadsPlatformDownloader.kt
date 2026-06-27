package com.nuvio.app.features.downloads

internal data class DownloadPlatformRequest(
    val downloadId: String,
    val displayTitle: String,
    val sourceUrl: String,
    val sourceHeaders: Map<String, String>,
    val destinationFileName: String,
    val isHlsStream: Boolean = false,
    val hlsAudioUrl: String? = null,
    val hlsSubtitleUrl: String? = null,
)

internal data class HlsCompanionOutcome(
    val audioLocalFileUri: String?,
    val subtitleLocalFileUri: String?,
)

internal interface DownloadsTaskHandle {
    fun cancel()
}

internal expect object DownloadsPlatformDownloader {
    fun start(
        request: DownloadPlatformRequest,
        onProgress: (downloadedBytes: Long, totalBytes: Long?) -> Unit,
        onSuccess: (localFileUri: String, totalBytes: Long?, companion: HlsCompanionOutcome?) -> Unit,
        onFailure: (message: String) -> Unit,
    ): DownloadsTaskHandle

    fun removeFile(localFileUri: String?): Boolean

    fun removePartialFile(destinationFileName: String): Boolean

    fun resolveLocalFileUri(localFileUri: String?, destinationFileName: String): String?

    fun fetchUrlAsString(url: String, headers: Map<String, String>): String?
}
