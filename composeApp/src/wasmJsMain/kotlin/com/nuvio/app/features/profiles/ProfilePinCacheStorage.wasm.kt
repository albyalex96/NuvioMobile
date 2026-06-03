package com.nuvio.app.features.profiles

import com.nuvio.app.WebStorage

internal actual object ProfilePinCacheStorage {
    private const val PREFIX = "nuvio_pin_cache_"

    actual fun loadPayload(profileIndex: Int): String? = WebStorage.getString("$PREFIX$profileIndex")
    actual fun savePayload(profileIndex: Int, payload: String) { WebStorage.setString("$PREFIX$profileIndex", payload) }
    actual fun removePayload(profileIndex: Int) { WebStorage.remove("$PREFIX$profileIndex") }
}
