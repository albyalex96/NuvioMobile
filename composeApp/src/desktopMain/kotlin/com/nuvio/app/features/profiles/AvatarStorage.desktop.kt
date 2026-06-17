package com.nuvio.app.features.profiles

import com.nuvio.app.core.storage.DesktopStorage

internal actual object AvatarStorage {
    private val store = DesktopStorage.store("nuvio_avatar_storage")

    actual fun loadPayload(): String? =
        store.getString("avatar_payload")

    actual fun savePayload(payload: String) {
        store.putString("avatar_payload", payload)
    }
}
