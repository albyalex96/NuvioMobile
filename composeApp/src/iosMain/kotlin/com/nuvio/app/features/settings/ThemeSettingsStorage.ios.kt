package com.nuvio.app.features.settings

import com.nuvio.app.core.sync.decodeSyncBoolean
import com.nuvio.app.core.sync.decodeSyncString
import com.nuvio.app.core.sync.encodeSyncBoolean
import com.nuvio.app.core.sync.encodeSyncString
import com.nuvio.app.core.storage.ProfileScopedKey
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import platform.Foundation.NSUserDefaults

actual object ThemeSettingsStorage {
    private const val selectedThemeKey = "selected_theme"
    private const val customAccentHexKey = "custom_accent_hex"
    private const val customThemeFirstColorKey = "custom_theme_first_color"
    private const val customThemeSecondColorKey = "custom_theme_second_color"
    private const val themeAnimationStyleKey = "theme_animation_style"
    private const val selectedAppIconIdKey = "selected_app_icon_id"
    private const val amoledEnabledKey = "amoled_enabled"
    private const val amoledSurfacesEnabledKey = "amoled_surfaces_enabled"
    private const val liquidGlassNativeTabBarEnabledKey = "liquid_glass_native_tab_bar_enabled"
    private const val glassNavBarEnabledKey = "glass_nav_bar_enabled"
    private const val selectedAppLanguageKey = "selected_app_language"
    private const val dateFormatOptionKey = "date_format_option"
    private val profileScopedSyncKeys = listOf(
        selectedThemeKey,
        amoledEnabledKey,
        amoledSurfacesEnabledKey,
        liquidGlassNativeTabBarEnabledKey,
        glassNavBarEnabledKey,
    )

    actual fun loadSelectedTheme(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(ProfileScopedKey.of(selectedThemeKey))

    actual fun saveSelectedTheme(themeName: String) {
        NSUserDefaults.standardUserDefaults.setObject(themeName, forKey = ProfileScopedKey.of(selectedThemeKey))
    }

    actual fun loadCustomAccentHex(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(ProfileScopedKey.of(customAccentHexKey))

    actual fun saveCustomAccentHex(hex: String) {
        NSUserDefaults.standardUserDefaults.setObject(hex, forKey = ProfileScopedKey.of(customAccentHexKey))
    }

    actual fun loadCustomThemeFirstColor(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(ProfileScopedKey.of(customThemeFirstColorKey))

    actual fun saveCustomThemeFirstColor(hex: String) {
        NSUserDefaults.standardUserDefaults.setObject(hex, forKey = ProfileScopedKey.of(customThemeFirstColorKey))
    }

    actual fun loadCustomThemeSecondColor(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(ProfileScopedKey.of(customThemeSecondColorKey))

    actual fun saveCustomThemeSecondColor(hex: String) {
        NSUserDefaults.standardUserDefaults.setObject(hex, forKey = ProfileScopedKey.of(customThemeSecondColorKey))
    }

    actual fun loadThemeAnimationStyle(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(ProfileScopedKey.of(themeAnimationStyleKey))

    actual fun saveThemeAnimationStyle(style: String) {
        NSUserDefaults.standardUserDefaults.setObject(style, forKey = ProfileScopedKey.of(themeAnimationStyleKey))
    }

    actual fun loadSelectedAppIconId(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(ProfileScopedKey.of(selectedAppIconIdKey))

    actual fun saveSelectedAppIconId(iconId: String) {
        NSUserDefaults.standardUserDefaults.setObject(iconId, forKey = ProfileScopedKey.of(selectedAppIconIdKey))
    }

    actual fun loadAmoledEnabled(): Boolean? {
        val defaults = NSUserDefaults.standardUserDefaults
        val key = ProfileScopedKey.of(amoledEnabledKey)
        return if (defaults.objectForKey(key) != null) {
            defaults.boolForKey(key)
        } else {
            null
        }
    }

    actual fun saveAmoledEnabled(enabled: Boolean) {
        NSUserDefaults.standardUserDefaults.setBool(enabled, forKey = ProfileScopedKey.of(amoledEnabledKey))
    }

    actual fun loadAmoledSurfacesEnabled(): Boolean? {
        val defaults = NSUserDefaults.standardUserDefaults
        val key = ProfileScopedKey.of(amoledSurfacesEnabledKey)
        return if (defaults.objectForKey(key) != null) {
            defaults.boolForKey(key)
        } else {
            null
        }
    }

    actual fun saveAmoledSurfacesEnabled(enabled: Boolean) {
        NSUserDefaults.standardUserDefaults.setBool(enabled, forKey = ProfileScopedKey.of(amoledSurfacesEnabledKey))
    }

    actual fun loadLiquidGlassNativeTabBarEnabled(): Boolean? {
        val defaults = NSUserDefaults.standardUserDefaults
        val key = ProfileScopedKey.of(liquidGlassNativeTabBarEnabledKey)
        return if (defaults.objectForKey(key) != null) {
            defaults.boolForKey(key)
        } else {
            null
        }
    }

    actual fun saveLiquidGlassNativeTabBarEnabled(enabled: Boolean) {
        NSUserDefaults.standardUserDefaults.setBool(
            enabled,
            forKey = ProfileScopedKey.of(liquidGlassNativeTabBarEnabledKey),
        )
    }

    actual fun loadGlassNavBarEnabled(): Boolean? {
        val defaults = NSUserDefaults.standardUserDefaults
        val key = ProfileScopedKey.of(glassNavBarEnabledKey)
        return if (defaults.objectForKey(key) != null) {
            defaults.boolForKey(key)
        } else {
            null
        }
    }

    actual fun saveGlassNavBarEnabled(enabled: Boolean) {
        NSUserDefaults.standardUserDefaults.setBool(
            enabled,
            forKey = ProfileScopedKey.of(glassNavBarEnabledKey),
        )
    }

    actual fun loadSelectedAppLanguage(): String? {
        val value = NSUserDefaults.standardUserDefaults.stringForKey(selectedAppLanguageKey)
        if (value != null) return value
        val legacy = NSUserDefaults.standardUserDefaults.stringForKey(ProfileScopedKey.of(selectedAppLanguageKey))
        if (legacy != null) saveSelectedAppLanguage(legacy)
        return legacy
    }

    actual fun saveSelectedAppLanguage(languageCode: String) {
        NSUserDefaults.standardUserDefaults.setObject(languageCode, forKey = selectedAppLanguageKey)
    }

    actual fun applySelectedAppLanguage(languageCode: String) {
        if (languageCode.equals("device", ignoreCase = true)) {
            NSUserDefaults.standardUserDefaults.removeObjectForKey("AppleLanguages")
            NSUserDefaults.standardUserDefaults.synchronize()
            return
        }
        val normalizedCode = languageCode
            .trim()
            .takeIf { it.isNotBlank() }
            ?: AppLanguage.ENGLISH.code
        NSUserDefaults.standardUserDefaults.setObject(
            listOf(normalizedCode),
            forKey = "AppleLanguages",
        )
        NSUserDefaults.standardUserDefaults.synchronize()
    }

    actual fun loadDateFormatOption(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(dateFormatOptionKey)

    actual fun saveDateFormatOption(format: String) {
        NSUserDefaults.standardUserDefaults.setObject(format, forKey = dateFormatOptionKey)
    }

    actual fun exportToSyncPayload(): JsonObject = buildJsonObject {
        loadSelectedTheme()?.let { put(selectedThemeKey, encodeSyncString(it)) }
        loadCustomAccentHex()?.let { put(customAccentHexKey, encodeSyncString(it)) }
        loadCustomThemeFirstColor()?.let { put(customThemeFirstColorKey, encodeSyncString(it)) }
        loadCustomThemeSecondColor()?.let { put(customThemeSecondColorKey, encodeSyncString(it)) }
        loadThemeAnimationStyle()?.let { put(themeAnimationStyleKey, encodeSyncString(it)) }
        loadAmoledEnabled()?.let { put(amoledEnabledKey, encodeSyncBoolean(it)) }
        loadAmoledSurfacesEnabled()?.let { put(amoledSurfacesEnabledKey, encodeSyncBoolean(it)) }
        loadLiquidGlassNativeTabBarEnabled()?.let { put(liquidGlassNativeTabBarEnabledKey, encodeSyncBoolean(it)) }
        loadGlassNavBarEnabled()?.let { put(glassNavBarEnabledKey, encodeSyncBoolean(it)) }
    }

    actual fun replaceFromSyncPayload(payload: JsonObject) {
        profileScopedSyncKeys.forEach { key ->
            NSUserDefaults.standardUserDefaults.removeObjectForKey(ProfileScopedKey.of(key))
        }

        payload.decodeSyncString(selectedThemeKey)?.let(::saveSelectedTheme)
        payload.decodeSyncString(customAccentHexKey)?.let(::saveCustomAccentHex)
        payload.decodeSyncString(customThemeFirstColorKey)?.let(::saveCustomThemeFirstColor)
        payload.decodeSyncString(customThemeSecondColorKey)?.let(::saveCustomThemeSecondColor)
        payload.decodeSyncString(themeAnimationStyleKey)?.let(::saveThemeAnimationStyle)
        payload.decodeSyncBoolean(amoledEnabledKey)?.let(::saveAmoledEnabled)
        payload.decodeSyncBoolean(amoledSurfacesEnabledKey)?.let(::saveAmoledSurfacesEnabled)
        payload.decodeSyncBoolean(liquidGlassNativeTabBarEnabledKey)?.let(::saveLiquidGlassNativeTabBarEnabled)
        payload.decodeSyncBoolean(glassNavBarEnabledKey)?.let(::saveGlassNavBarEnabled)
        applySelectedAppLanguage(loadSelectedAppLanguage() ?: AppLanguage.DEVICE.code)
    }
}
