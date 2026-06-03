package com.nuvio.app.features.watchprogress

import com.nuvio.app.WebStorage

internal actual object ContinueWatchingEnrichmentStorage {
    actual fun loadPayload(key: String): String? = WebStorage.getString("nuvio_cwe_$key")
    actual fun savePayload(key: String, payload: String) { WebStorage.setString("nuvio_cwe_$key", payload) }
}
