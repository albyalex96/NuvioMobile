package com.nuvio.app.features.home

import com.nuvio.app.WebStorage

internal actual object HomeCatalogSettingsStorage {
    private const val KEY = "nuvio_home_catalog_settings"

    actual fun loadPayload(): String? = WebStorage.getString(KEY)
    actual fun savePayload(payload: String) { WebStorage.setString(KEY, payload) }
}
