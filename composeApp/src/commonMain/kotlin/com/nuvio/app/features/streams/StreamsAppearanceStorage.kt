package com.nuvio.app.features.streams

internal expect object StreamsAppearanceStorage {
    fun saveDisplayMode(mode: DisplayMode)
    fun loadDisplayMode(): DisplayMode
}