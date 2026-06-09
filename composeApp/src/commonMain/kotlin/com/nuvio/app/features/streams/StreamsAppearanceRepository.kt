package com.nuvio.app.features.streams

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object StreamsAppearanceRepository {

    private val _uiState = MutableStateFlow(StreamsAppearanceSettings())
    val uiState: StateFlow<StreamsAppearanceSettings> = _uiState.asStateFlow()

    private var hasLoaded = false
    private var displayMode: DisplayMode = DisplayMode.POLISHED
    private var badgeAnimationsEnabled: Boolean = true

    fun ensureLoaded() {
        if (hasLoaded) return
        loadFromDisk()
    }

    fun setDisplayMode(value: DisplayMode) {
        ensureLoaded()
        if (displayMode == value) return
        displayMode = value
        publish()
        StreamsAppearanceStorage.saveDisplayMode(value)
    }

    fun setBadgeAnimationsEnabled(enabled: Boolean) {
        ensureLoaded()
        if (badgeAnimationsEnabled == enabled) return
        badgeAnimationsEnabled = enabled
        publish()
        StreamsAppearanceStorage.saveBadgeAnimationsEnabled(enabled)
    }

    private fun loadFromDisk() {
        hasLoaded = true
        displayMode = StreamsAppearanceStorage.loadDisplayMode()
        badgeAnimationsEnabled = StreamsAppearanceStorage.loadBadgeAnimationsEnabled()
        publish()
    }

    private fun publish() {
        _uiState.value = StreamsAppearanceSettings(
            displayMode = displayMode,
            badgeAnimationsEnabled = badgeAnimationsEnabled,
        )
    }
}