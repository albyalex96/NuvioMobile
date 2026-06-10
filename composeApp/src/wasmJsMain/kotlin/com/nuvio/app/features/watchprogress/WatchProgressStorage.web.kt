package com.nuvio.app.features.watchprogress

import com.nuvio.app.core.platform.WebKeyValueStorage

internal actual object WatchProgressStorage {
    private const val namespace = "nuvio_watchprogress"

    actual fun loadPayload(profileId: Int): String? =
        WebKeyValueStorage.getString(namespace, payloadKey(profileId))

    actual fun savePayload(profileId: Int, payload: String) {
        WebKeyValueStorage.setString(namespace, payloadKey(profileId), payload)
    }

    private fun payloadKey(profileId: Int): String = "watchprogress_payload_$profileId"
}
