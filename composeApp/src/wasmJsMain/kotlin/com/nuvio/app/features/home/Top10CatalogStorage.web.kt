package com.nuvio.app.features.home

import com.nuvio.app.core.platform.WebKeyValueStorage

internal actual object Top10CatalogStorage {
    private const val namespace = "nuvio_top10"

    actual fun loadPayload(): String? =
        WebKeyValueStorage.getString(namespace, "payload")

    actual fun savePayload(payload: String) =
        WebKeyValueStorage.setString(namespace, "payload", payload)
}
