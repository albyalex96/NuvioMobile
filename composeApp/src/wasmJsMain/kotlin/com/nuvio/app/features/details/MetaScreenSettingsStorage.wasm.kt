package com.nuvio.app.features.details

import com.nuvio.app.WebStorage

internal actual object MetaScreenSettingsStorage {
    private const val KEY = "nuvio_meta_screen_settings"

    actual fun loadPayload(): String? = WebStorage.getString(KEY)
    actual fun savePayload(payload: String) { WebStorage.setString(KEY, payload) }
}
