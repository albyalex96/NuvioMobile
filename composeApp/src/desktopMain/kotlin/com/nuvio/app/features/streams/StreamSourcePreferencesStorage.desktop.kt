package com.nuvio.app.features.streams

import com.nuvio.app.core.storage.DesktopStorage

internal actual object StreamSourcePreferencesStorage {
    private val store = DesktopStorage.store("nuvio_stream_source_preferences")

    actual fun loadPinnedSourcesPayload(): String? = store.getString("pinned_sources")

    actual fun savePinnedSourcesPayload(payload: String) {
        store.putString("pinned_sources", payload)
        store.remove("pinned_source_id")
        store.remove("pinned_source_name")
    }

    actual fun loadLegacyPinnedSourceId(): String? = store.getString("pinned_source_id")

    actual fun loadLegacyPinnedSourceName(): String? = store.getString("pinned_source_name")

    actual fun clearPinnedSources() {
        store.remove("pinned_sources")
        store.remove("pinned_source_id")
        store.remove("pinned_source_name")
    }
}
