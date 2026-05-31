package com.nuvio.app.features.streams

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object StreamsAppearanceRepository {

    private val _uiState = MutableStateFlow(StreamsAppearanceSettings())
    val uiState: StateFlow<StreamsAppearanceSettings> = _uiState.asStateFlow()

    private var hasLoaded = false
    private var displayMode: DisplayMode = DisplayMode.POLISHED

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

    private fun loadFromDisk() {
        hasLoaded = true
        displayMode = StreamsAppearanceStorage.loadDisplayMode()
        publish()
    }

    private fun publish() {
        _uiState.value = StreamsAppearanceSettings(
            displayMode = displayMode,
        )
    }
}