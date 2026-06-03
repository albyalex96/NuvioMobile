package com.nuvio.app.features.trakt

import com.nuvio.app.WebStorage

internal actual object TraktAuthStorage {
    private const val KEY = "nuvio_trakt_auth"

    actual fun loadPayload(): String? = WebStorage.getString(KEY)
    actual fun savePayload(payload: String) { WebStorage.setString(KEY, payload) }
}
