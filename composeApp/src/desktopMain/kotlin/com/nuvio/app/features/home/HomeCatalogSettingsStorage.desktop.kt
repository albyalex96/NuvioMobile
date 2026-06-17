package com.nuvio.app.features.home

import com.nuvio.app.core.storage.DesktopStorage

internal actual object HomeCatalogSettingsStorage {
    private val store = DesktopStorage.store("nuvio_home_catalog_settings")

    actual fun loadPayload(): String? =
        store.getString("home_catalog_payload")

    actual fun savePayload(payload: String) {
        store.putString("home_catalog_payload", payload)
    }
}
