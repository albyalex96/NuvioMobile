package com.nuvio.app.features.watchprogress

import com.nuvio.app.WebStorage

internal actual object ContinueWatchingPreferencesStorage {
    private const val KEY = "nuvio_continue_watching_preferences"

    actual fun loadPayload(): String? = WebStorage.getString(KEY)
    actual fun savePayload(payload: String) { WebStorage.setString(KEY, payload) }
}
