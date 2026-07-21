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
    val hlsAudioUrls: List<String> = emptyList(),
    val hlsSubtitleUrls: List<String> = emptyList(),
)

internal data class HlsCompanionOutcome(
    val audioLocalFileUri: String?,
    val subtitleLocalFileUri: String?,
    val audioLocalFileUris: List<String> = emptyList(),
    val subtitleLocalFileUris: List<String> = emptyList(),
    val warningMessage: String? = null,
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
        onWarning: ((message: String) -> Unit)? = null,
        onPhase: ((phase: String) -> Unit)? = null,
        onTrackProgress: ((trackName: String, downloadedBytes: Long, totalBytes: Long?) -> Unit)? = null,
    ): DownloadsTaskHandle

    fun removeFile(localFileUri: String?): Boolean

    fun removePartialFile(destinationFileName: String): Boolean

    fun resolveLocalFileUri(localFileUri: String?, destinationFileName: String): String?

    fun fetchUrlAsString(url: String, headers: Map<String, String>): String?

    fun openDownloadsDirectory(): Boolean
}
