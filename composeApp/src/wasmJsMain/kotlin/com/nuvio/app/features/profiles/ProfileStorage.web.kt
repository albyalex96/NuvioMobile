package com.nuvio.app.features.profiles

import com.nuvio.app.core.platform.WebKeyValueStorage

internal actual object ProfileStorage {
    private const val namespace = "nuvio_profile_cache"
    private const val payloadKey = "profile_payload"

    actual fun loadPayload(): String? =
        WebKeyValueStorage.getString(namespace, payloadKey)

    actual fun savePayload(payload: String) {
        WebKeyValueStorage.setString(namespace, payloadKey, payload)
    }
}
