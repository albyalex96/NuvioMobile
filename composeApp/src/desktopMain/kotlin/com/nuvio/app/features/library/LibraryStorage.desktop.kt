package com.nuvio.app.features.library

import com.nuvio.app.core.storage.DesktopStorage

internal actual object LibraryStorage {
    private val store = DesktopStorage.store("nuvio_library")

    actual fun loadPayload(profileId: Int): String? =
        store.getString("library_payload_$profileId")

    actual fun savePayload(profileId: Int, payload: String) {
        store.putString("library_payload_$profileId", payload)
    }
}
