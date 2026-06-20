package com.nuvio.app.features.mal

import com.nuvio.app.core.storage.DesktopStorage

internal actual object MalAuthStorage {
    private val store = DesktopStorage.store("nuvio_mal_auth")

    actual fun loadPayload(): String? = store.getString("mal_auth_payload")

    actual fun savePayload(payload: String) = store.putString("mal_auth_payload", payload)
}
