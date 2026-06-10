package com.nuvio.app.features.streams

import com.nuvio.app.core.platform.WebKeyValueStorage

internal actual object StreamsAppearanceStorage {
    private const val namespace = "nuvio_streams_appearance"

    actual fun saveDisplayMode(mode: DisplayMode) =
        WebKeyValueStorage.setString(namespace, "display_mode", mode.name)

    actual fun loadDisplayMode(): DisplayMode =
        DisplayMode.fromString(WebKeyValueStorage.getString(namespace, "display_mode"))

    actual fun saveBadgeAnimationsEnabled(enabled: Boolean) =
        WebKeyValueStorage.setBoolean(namespace, "badge_animations", enabled)

    actual fun loadBadgeAnimationsEnabled(): Boolean =
        WebKeyValueStorage.getBoolean(namespace, "badge_animations") ?: true
}
