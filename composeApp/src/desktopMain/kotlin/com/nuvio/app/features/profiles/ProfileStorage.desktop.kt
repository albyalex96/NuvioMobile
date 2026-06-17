package com.nuvio.app.features.profiles

import com.nuvio.app.core.storage.DesktopStorage

internal actual object ProfileStorage {
    private val store = DesktopStorage.store("nuvio_profile_storage")

    actual fun loadPayload(): String? =
        store.getString("profile_payload")

    actual fun savePayload(payload: String) {
        store.putString("profile_payload", payload)
    }
}
