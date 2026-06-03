package com.nuvio.app.features.streams

import com.nuvio.app.WebStorage
import kotlinx.serialization.json.*

internal actual object StreamBadgeSettingsStorage {
    private const val KEY = "nuvio_stream_badge_settings"

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

    actual fun loadStreamBadgeRules(): String? = prefs()["streamBadgeRules"]?.jsonPrimitive?.contentOrNull
    actual fun saveStreamBadgeRules(rules: String) { saveField("streamBadgeRules", rules) }
    actual fun loadShowFileSizeBadges(): Boolean? = prefs()["showFileSizeBadges"]?.jsonPrimitive?.booleanOrNull
    actual fun saveShowFileSizeBadges(enabled: Boolean) { saveField("showFileSizeBadges", enabled) }
    actual fun loadLegacyDebridStreamBadgeRules(): String? = prefs()["legacyDebridStreamBadgeRules"]?.jsonPrimitive?.contentOrNull
    actual fun clearLegacyDebridStreamBadgeRules() { val p = prefs().toMutableMap(); p.remove("legacyDebridStreamBadgeRules"); save(JsonObject(p)) }
    actual fun exportToSyncPayload(): JsonObject = prefs()
    actual fun replaceFromSyncPayload(payload: JsonObject) { WebStorage.setString(KEY, Json.encodeToString(JsonObject.serializer(), payload)) }
}
