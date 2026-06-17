package com.nuvio.app.features.details

import com.nuvio.app.core.storage.DesktopStorage

internal actual object MetaScreenSettingsStorage {
    private val store = DesktopStorage.store("nuvio_meta_screen_settings")

    actual fun loadPayload(): String? =
        store.getString("meta_screen_payload")

    actual fun savePayload(payload: String) {
        store.putString("meta_screen_payload", payload)
    }
}
