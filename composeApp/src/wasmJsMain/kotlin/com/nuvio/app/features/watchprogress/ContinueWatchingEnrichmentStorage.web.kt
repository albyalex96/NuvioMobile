package com.nuvio.app.features.watchprogress

import com.nuvio.app.core.platform.WebKeyValueStorage

internal actual object ContinueWatchingEnrichmentStorage {
    private const val namespace = "nuvio_cw_enrichment"

    actual fun loadPayload(key: String): String? =
        WebKeyValueStorage.getString(namespace, key)

    actual fun savePayload(key: String, payload: String) {
        WebKeyValueStorage.setString(namespace, key, payload)
    }

    actual fun removePayload(key: String) {
        WebKeyValueStorage.remove(namespace, key)
    }
}
