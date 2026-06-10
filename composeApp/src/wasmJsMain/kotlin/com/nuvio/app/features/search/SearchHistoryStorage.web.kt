package com.nuvio.app.features.search

import com.nuvio.app.core.platform.WebKeyValueStorage
import com.nuvio.app.core.storage.ProfileScopedKey

internal actual object SearchHistoryStorage {
    private const val namespace = "nuvio_search_history"
    private const val payloadKey = "search_history_payload"

    actual fun loadPayload(): String? =
        WebKeyValueStorage.getString(namespace, ProfileScopedKey.of(payloadKey))

    actual fun savePayload(payload: String) {
        WebKeyValueStorage.setString(namespace, ProfileScopedKey.of(payloadKey), payload)
    }
}
