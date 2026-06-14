package com.nuvio.app.features.streams

data class StreamsAppearanceSettings(
    val displayMode: DisplayMode = DisplayMode.ORIGINAL,
    val badgeAnimationsEnabled: Boolean = true,
    val sortByQuality: Boolean = false,
)