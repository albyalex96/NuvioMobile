package com.nuvio.app.features.settings

import com.nuvio.app.WebStorage
import kotlinx.serialization.json.*

internal actual object ThemeSettingsStorage {
    private const val KEY = "nuvio_theme_settings"

    private fun prefs(): JsonObject {
        val raw = WebStorage.getString(KEY) ?: return JsonObject(emptyMap())
        return Json.parseToJsonElement(raw).jsonObject
    }
    private fun save(obj: JsonObject) { WebStorage.setString(KEY, Json.encodeToString(JsonObject.serializer(), obj)) }

    private inline fun <reified T> saveField(name: String, value: T) {
        val p = prefs().toMutableMap()
        p[name] = when (value) {
            is Boolean -> JsonPrimitive(value)
            is String -> JsonPrimitive(value)
            else -> JsonPrimitive(value.toString())
        }
        save(JsonObject(p))
    }

    actual fun loadSelectedTheme(): String? = prefs()["selectedTheme"]?.jsonPrimitive?.contentOrNull
    actual fun saveSelectedTheme(themeName: String) { saveField("selectedTheme", themeName) }
    actual fun loadAmoledEnabled(): Boolean? = prefs()["amoledEnabled"]?.jsonPrimitive?.booleanOrNull
    actual fun saveAmoledEnabled(enabled: Boolean) { saveField("amoledEnabled", enabled) }
    actual fun loadLiquidGlassNativeTabBarEnabled(): Boolean? = prefs()["liquidGlassNativeTabBarEnabled"]?.jsonPrimitive?.booleanOrNull
    actual fun saveLiquidGlassNativeTabBarEnabled(enabled: Boolean) { saveField("liquidGlassNativeTabBarEnabled", enabled) }
    actual fun loadSelectedAppLanguage(): String? = prefs()["selectedAppLanguage"]?.jsonPrimitive?.contentOrNull
    actual fun saveSelectedAppLanguage(languageCode: String) { saveField("selectedAppLanguage", languageCode) }
    actual fun applySelectedAppLanguage(languageCode: String) {}
    actual fun exportToSyncPayload(): JsonObject = prefs()
    actual fun replaceFromSyncPayload(payload: JsonObject) { WebStorage.setString(KEY, Json.encodeToString(JsonObject.serializer(), payload)) }
}
