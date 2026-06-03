package com.nuvio.app.features.home

import com.nuvio.app.WebStorage

internal actual object Top10CatalogStorage {
    private const val KEY = "nuvio_top10_catalog"

    actual fun loadPayload(): String? = WebStorage.getString(KEY)
    actual fun savePayload(payload: String) { WebStorage.setString(KEY, payload) }
}
