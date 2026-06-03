package com.nuvio.app.features.trakt

import com.nuvio.app.WebStorage

internal actual object TraktLibraryStorage {
    private const val KEY = "nuvio_trakt_library"

    actual fun loadPayload(): String? = WebStorage.getString(KEY)
    actual fun savePayload(payload: String) { WebStorage.setString(KEY, payload) }
}
