package com.nuvio.app.features.settings

import com.nuvio.app.core.storage.DesktopStorage
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

internal actual object ThemeSettingsStorage {
    private val store = DesktopStorage.store("nuvio_theme_settings")
    private val json = Json { ignoreUnknownKeys = true }

    actual fun loadSelectedTheme(): String? =
        store.getString("selected_theme")

    actual fun saveSelectedTheme(themeName: String) {
        store.putString("selected_theme", themeName)
    }

    actual fun loadCustomAccentHex(): String? =
        store.getString("custom_accent_hex")

    actual fun saveCustomAccentHex(hex: String) {
        store.putString("custom_accent_hex", hex)
    }

    actual fun loadCustomThemeFirstColor(): String? =
        store.getString("custom_theme_first_color")

    actual fun saveCustomThemeFirstColor(hex: String) {
        store.putString("custom_theme_first_color", hex)
    }

    actual fun loadCustomThemeSecondColor(): String? =
        store.getString("custom_theme_second_color")

    actual fun saveCustomThemeSecondColor(hex: String) {
        store.putString("custom_theme_second_color", hex)
    }

    actual fun loadThemeAnimationStyle(): String? =
        store.getString("theme_animation_style")

    actual fun saveThemeAnimationStyle(style: String) {
        store.putString("theme_animation_style", style)
    }

    actual fun loadSelectedAppIconId(): String? =
        store.getString("selected_app_icon_id")

    actual fun saveSelectedAppIconId(iconId: String) {
        store.putString("selected_app_icon_id", iconId)
    }

    actual fun loadAmoledEnabled(): Boolean? =
        store.getBoolean("amoled_enabled")

    actual fun saveAmoledEnabled(enabled: Boolean) {
        store.putBoolean("amoled_enabled", enabled)
    }

    actual fun loadAmoledSurfacesEnabled(): Boolean? =
        store.getBoolean("amoled_surfaces_enabled")

    actual fun saveAmoledSurfacesEnabled(enabled: Boolean) {
        store.putBoolean("amoled_surfaces_enabled", enabled)
    }

    actual fun loadLiquidGlassNativeTabBarEnabled(): Boolean? =
        store.getBoolean("liquid_glass_native_tab_bar_enabled")

    actual fun saveLiquidGlassNativeTabBarEnabled(enabled: Boolean) {
        store.putBoolean("liquid_glass_native_tab_bar_enabled", enabled)
    }

    actual fun loadGlassNavBarEnabled(): Boolean? =
        store.getBoolean("glass_nav_bar_enabled")

    actual fun saveGlassNavBarEnabled(enabled: Boolean) {
        store.putBoolean("glass_nav_bar_enabled", enabled)
    }

    actual fun loadSelectedAppLanguage(): String? =
        store.getString("selected_app_language")

    actual fun saveSelectedAppLanguage(languageCode: String) {
        store.putString("selected_app_language", languageCode)
    }

    actual fun applySelectedAppLanguage(languageCode: String) {
        // No-op on desktop (language change requires app restart)
    }

    actual fun loadDateFormatOption(): String? =
        store.getString("date_format_option")

    actual fun saveDateFormatOption(format: String) {
        store.putString("date_format_option", format)
    }

    actual fun exportToSyncPayload(): JsonObject {
        val map = mutableMapOf<String, String>()
        loadSelectedTheme()?.let { map["selected_theme"] = it }
        loadAmoledEnabled()?.let { map["amoled_enabled"] = it.toString() }
        loadAmoledSurfacesEnabled()?.let { map["amoled_surfaces_enabled"] = it.toString() }
        loadLiquidGlassNativeTabBarEnabled()?.let { map["liquid_glass_native_tab_bar_enabled"] = it.toString() }
        loadGlassNavBarEnabled()?.let { map["glass_nav_bar_enabled"] = it.toString() }
        loadSelectedAppLanguage()?.let { map["selected_app_language"] = it }
        loadCustomAccentHex()?.let { map["custom_accent_hex"] = it }
        loadCustomThemeFirstColor()?.let { map["custom_theme_first_color"] = it }
        loadCustomThemeSecondColor()?.let { map["custom_theme_second_color"] = it }
        loadThemeAnimationStyle()?.let { map["theme_animation_style"] = it }
        return json.decodeFromString(json.encodeToString(map))
    }

    actual fun replaceFromSyncPayload(payload: JsonObject) {
        payload["selected_theme"]?.let { saveSelectedTheme(it.toString()) }
        payload["amoled_enabled"]?.let { saveAmoledEnabled(it.toString().toBooleanStrictOrNull() ?: return@let) }
        payload["amoled_surfaces_enabled"]?.let { saveAmoledSurfacesEnabled(it.toString().toBooleanStrictOrNull() ?: return@let) }
        payload["liquid_glass_native_tab_bar_enabled"]?.let { saveLiquidGlassNativeTabBarEnabled(it.toString().toBooleanStrictOrNull() ?: return@let) }
        payload["glass_nav_bar_enabled"]?.let { saveGlassNavBarEnabled(it.toString().toBooleanStrictOrNull() ?: return@let) }
        payload["selected_app_language"]?.let { saveSelectedAppLanguage(it.toString()) }
        payload["custom_accent_hex"]?.let { saveCustomAccentHex(it.toString()) }
        payload["custom_theme_first_color"]?.let { saveCustomThemeFirstColor(it.toString()) }
        payload["custom_theme_second_color"]?.let { saveCustomThemeSecondColor(it.toString()) }
        payload["theme_animation_style"]?.let { saveThemeAnimationStyle(it.toString()) }
    }
}
