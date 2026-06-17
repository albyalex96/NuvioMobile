package com.nuvio.app.features.home

import com.nuvio.app.core.storage.DesktopStorage

internal actual object Top10CatalogStorage {
    private val store = DesktopStorage.store("nuvio_top10_catalog_settings")

    actual fun loadPayload(): String? = store.getString("top10_catalog_payload")

    actual fun savePayload(payload: String) = store.putString("top10_catalog_payload", payload)
}
