package com.nuvio.app.features.trakt

import com.nuvio.app.core.storage.DesktopStorage

internal actual object TraktAuthStorage {
    private val store = DesktopStorage.store("nuvio_trakt_auth")

    actual fun loadPayload(): String? = store.getString("trakt_auth_payload")

    actual fun savePayload(payload: String) = store.putString("trakt_auth_payload", payload)
}
