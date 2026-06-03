package com.nuvio.app.features.downloads

import com.nuvio.app.WebStorage

internal actual object DownloadsStorage {
    private const val KEY = "nuvio_downloads"

    actual fun loadPayload(): String? = WebStorage.getString(KEY)
    actual fun savePayload(payload: String) { WebStorage.setString(KEY, payload) }
}
