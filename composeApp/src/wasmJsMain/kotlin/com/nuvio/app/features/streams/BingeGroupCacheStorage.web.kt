package com.nuvio.app.features.streams

import com.nuvio.app.core.platform.WebKeyValueStorage

internal actual object BingeGroupCacheStorage {
    private const val namespace = "nuvio_binge"

    actual fun load(hashedKey: String): String? =
        WebKeyValueStorage.getString(namespace, hashedKey)

    actual fun save(hashedKey: String, value: String) =
        WebKeyValueStorage.setString(namespace, hashedKey, value)

    actual fun remove(hashedKey: String) =
        WebKeyValueStorage.remove(namespace, hashedKey)
}
