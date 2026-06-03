package com.nuvio.app.features.streams

import com.nuvio.app.WebStorage

internal actual object StreamsAppearanceStorage {
    private const val KEY = "nuvio_streams_appearance"

    actual fun saveDisplayMode(mode: DisplayMode) {
        WebStorage.setString(KEY, mode.name)
    }

    actual fun loadDisplayMode(): DisplayMode {
        val raw = WebStorage.getString(KEY)
        return DisplayMode.fromString(raw)
    }
}
