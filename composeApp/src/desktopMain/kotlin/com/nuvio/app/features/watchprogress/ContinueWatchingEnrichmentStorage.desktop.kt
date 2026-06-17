package com.nuvio.app.features.watchprogress

import com.nuvio.app.core.storage.DesktopStorage

internal actual object ContinueWatchingEnrichmentStorage {
    private val store = DesktopStorage.store("nuvio_cw_enrichment")

    actual fun loadPayload(key: String): String? = store.getString(key)

    actual fun savePayload(key: String, payload: String) = store.putString(key, payload)

    actual fun removePayload(key: String) = store.remove(key)
}
