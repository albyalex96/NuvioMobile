package com.nuvio.app.features.collection

import com.nuvio.app.core.storage.DesktopStorage

internal actual object CollectionMobileSettingsStorage {
    private val store = DesktopStorage.store("nuvio_collection_settings")

    actual fun loadPayload(): String? =
        store.getString("collection_settings_payload")

    actual fun savePayload(payload: String) {
        store.putString("collection_settings_payload", payload)
    }
}
