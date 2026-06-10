package com.nuvio.app.features.watchprogress

import com.nuvio.app.core.platform.WebKeyValueStorage
import com.nuvio.app.core.storage.ProfileScopedKey

internal actual object ContinueWatchingPreferencesStorage {
    private const val namespace = "nuvio_continue_watching_preferences"
    private const val payloadKey = "continue_watching_preferences_payload"

    actual fun loadPayload(): String? =
        WebKeyValueStorage.getString(namespace, ProfileScopedKey.of(payloadKey))

    actual fun savePayload(payload: String) {
        WebKeyValueStorage.setString(namespace, ProfileScopedKey.of(payloadKey), payload)
    }
}
