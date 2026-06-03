package com.nuvio.app.features.streams

import com.nuvio.app.WebStorage

internal actual object BingeGroupCacheStorage {
    private const val PREFIX = "nuvio_binge_group_"

    actual fun load(hashedKey: String): String? = WebStorage.getString("$PREFIX$hashedKey")
    actual fun save(hashedKey: String, value: String) { WebStorage.setString("$PREFIX$hashedKey", value) }
    actual fun remove(hashedKey: String) { WebStorage.remove("$PREFIX$hashedKey") }
}
