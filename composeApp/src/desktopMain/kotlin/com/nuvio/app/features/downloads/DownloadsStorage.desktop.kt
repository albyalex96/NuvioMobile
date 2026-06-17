package com.nuvio.app.features.downloads

import com.nuvio.app.core.storage.DesktopStorage

internal actual object DownloadsStorage {
    private val store = DesktopStorage.store("nuvio_downloads")

    actual fun loadPayload(): String? =
        store.getString("downloads_payload")

    actual fun savePayload(payload: String) {
        store.putString("downloads_payload", payload)
    }

    actual fun getDownloadLocationUri(): String? =
        store.getString("download_location_uri")

    actual fun setDownloadLocationUri(uri: String?) {
        store.putString("download_location_uri", uri)
    }
}
