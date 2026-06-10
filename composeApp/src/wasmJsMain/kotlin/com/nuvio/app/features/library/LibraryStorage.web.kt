package com.nuvio.app.features.library

import com.nuvio.app.core.platform.WebKeyValueStorage

internal actual object LibraryStorage {
    private const val namespace = "nuvio_library"

    actual fun loadPayload(profileId: Int): String? =
        WebKeyValueStorage.getString(namespace, payloadKey(profileId))

    actual fun savePayload(profileId: Int, payload: String) {
        WebKeyValueStorage.setString(namespace, payloadKey(profileId), payload)
    }

    private fun payloadKey(profileId: Int): String = "library_payload_$profileId"
}
