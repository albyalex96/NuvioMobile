package com.nuvio.app.features.opensubtitles

import com.nuvio.app.core.storage.ProfileScopedKey
import com.nuvio.app.core.sync.decodeSyncBoolean
import com.nuvio.app.core.sync.decodeSyncString
import com.nuvio.app.core.sync.decodeSyncStringSet
import com.nuvio.app.core.sync.encodeSyncBoolean
import com.nuvio.app.core.sync.encodeSyncString
import com.nuvio.app.core.sync.encodeSyncStringSet
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import platform.Foundation.NSUserDefaults

actual object OpenSubtitlesSettingsStorage {
    private const val enabledKey = "opensubtitles_enabled"
    private const val apiKeyKey = "opensubtitles_api_key"
    private const val languagesKey = "opensubtitles_languages"
    private val syncKeys = listOf(enabledKey, apiKeyKey, languagesKey)

    actual fun loadEnabled(): Boolean? {
        val defaults = NSUserDefaults.standardUserDefaults
        val scopedKey = ProfileScopedKey.of(enabledKey)
        return if (defaults.objectForKey(scopedKey) != null) defaults.boolForKey(scopedKey) else null
    }

    actual fun saveEnabled(enabled: Boolean) {
        NSUserDefaults.standardUserDefaults.setBool(enabled, forKey = ProfileScopedKey.of(enabledKey))
    }

    actual fun loadApiKey(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(ProfileScopedKey.of(apiKeyKey))

    actual fun saveApiKey(apiKey: String) {
        NSUserDefaults.standardUserDefaults.setObject(apiKey, forKey = ProfileScopedKey.of(apiKeyKey))
    }

    actual fun loadLanguages(): Set<String>? {
        val raw = NSUserDefaults.standardUserDefaults.stringForKey(ProfileScopedKey.of(languagesKey)) ?: return null
        if (raw.isBlank()) return emptySet()
        return raw.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet()
    }

    actual fun saveLanguages(languages: Set<String>) {
        val raw = if (languages.isEmpty()) "" else languages.joinToString(",")
        NSUserDefaults.standardUserDefaults.setObject(raw, forKey = ProfileScopedKey.of(languagesKey))
    }

    actual fun exportToSyncPayload(): JsonObject = buildJsonObject {
        loadEnabled()?.let { put(enabledKey, encodeSyncBoolean(it)) }
        loadApiKey()?.let { put(apiKeyKey, encodeSyncString(it)) }
        loadLanguages()?.let { put(languagesKey, encodeSyncStringSet(it)) }
    }

    actual fun replaceFromSyncPayload(payload: JsonObject) {
        val defaults = NSUserDefaults.standardUserDefaults
        syncKeys.forEach { defaults.removeObjectForKey(ProfileScopedKey.of(it)) }

        payload.decodeSyncBoolean(enabledKey)?.let(::saveEnabled)
        payload.decodeSyncString(apiKeyKey)?.let(::saveApiKey)
        payload.decodeSyncStringSet(languagesKey)?.let(::saveLanguages)
    }
}
