package com.nuvio.app.features.downloads

import com.nuvio.app.core.platform.WebKeyValueStorage
import com.nuvio.app.core.platform.webNowEpochMs
import com.nuvio.app.core.storage.ProfileScopedKey

internal actual object DownloadsClock {
    actual fun nowEpochMs(): Long = webNowEpochMs()
}

internal actual object DownloadsStorage {
    private const val namespace = "nuvio_downloads"
    private const val payloadKey = "downloads_payload"

    actual fun loadPayload(): String? =
        WebKeyValueStorage.getString(namespace, ProfileScopedKey.of(payloadKey))

    actual fun savePayload(payload: String) {
        WebKeyValueStorage.setString(namespace, ProfileScopedKey.of(payloadKey), payload)
    }
}

internal actual object DownloadsLiveStatusPlatform {
    actual fun onItemsChanged(items: List<DownloadItem>) = Unit
}

internal actual object DownloadsPlatformDownloader {
    actual fun start(
        request: DownloadPlatformRequest,
        onProgress: (downloadedBytes: Long, totalBytes: Long?) -> Unit,
        onSuccess: (localFileUri: String, totalBytes: Long?) -> Unit,
        onFailure: (message: String) -> Unit,
    ): DownloadsTaskHandle {
        onFailure("Downloads are not available on web.")
        return object : DownloadsTaskHandle {
            override fun cancel() = Unit
        }
    }

    actual fun removeFile(localFileUri: String?): Boolean = false

    actual fun removePartialFile(destinationFileName: String): Boolean = false

    actual fun resolveLocalFileUri(localFileUri: String?, destinationFileName: String): String? =
        localFileUri

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
        onFailure("HLS downloads are not available on web.")
        return object : DownloadsTaskHandle {
            override fun cancel() = Unit
        }
    }
}
