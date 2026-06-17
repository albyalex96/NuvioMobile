package com.nuvio.app.core.ui

import com.nuvio.app.core.storage.DesktopStorage

internal actual object PosterCardStyleStorage {
    private val store = DesktopStorage.store("nuvio_poster_card_style")

    actual fun loadPayload(): String? =
        store.getString("poster_card_payload")

    actual fun savePayload(payload: String) {
        store.putString("poster_card_payload", payload)
    }
}
