package com.nuvio.app.features.search

import com.nuvio.app.WebStorage

internal actual object SearchHistoryStorage {
    private const val KEY = "nuvio_search_history"

    actual fun loadPayload(): String? = WebStorage.getString(KEY)
    actual fun savePayload(payload: String) { WebStorage.setString(KEY, payload) }
}
