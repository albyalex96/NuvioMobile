package com.nuvio.app.features.watched

import com.nuvio.app.core.platform.WebKeyValueStorage

actual object WatchedStorage {
    private const val namespace = "nuvio_watched"

    actual fun loadPayload(profileId: Int): String? =
        WebKeyValueStorage.getString(namespace, payloadKey(profileId))

    actual fun savePayload(profileId: Int, payload: String) {
        WebKeyValueStorage.setString(namespace, payloadKey(profileId), payload)
    }

    private fun payloadKey(profileId: Int): String = "watched_payload_$profileId"
}
