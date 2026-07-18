package com.nuvio.app.features.opensubtitles

import com.nuvio.app.core.storage.DesktopStorage
import com.nuvio.app.core.sync.decodeSyncBoolean
import com.nuvio.app.core.sync.decodeSyncString
import com.nuvio.app.core.sync.decodeSyncStringSet
import com.nuvio.app.core.sync.encodeSyncBoolean
import com.nuvio.app.core.sync.encodeSyncString
import com.nuvio.app.core.sync.encodeSyncStringSet
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

actual object OpenSubtitlesSettingsStorage {
    private val store = DesktopStorage.store("nuvio_opensubtitles_settings")
    private const val enabledKey = "opensubtitles_enabled"
    private const val apiKeyKey = "opensubtitles_api_key"
    private const val languagesKey = "opensubtitles_languages"
    private val syncKeys = listOf(enabledKey, apiKeyKey, languagesKey)

    actual fun loadEnabled(): Boolean? =
        if (store.contains(enabledKey)) store.getBoolean(enabledKey) else null

    actual fun saveEnabled(enabled: Boolean) { store.putBoolean(enabledKey, enabled) }

    actual fun loadApiKey(): String? = store.getString(apiKeyKey)

    actual fun saveApiKey(apiKey: String) { store.putString(apiKeyKey, apiKey) }

    actual fun loadLanguages(): Set<String>? {
        val raw = store.getString(languagesKey) ?: return null
        if (raw.isBlank()) return emptySet()
        return raw.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet()
    }

    actual fun saveLanguages(languages: Set<String>) {
        val raw = if (languages.isEmpty()) "" else languages.joinToString(",")
        store.putString(languagesKey, raw)
    }

    actual fun exportToSyncPayload(): JsonObject = buildJsonObject {
        loadEnabled()?.let { put(enabledKey, encodeSyncBoolean(it)) }
        loadApiKey()?.let { put(apiKeyKey, encodeSyncString(it)) }
        loadLanguages()?.let { put(languagesKey, encodeSyncStringSet(it)) }
    }

    actual fun replaceFromSyncPayload(payload: JsonObject) {
        syncKeys.forEach { store.remove(it) }

        payload.decodeSyncBoolean(enabledKey)?.let(::saveEnabled)
        payload.decodeSyncString(apiKeyKey)?.let(::saveApiKey)
        payload.decodeSyncStringSet(languagesKey)?.let(::saveLanguages)
    }
}
