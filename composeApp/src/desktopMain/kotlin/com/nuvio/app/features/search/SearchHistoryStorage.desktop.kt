package com.nuvio.app.features.search

import com.nuvio.app.core.storage.DesktopStorage

internal actual object SearchHistoryStorage {
    private val store = DesktopStorage.store("nuvio_search_history")

    actual fun loadPayload(): String? = store.getString("search_history_payload")

    actual fun savePayload(payload: String) = store.putString("search_history_payload", payload)
}
