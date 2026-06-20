package com.nuvio.app.features.mal

import com.nuvio.app.core.storage.DesktopStorage

internal actual object MalLibraryStorage {
    private val store = DesktopStorage.store("nuvio_mal_library")

    actual fun loadPayload(): String? = store.getString("mal_library_payload")

    actual fun savePayload(payload: String) = store.putString("mal_library_payload", payload)
}
