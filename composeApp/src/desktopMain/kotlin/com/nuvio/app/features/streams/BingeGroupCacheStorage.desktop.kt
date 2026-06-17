package com.nuvio.app.features.streams

import com.nuvio.app.core.storage.DesktopStorage

internal actual object BingeGroupCacheStorage {
    private val store = DesktopStorage.store("nuvio_binge_group_cache")

    actual fun load(hashedKey: String): String? =
        store.getString("binge_group_$hashedKey")

    actual fun save(hashedKey: String, value: String) {
        store.putString("binge_group_$hashedKey", value)
    }

    actual fun remove(hashedKey: String) {
        store.remove("binge_group_$hashedKey")
    }
}
