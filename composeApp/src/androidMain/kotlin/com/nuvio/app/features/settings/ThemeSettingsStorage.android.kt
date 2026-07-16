package com.nuvio.app.features.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.nuvio.app.core.sync.decodeSyncBoolean
import com.nuvio.app.core.sync.decodeSyncString
import com.nuvio.app.core.sync.encodeSyncBoolean
import com.nuvio.app.core.sync.encodeSyncString
import com.nuvio.app.core.storage.ProfileScopedKey
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

actual object ThemeSettingsStorage {
    private const val preferencesName = "nuvio_theme_settings"
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

    private var preferences: SharedPreferences? = null

    fun initialize(context: Context) {
        preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
        applySelectedAppLanguage(loadSelectedAppLanguage() ?: AppLanguage.DEVICE.code)
    }

    actual fun loadSelectedTheme(): String? =
        preferences?.getString(ProfileScopedKey.of(selectedThemeKey), null)

    actual fun saveSelectedTheme(themeName: String) {
        preferences
            ?.edit()
            ?.putString(ProfileScopedKey.of(selectedThemeKey), themeName)
            ?.apply()
    }

    actual fun loadCustomAccentHex(): String? =
        preferences?.getString(ProfileScopedKey.of(customAccentHexKey), null)

    actual fun saveCustomAccentHex(hex: String) {
        preferences
            ?.edit()
            ?.putString(ProfileScopedKey.of(customAccentHexKey), hex)
            ?.apply()
    }

    actual fun loadCustomThemeFirstColor(): String? =
        preferences?.getString(ProfileScopedKey.of(customThemeFirstColorKey), null)

    actual fun saveCustomThemeFirstColor(hex: String) {
        preferences
            ?.edit()
            ?.putString(ProfileScopedKey.of(customThemeFirstColorKey), hex)
            ?.apply()
    }

    actual fun loadCustomThemeSecondColor(): String? =
        preferences?.getString(ProfileScopedKey.of(customThemeSecondColorKey), null)

    actual fun saveCustomThemeSecondColor(hex: String) {
        preferences
            ?.edit()
            ?.putString(ProfileScopedKey.of(customThemeSecondColorKey), hex)
            ?.apply()
    }

    actual fun loadThemeAnimationStyle(): String? =
        preferences?.getString(ProfileScopedKey.of(themeAnimationStyleKey), null)

    actual fun saveThemeAnimationStyle(style: String) {
        preferences
            ?.edit()
            ?.putString(ProfileScopedKey.of(themeAnimationStyleKey), style)
            ?.apply()
    }

    actual fun loadSelectedAppIconId(): String? =
        preferences?.getString(ProfileScopedKey.of(selectedAppIconIdKey), null)

    actual fun saveSelectedAppIconId(iconId: String) {
        preferences
            ?.edit()
            ?.putString(ProfileScopedKey.of(selectedAppIconIdKey), iconId)
            ?.apply()
    }

    actual fun loadAmoledEnabled(): Boolean? =
        preferences?.let { prefs ->
            val key = ProfileScopedKey.of(amoledEnabledKey)
            if (prefs.contains(key)) prefs.getBoolean(key, false) else null
        }

    actual fun saveAmoledEnabled(enabled: Boolean) {
        preferences
            ?.edit()
            ?.putBoolean(ProfileScopedKey.of(amoledEnabledKey), enabled)
            ?.apply()
    }

    actual fun loadAmoledSurfacesEnabled(): Boolean? =
        preferences?.let { prefs ->
            val key = ProfileScopedKey.of(amoledSurfacesEnabledKey)
            if (prefs.contains(key)) prefs.getBoolean(key, false) else null
        }

    actual fun saveAmoledSurfacesEnabled(enabled: Boolean) {
        preferences
            ?.edit()
            ?.putBoolean(ProfileScopedKey.of(amoledSurfacesEnabledKey), enabled)
            ?.apply()
    }

    actual fun loadLiquidGlassNativeTabBarEnabled(): Boolean? =
        preferences?.let { prefs ->
            val key = ProfileScopedKey.of(liquidGlassNativeTabBarEnabledKey)
            if (prefs.contains(key)) prefs.getBoolean(key, false) else null
        }

    actual fun saveLiquidGlassNativeTabBarEnabled(enabled: Boolean) {
        preferences
            ?.edit()
            ?.putBoolean(ProfileScopedKey.of(liquidGlassNativeTabBarEnabledKey), enabled)
            ?.apply()
    }

    actual fun loadGlassNavBarEnabled(): Boolean? =
        preferences?.let { prefs ->
            val key = ProfileScopedKey.of(glassNavBarEnabledKey)
            if (prefs.contains(key)) prefs.getBoolean(key, false) else null
        }

    actual fun saveGlassNavBarEnabled(enabled: Boolean) {
        preferences
            ?.edit()
            ?.putBoolean(ProfileScopedKey.of(glassNavBarEnabledKey), enabled)
            ?.apply()
    }

    actual fun loadSelectedAppLanguage(): String? {
        val value = preferences?.getString(selectedAppLanguageKey, null)
        if (value != null) return value
        val legacy = preferences?.getString(ProfileScopedKey.of(selectedAppLanguageKey), null)
        if (legacy != null) saveSelectedAppLanguage(legacy)
        return legacy
    }

    actual fun saveSelectedAppLanguage(languageCode: String) {
        preferences
            ?.edit()
            ?.putString(selectedAppLanguageKey, languageCode)
            ?.apply()
    }

    actual fun applySelectedAppLanguage(languageCode: String) {
        if (languageCode.equals("device", ignoreCase = true)) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
        } else {
            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(languageCode),
            )
        }
    }

    actual fun loadDateFormatOption(): String? =
        preferences?.getString(dateFormatOptionKey, null)

    actual fun saveDateFormatOption(format: String) {
        preferences
            ?.edit()
            ?.putString(dateFormatOptionKey, format)
            ?.apply()
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
        preferences?.edit()?.apply {
            profileScopedSyncKeys.forEach { remove(ProfileScopedKey.of(it)) }
        }?.apply()

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
