package com.nuvio.app.features.downloads

internal data class DownloadPlatformRequest(
    val sourceUrl: String,
    val sourceHeaders: Map<String, String>,
    val destinationFileName: String,
)

internal data class HlsSegmentSpec(
    val url: String,
    val keyIndex: Int?,
)

internal data class HlsDownloadContext(
    val segments: List<HlsSegmentSpec>,
    val keyDataList: List<ByteArray>,
    /** Per-key IV bytes (16 bytes each). null at index means derive from segment index. */
    val keyIvList: List<ByteArray?>,
    val mapInitSegment: ByteArray?,
    val sourceHeaders: Map<String, String>,
    val destinationFileName: String,
)

internal interface DownloadsTaskHandle {
    fun cancel()
}

internal expect object DownloadsPlatformDownloader {
    fun start(
        request: DownloadPlatformRequest,
        onProgress: (downloadedBytes: Long, totalBytes: Long?) -> Unit,
        onSuccess: (localFileUri: String, totalBytes: Long?) -> Unit,
        onFailure: (message: String) -> Unit,
    ): DownloadsTaskHandle

    fun removeFile(localFileUri: String?): Boolean

    fun removePartialFile(destinationFileName: String): Boolean

    fun resolveLocalFileUri(localFileUri: String?, destinationFileName: String): String?

    fun fetchUrlAsString(url: String, headers: Map<String, String>): String?

    fun fetchUrlAsBytes(url: String, headers: Map<String, String>): ByteArray?

    fun probeHlsContentType(url: String, headers: Map<String, String>): Boolean

    fun downloadHlsSegments(
        context: HlsDownloadContext,
        onProgress: (downloadedBytes: Long, totalBytes: Long?) -> Unit,
        onSuccess: (localFileUri: String, totalBytes: Long?) -> Unit,
        onFailure: (message: String) -> Unit,
    ): DownloadsTaskHandle
}

internal expect fun ByteArray.aes128CbcDecrypt(key: ByteArray, iv: ByteArray): ByteArray

internal expect fun remuxTsToMp4(inputPath: String, outputPath: String): Boolean
