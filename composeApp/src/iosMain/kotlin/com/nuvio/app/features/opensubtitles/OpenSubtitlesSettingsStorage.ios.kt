package com.nuvio.app.features.opensubtitles

import com.nuvio.app.core.storage.ProfileScopedKey
import platform.Foundation.NSUserDefaults

actual object OpenSubtitlesSettingsStorage {
    private const val enabledKey = "opensubtitles_enabled"
    private const val apiKeyKey = "opensubtitles_api_key"
    private const val languagesKey = "opensubtitles_languages"

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
}
