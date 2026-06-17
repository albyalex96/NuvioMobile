package com.nuvio.app.features.streams

import com.nuvio.app.core.storage.DesktopStorage

internal actual object StreamLinkCacheStorage {
    private val store = DesktopStorage.store("nuvio_stream_link_cache")

    actual fun loadEntry(hashedKey: String): String? =
        store.getString("cache_entry_$hashedKey")

    actual fun saveEntry(hashedKey: String, payload: String) {
        store.putString("cache_entry_$hashedKey", payload)
    }

    actual fun removeEntry(hashedKey: String) {
        store.remove("cache_entry_$hashedKey")
    }
}
