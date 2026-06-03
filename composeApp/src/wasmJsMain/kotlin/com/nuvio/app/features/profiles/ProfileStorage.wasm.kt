package com.nuvio.app.features.profiles

import com.nuvio.app.WebStorage

internal actual object ProfileStorage {
    private const val KEY = "nuvio_profiles"

    actual fun loadPayload(): String? = WebStorage.getString(KEY)
    actual fun savePayload(payload: String) { WebStorage.setString(KEY, payload) }
}
