package com.nuvio.app.features.streams

import com.nuvio.app.core.platform.WebKeyValueStorage
import com.nuvio.app.core.storage.ProfileScopedKey

internal actual object StreamLinkCacheStorage {
    private const val namespace = "nuvio_stream_link_cache"

    actual fun loadEntry(hashedKey: String): String? =
        WebKeyValueStorage.getString(namespace, ProfileScopedKey.of(hashedKey))

    actual fun saveEntry(hashedKey: String, payload: String) {
        WebKeyValueStorage.setString(namespace, ProfileScopedKey.of(hashedKey), payload)
    }

    actual fun removeEntry(hashedKey: String) {
        WebKeyValueStorage.remove(namespace, ProfileScopedKey.of(hashedKey))
    }
}
