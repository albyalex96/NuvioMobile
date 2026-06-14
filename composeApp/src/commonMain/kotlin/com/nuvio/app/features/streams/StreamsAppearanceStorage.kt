
package com.nuvio.app.features.streams

internal expect object StreamsAppearanceStorage {
    fun saveDisplayMode(mode: DisplayMode)
    fun loadDisplayMode(): DisplayMode
    fun saveBadgeAnimationsEnabled(enabled: Boolean)
    fun loadBadgeAnimationsEnabled(): Boolean
    fun saveSortByQuality(enabled: Boolean)
    fun loadSortByQuality(): Boolean
}