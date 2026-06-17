package com.nuvio.app.features.trakt

import com.nuvio.app.core.storage.DesktopStorage

internal actual object TraktLibraryStorage {
    private val store = DesktopStorage.store("nuvio_trakt_library")

    actual fun loadPayload(): String? = store.getString("trakt_library_payload")

    actual fun savePayload(payload: String) = store.putString("trakt_library_payload", payload)
}
