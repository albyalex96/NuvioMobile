package com.nuvio.app.features.streams

import platform.Foundation.NSUserDefaults

internal actual object StreamsAppearanceStorage {

    private const val KEY_DISPLAY_MODE = "display_mode"
    private const val KEY_BADGE_ANIMATIONS = "badge_animations"

    actual fun saveDisplayMode(mode: DisplayMode) {
        NSUserDefaults.standardUserDefaults.setObject(mode.name, forKey = KEY_DISPLAY_MODE)
    }

    actual fun loadDisplayMode(): DisplayMode {
        val raw = NSUserDefaults.standardUserDefaults.stringForKey(KEY_DISPLAY_MODE)
        return DisplayMode.fromString(raw)
    }

    actual fun saveBadgeAnimationsEnabled(enabled: Boolean) {
        NSUserDefaults.standardUserDefaults.setBool(enabled, forKey = KEY_BADGE_ANIMATIONS)
    }

    actual fun loadBadgeAnimationsEnabled(): Boolean {
        val defaults = NSUserDefaults.standardUserDefaults
        return if (defaults.objectForKey(KEY_BADGE_ANIMATIONS) != null) {
            defaults.boolForKey(KEY_BADGE_ANIMATIONS)
        } else {
            true
        }
    }
}
