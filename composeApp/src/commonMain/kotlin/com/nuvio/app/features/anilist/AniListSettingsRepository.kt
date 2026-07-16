package com.nuvio.app.features.anilist

import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object AniListSettingsRepository {
    private val log = Logger.withTag("AniListSettings")
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val _uiState = MutableStateFlow(AniListSettingsUiState())
    val uiState: StateFlow<AniListSettingsUiState> = _uiState.asStateFlow()

    private var hasLoaded = false

    fun ensureLoaded() {
        if (hasLoaded) return
        loadFromDisk()
    }

    fun onProfileChanged() {
        loadFromDisk()
    }

    fun clearLocalState() {
        hasLoaded = false
        _uiState.value = AniListSettingsUiState()
        persist()
    }

    fun setEnableSync(enabled: Boolean) {
        ensureLoaded()
        if (_uiState.value.enableSync == enabled) return
        _uiState.value = _uiState.value.copy(enableSync = enabled)
        persist()
    }

    fun setSyncWatching(enabled: Boolean) {
        ensureLoaded()
        if (_uiState.value.syncWatching == enabled) return
        _uiState.value = _uiState.value.copy(syncWatching = enabled)
        persist()
    }

    fun setAutoSync(enabled: Boolean) {
        ensureLoaded()
        if (_uiState.value.autoSync == enabled) return
        _uiState.value = _uiState.value.copy(autoSync = enabled)
        persist()
    }

    fun setSyncOnLaunch(enabled: Boolean) {
        ensureLoaded()
        if (_uiState.value.syncOnLaunch == enabled) return
        _uiState.value = _uiState.value.copy(syncOnLaunch = enabled)
        persist()
    }

    fun updateLastSyncTimestamp(timestamp: Long) {
        ensureLoaded()
        if (_uiState.value.lastSyncTimestamp == timestamp) return
        _uiState.value = _uiState.value.copy(lastSyncTimestamp = timestamp)
        persist()
    }

    fun setSectionEnabled(type: String, enabled: Boolean) {
        ensureLoaded()
        val currentSections = _uiState.value.librarySections.toMutableList()
        val index = currentSections.indexOfFirst { it.type == type }
        if (index != -1) {
            currentSections[index] = currentSections[index].copy(enabled = enabled)
            _uiState.value = _uiState.value.copy(librarySections = currentSections)
            persist()
        }
    }

    fun moveSection(fromIndex: Int, toIndex: Int) {
        ensureLoaded()
        val currentSections = _uiState.value.librarySections.toMutableList()
        if (fromIndex in currentSections.indices && toIndex in currentSections.indices) {
            val item = currentSections.removeAt(fromIndex)
            currentSections.add(toIndex, item)
            _uiState.value = _uiState.value.copy(librarySections = currentSections)
            persist()
        }
    }

    fun resetLibrarySections() {
        ensureLoaded()
        _uiState.value = _uiState.value.copy(librarySections = AniListSettingsUiState.defaultLibrarySections)
        persist()
    }

    fun setMarkWatchedThreshold(threshold: Float) {
        ensureLoaded()
        val clamped = threshold.coerceIn(0f, 1f)
        if (_uiState.value.markWatchedThreshold == clamped) return
        _uiState.value = _uiState.value.copy(markWatchedThreshold = clamped)
        persist()
    }

    fun setAutoAddNewAnime(enabled: Boolean) {
        ensureLoaded()
        if (_uiState.value.autoAddNewAnime == enabled) return
        _uiState.value = _uiState.value.copy(autoAddNewAnime = enabled)
        persist()
    }

    private fun loadFromDisk() {
        hasLoaded = true
        val payload = AniListStorage.loadSettingsPayload().orEmpty().trim()
        val loadedState = if (payload.isBlank()) {
            AniListSettingsUiState()
        } else {
            runCatching { json.decodeFromString<AniListSettingsUiState>(payload) }
                .getOrElse {
                    log.w { "Failed to parse AniList settings payload: ${it.message}" }
                    AniListSettingsUiState()
                }
        }
        _uiState.value = loadedState
    }

    private fun persist() {
        runCatching {
            val payload = json.encodeToString(_uiState.value)
            AniListStorage.saveSettingsPayload(payload)
        }.onFailure {
            log.w { "Failed to persist AniList settings state: ${it.message}" }
        }
    }
}
