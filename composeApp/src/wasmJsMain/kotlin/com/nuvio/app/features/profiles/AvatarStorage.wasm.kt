package com.nuvio.app.features.profiles

import com.nuvio.app.WebStorage

internal actual object AvatarStorage {
    private const val KEY = "nuvio_avatars"

    actual fun loadPayload(): String? = WebStorage.getString(KEY)
    actual fun savePayload(payload: String) { WebStorage.setString(KEY, payload) }
}
