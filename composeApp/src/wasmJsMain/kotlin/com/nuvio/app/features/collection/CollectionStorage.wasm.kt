package com.nuvio.app.features.collection

import com.nuvio.app.WebStorage

internal actual object CollectionStorage {
    private const val KEY = "nuvio_collection"

    actual fun loadPayload(): String? = WebStorage.getString(KEY)
    actual fun savePayload(payload: String) { WebStorage.setString(KEY, payload) }
}
