package com.nuvio.app.features.streams

import com.nuvio.app.core.storage.DesktopStorage

internal actual object StreamsAppearanceStorage {
    private val store = DesktopStorage.store("nuvio_streams_appearance")

    actual fun saveDisplayMode(mode: DisplayMode) {
        store.putString("display_mode", mode.name)
    }

    actual fun loadDisplayMode(): DisplayMode {
        val raw = store.getString("display_mode")
        return DisplayMode.fromString(raw)
    }

    actual fun saveBadgeAnimationsEnabled(enabled: Boolean) {
        store.putBoolean("badge_animations", enabled)
    }

    actual fun loadBadgeAnimationsEnabled(): Boolean =
        store.getBoolean("badge_animations") ?: true

    actual fun saveSortByQuality(enabled: Boolean) {
        store.putBoolean("sort_by_quality", enabled)
    }

    actual fun loadSortByQuality(): Boolean =
        store.getBoolean("sort_by_quality") ?: false
}
