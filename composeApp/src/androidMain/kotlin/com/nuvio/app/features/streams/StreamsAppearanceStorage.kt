package com.nuvio.app.features.streams

import android.content.Context
import android.content.SharedPreferences

internal actual object StreamsAppearanceStorage {

    private const val PREFS_NAME = "nuvio_streams_appearance"
    private const val KEY_DISPLAY_MODE = "display_mode"
    private const val KEY_BADGE_ANIMATIONS = "badge_animations"
    private const val KEY_SORT_BY_QUALITY = "sort_by_quality"

    private var preferences: SharedPreferences? = null

    fun initialize(context: Context) {
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    actual fun saveDisplayMode(mode: DisplayMode) {
        preferences?.edit()?.putString(KEY_DISPLAY_MODE, mode.name)?.apply()
    }

    actual fun loadDisplayMode(): DisplayMode {
        val raw = preferences?.getString(KEY_DISPLAY_MODE, null)
        return DisplayMode.fromString(raw)
    }

    actual fun saveBadgeAnimationsEnabled(enabled: Boolean) {
        preferences?.edit()?.putBoolean(KEY_BADGE_ANIMATIONS, enabled)?.apply()
    }

    actual fun loadBadgeAnimationsEnabled(): Boolean {
        return preferences?.getBoolean(KEY_BADGE_ANIMATIONS, true) ?: true
    }

    actual fun saveSortByQuality(enabled: Boolean) {
        preferences?.edit()?.putBoolean(KEY_SORT_BY_QUALITY, enabled)?.apply()
    }

    actual fun loadSortByQuality(): Boolean {
        return preferences?.getBoolean(KEY_SORT_BY_QUALITY, false) ?: false
    }
}