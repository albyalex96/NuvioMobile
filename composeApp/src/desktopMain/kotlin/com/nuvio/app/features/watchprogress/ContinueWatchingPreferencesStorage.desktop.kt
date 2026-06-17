package com.nuvio.app.features.watchprogress

import com.nuvio.app.core.storage.DesktopStorage

internal actual object ContinueWatchingPreferencesStorage {
    private val store = DesktopStorage.store("nuvio_continue_watching_preferences")

    actual fun loadPayload(): String? = store.getString("continue_watching_preferences_payload")

    actual fun savePayload(payload: String) = store.putString("continue_watching_preferences_payload", payload)
}
