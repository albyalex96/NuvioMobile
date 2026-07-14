package com.nuvio.app.features.streams

internal actual object StreamSourcePreferencesStorage {
    actual fun loadPinnedSourcesPayload(): String? = null
    actual fun savePinnedSourcesPayload(payload: String) = Unit
    actual fun loadLegacyPinnedSourceId(): String? = null
    actual fun loadLegacyPinnedSourceName(): String? = null
    actual fun clearPinnedSources() = Unit
}
