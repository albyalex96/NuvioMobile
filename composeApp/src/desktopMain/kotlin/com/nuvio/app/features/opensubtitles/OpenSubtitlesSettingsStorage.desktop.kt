package com.nuvio.app.features.opensubtitles

import com.nuvio.app.core.storage.DesktopStorage

actual object OpenSubtitlesSettingsStorage {
    private val store = DesktopStorage.store("nuvio_opensubtitles_settings")

    actual fun loadEnabled(): Boolean? =
        if (store.contains("opensubtitles_enabled")) store.getBoolean("opensubtitles_enabled") else null

    actual fun saveEnabled(enabled: Boolean) { store.putBoolean("opensubtitles_enabled", enabled) }

    actual fun loadApiKey(): String? = store.getString("opensubtitles_api_key")

    actual fun saveApiKey(apiKey: String) { store.putString("opensubtitles_api_key", apiKey) }

    actual fun loadLanguages(): Set<String>? {
        val raw = store.getString("opensubtitles_languages") ?: return null
        if (raw.isBlank()) return emptySet()
        return raw.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet()
    }

    actual fun saveLanguages(languages: Set<String>) {
        val raw = if (languages.isEmpty()) "" else languages.joinToString(",")
        store.putString("opensubtitles_languages", raw)
    }
}
