package com.nuvio.app.features.settings

import com.nuvio.app.core.platform.WebKeyValueStorage
import com.nuvio.app.core.storage.ProfileScopedKey
import com.nuvio.app.core.sync.decodeSyncBoolean
import com.nuvio.app.core.sync.decodeSyncString
import com.nuvio.app.core.sync.encodeSyncBoolean
import com.nuvio.app.core.sync.encodeSyncString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal actual object ThemeSettingsStorage {
    private const val namespace = "nuvio_theme_settings"
    private const val selectedThemeKey = "selected_theme"
    private const val amoledEnabledKey = "amoled_enabled"
    private const val amoledSurfacesEnabledKey = "amoled_surfaces_enabled"
    private const val liquidGlassNativeTabBarEnabledKey = "liquid_glass_native_tab_bar_enabled"
    private const val glassNavBarEnabledKey = "glass_nav_bar_enabled"
    private const val selectedAppLanguageKey = "selected_app_language"
    private val profileScopedSyncKeys = listOf(
        selectedThemeKey, amoledEnabledKey, amoledSurfacesEnabledKey,
        liquidGlassNativeTabBarEnabledKey, glassNavBarEnabledKey, selectedAppLanguageKey,
    )

    actual fun loadSelectedTheme(): String? = loadString(selectedThemeKey)
    actual fun saveSelectedTheme(themeName: String) = saveString(selectedThemeKey, themeName)
    actual fun loadAmoledEnabled(): Boolean? = loadBoolean(amoledEnabledKey)
    actual fun saveAmoledEnabled(enabled: Boolean) = saveBoolean(amoledEnabledKey, enabled)
    actual fun loadAmoledSurfacesEnabled(): Boolean? = loadBoolean(amoledSurfacesEnabledKey)
    actual fun saveAmoledSurfacesEnabled(enabled: Boolean) = saveBoolean(amoledSurfacesEnabledKey, enabled)
    actual fun loadLiquidGlassNativeTabBarEnabled(): Boolean? = loadBoolean(liquidGlassNativeTabBarEnabledKey)
    actual fun saveLiquidGlassNativeTabBarEnabled(enabled: Boolean) = saveBoolean(liquidGlassNativeTabBarEnabledKey, enabled)
    actual fun loadGlassNavBarEnabled(): Boolean? = loadBoolean(glassNavBarEnabledKey)
    actual fun saveGlassNavBarEnabled(enabled: Boolean) = saveBoolean(glassNavBarEnabledKey, enabled)
    actual fun loadSelectedAppLanguage(): String? = loadString(selectedAppLanguageKey)
    actual fun saveSelectedAppLanguage(languageCode: String) = saveString(selectedAppLanguageKey, languageCode)
    actual fun applySelectedAppLanguage(languageCode: String) = Unit

    actual fun exportToSyncPayload(): JsonObject = buildJsonObject {
        loadSelectedTheme()?.let { put(selectedThemeKey, encodeSyncString(it)) }
        loadAmoledEnabled()?.let { put(amoledEnabledKey, encodeSyncBoolean(it)) }
        loadAmoledSurfacesEnabled()?.let { put(amoledSurfacesEnabledKey, encodeSyncBoolean(it)) }
        loadLiquidGlassNativeTabBarEnabled()?.let { put(liquidGlassNativeTabBarEnabledKey, encodeSyncBoolean(it)) }
        loadGlassNavBarEnabled()?.let { put(glassNavBarEnabledKey, encodeSyncBoolean(it)) }
        loadSelectedAppLanguage()?.let { put(selectedAppLanguageKey, encodeSyncString(it)) }
    }

    actual fun replaceFromSyncPayload(payload: JsonObject) {
        profileScopedSyncKeys.forEach { WebKeyValueStorage.remove(namespace, ProfileScopedKey.of(it)) }
        payload.decodeSyncString(selectedThemeKey)?.let(::saveSelectedTheme)
        payload.decodeSyncBoolean(amoledEnabledKey)?.let(::saveAmoledEnabled)
        payload.decodeSyncBoolean(amoledSurfacesEnabledKey)?.let(::saveAmoledSurfacesEnabled)
        payload.decodeSyncBoolean(liquidGlassNativeTabBarEnabledKey)?.let(::saveLiquidGlassNativeTabBarEnabled)
        payload.decodeSyncBoolean(glassNavBarEnabledKey)?.let(::saveGlassNavBarEnabled)
        payload.decodeSyncString(selectedAppLanguageKey)?.let(::saveSelectedAppLanguage)
    }

    private fun loadBoolean(key: String): Boolean? = WebKeyValueStorage.getBoolean(namespace, ProfileScopedKey.of(key))
    private fun saveBoolean(key: String, value: Boolean) = WebKeyValueStorage.setBoolean(namespace, ProfileScopedKey.of(key), value)
    private fun loadString(key: String): String? = WebKeyValueStorage.getString(namespace, ProfileScopedKey.of(key))
    private fun saveString(key: String, value: String) = WebKeyValueStorage.setString(namespace, ProfileScopedKey.of(key), value)
}
