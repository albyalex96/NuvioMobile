package com.nuvio.app.features.opensubtitles

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object OpenSubtitlesSettingsRepository {
    private val _uiState = MutableStateFlow(OpenSubtitlesSettings())
    val uiState: StateFlow<OpenSubtitlesSettings> = _uiState.asStateFlow()

    private var hasLoaded = false

    private var enabled = false
    private var apiKey = ""
    private var languages = emptySet<String>()

    fun ensureLoaded() {
        if (hasLoaded) return
        loadFromDisk()
    }

    fun onProfileChanged() {
        loadFromDisk()
    }

    fun snapshot(): OpenSubtitlesSettings {
        ensureLoaded()
        return _uiState.value
    }

    fun setEnabled(value: Boolean) {
        ensureLoaded()
        if (value && apiKey.isBlank()) return
        if (enabled == value) return
        enabled = value
        publish()
        OpenSubtitlesSettingsStorage.saveEnabled(value)
    }

    fun setApiKey(value: String) {
        ensureLoaded()
        val normalized = value.trim()
        if (apiKey == normalized) return
        apiKey = normalized
        if (apiKey.isBlank()) {
            enabled = false
            OpenSubtitlesSettingsStorage.saveEnabled(false)
        }
        publish()
        OpenSubtitlesSettingsStorage.saveApiKey(normalized)
    }

    fun setLanguages(value: Set<String>) {
        ensureLoaded()
        if (languages == value) return
        languages = value
        publish()
        OpenSubtitlesSettingsStorage.saveLanguages(value)
    }

    private fun loadFromDisk() {
        hasLoaded = true
        enabled = OpenSubtitlesSettingsStorage.loadEnabled() ?: false
        apiKey = OpenSubtitlesSettingsStorage.loadApiKey()?.trim().orEmpty()
        languages = OpenSubtitlesSettingsStorage.loadLanguages() ?: emptySet()
        if (apiKey.isBlank()) enabled = false
        publish()
    }

    private fun publish() {
        _uiState.value = OpenSubtitlesSettings(
            enabled = enabled,
            apiKey = apiKey,
            languages = languages,
        )
    }
}
