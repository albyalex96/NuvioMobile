package com.nuvio.app.features.profiles

import com.nuvio.app.core.storage.DesktopStorage

internal actual object ProfilePinCacheStorage {
    private val store = DesktopStorage.store("nuvio_profile_pin_cache")

    actual fun loadPayload(profileIndex: Int): String? =
        store.getString("pin_cache_$profileIndex")

    actual fun savePayload(profileIndex: Int, payload: String) {
        store.putString("pin_cache_$profileIndex", payload)
    }

    actual fun removePayload(profileIndex: Int) {
        store.remove("pin_cache_$profileIndex")
    }
}
