package com.nuvio.app.features.streams

import com.nuvio.app.WebStorage

internal actual object StreamLinkCacheStorage {
    private const val PREFIX = "nuvio_stream_link_cache_"

    actual fun loadEntry(hashedKey: String): String? = WebStorage.getString("$PREFIX$hashedKey")
    actual fun saveEntry(hashedKey: String, payload: String) { WebStorage.setString("$PREFIX$hashedKey", payload) }
    actual fun removeEntry(hashedKey: String) { WebStorage.remove("$PREFIX$hashedKey") }
}
