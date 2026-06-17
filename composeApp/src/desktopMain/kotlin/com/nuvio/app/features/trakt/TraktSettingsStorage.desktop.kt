package com.nuvio.app.features.trakt

import com.nuvio.app.core.storage.DesktopStorage

internal actual object TraktSettingsStorage {
    private val store = DesktopStorage.store("nuvio_trakt_settings")

    actual fun loadPayload(): String? = store.getString("trakt_settings_payload")

    actual fun savePayload(payload: String) = store.putString("trakt_settings_payload", payload)
}
