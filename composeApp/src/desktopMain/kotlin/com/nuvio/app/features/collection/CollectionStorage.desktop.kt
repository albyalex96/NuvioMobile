package com.nuvio.app.features.collection

import com.nuvio.app.core.storage.DesktopStorage

internal actual object CollectionStorage {
    private val store = DesktopStorage.store("nuvio_collection")

    actual fun loadPayload(): String? =
        store.getString("collection_payload")

    actual fun savePayload(payload: String) {
        store.putString("collection_payload", payload)
    }
}
