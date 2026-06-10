package com.nuvio.app.core.ui

import com.nuvio.app.core.platform.WebKeyValueStorage
import com.nuvio.app.core.storage.ProfileScopedKey

internal actual object PosterCardStyleStorage {
    private const val namespace = "nuvio_poster_card_style"
    private const val payloadKey = "poster_card_style_payload"

    actual fun loadPayload(): String? =
        WebKeyValueStorage.getString(namespace, ProfileScopedKey.of(payloadKey))

    actual fun savePayload(payload: String) {
        WebKeyValueStorage.setString(namespace, ProfileScopedKey.of(payloadKey), payload)
    }
}
