package com.nuvio.app.features.collection

import com.nuvio.app.core.platform.WebKeyValueStorage
import com.nuvio.app.core.storage.ProfileScopedKey

internal actual object CollectionMobileSettingsStorage {
    private const val namespace = "nuvio_collection_mobile_settings"
    private const val payloadKey = "collection_mobile_settings_payload"

    actual fun loadPayload(): String? =
        WebKeyValueStorage.getString(namespace, ProfileScopedKey.of(payloadKey))

    actual fun savePayload(payload: String) {
        WebKeyValueStorage.setString(namespace, ProfileScopedKey.of(payloadKey), payload)
    }
}
