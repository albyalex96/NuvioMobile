package com.nuvio.app.features.opensubtitles

import android.content.Context
import android.content.SharedPreferences
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

actual object OpenSubtitlesSettingsStorage {
    private const val preferencesName = "nuvio_opensubtitles_settings"
    private const val enabledKey = "opensubtitles_enabled"
    private const val apiKeyKey = "opensubtitles_api_key"
    private const val languagesKey = "opensubtitles_languages"
    private val syncKeys = listOf(enabledKey, apiKeyKey, languagesKey)

    private var preferences: SharedPreferences? = null

    fun initialize(context: Context) {
        preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
    }

    actual fun loadEnabled(): Boolean? =
        preferences?.let { sp ->
            val scopedKey = ProfileScopedKey.of(enabledKey)
            if (sp.contains(scopedKey)) sp.getBoolean(scopedKey, false) else null
        }

    actual fun saveEnabled(enabled: Boolean) {
        preferences?.edit()?.putBoolean(ProfileScopedKey.of(enabledKey), enabled)?.apply()
    }

    actual fun loadApiKey(): String? =
        preferences?.getString(ProfileScopedKey.of(apiKeyKey), null)

    actual fun saveApiKey(apiKey: String) {
        preferences?.edit()?.putString(ProfileScopedKey.of(apiKeyKey), apiKey)?.apply()
    }

    actual fun loadLanguages(): Set<String>? {
        val raw = preferences?.getString(ProfileScopedKey.of(languagesKey), null) ?: return null
        if (raw.isBlank()) return emptySet()
        return raw.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet()
    }

    actual fun saveLanguages(languages: Set<String>) {
        val raw = if (languages.isEmpty()) "" else languages.joinToString(",")
        preferences?.edit()?.putString(ProfileScopedKey.of(languagesKey), raw)?.apply()
    }

    actual fun exportToSyncPayload(): JsonObject = buildJsonObject {
        loadEnabled()?.let { put(enabledKey, encodeSyncBoolean(it)) }
        loadApiKey()?.let { put(apiKeyKey, encodeSyncString(it)) }
        loadLanguages()?.let { put(languagesKey, encodeSyncStringSet(it)) }
    }

    actual fun replaceFromSyncPayload(payload: JsonObject) {
        preferences?.edit()?.apply {
            syncKeys.forEach { remove(ProfileScopedKey.of(it)) }
        }?.apply()

        payload.decodeSyncBoolean(enabledKey)?.let(::saveEnabled)
        payload.decodeSyncString(apiKeyKey)?.let(::saveApiKey)
        payload.decodeSyncStringSet(languagesKey)?.let(::saveLanguages)
    }
}
