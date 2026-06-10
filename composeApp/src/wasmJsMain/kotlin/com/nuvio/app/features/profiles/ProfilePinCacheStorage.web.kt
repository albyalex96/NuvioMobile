package com.nuvio.app.features.profiles

import com.nuvio.app.core.platform.WebKeyValueStorage

internal actual object ProfilePinCacheStorage {
    private const val namespace = "nuvio_profile_pin_cache"

    actual fun loadPayload(profileIndex: Int): String? =
        WebKeyValueStorage.getString(namespace, payloadKey(profileIndex))

    actual fun savePayload(profileIndex: Int, payload: String) {
        WebKeyValueStorage.setString(namespace, payloadKey(profileIndex), payload)
    }

    actual fun removePayload(profileIndex: Int) {
        WebKeyValueStorage.remove(namespace, payloadKey(profileIndex))
    }

    private fun payloadKey(profileIndex: Int): String = "profile_pin_cache_$profileIndex"
}
