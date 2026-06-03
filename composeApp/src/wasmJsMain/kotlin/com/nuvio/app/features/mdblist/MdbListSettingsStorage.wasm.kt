package com.nuvio.app.features.mdblist

import com.nuvio.app.WebStorage
import kotlinx.serialization.json.*

internal actual object MdbListSettingsStorage {
    private const val KEY = "nuvio_mdblist_settings"

    private fun prefs(): JsonObject {
        val raw = WebStorage.getString(KEY) ?: return JsonObject(emptyMap())
        return Json.parseToJsonElement(raw) as? JsonObject ?: JsonObject(emptyMap())
    }
    private fun save(obj: JsonObject) { WebStorage.setString(KEY, Json.encodeToString(JsonObject.serializer(), obj)) }

    private fun JsonPrimitive?.bool(): Boolean? = this?.booleanOrNull
    private fun JsonPrimitive?.str(): String? = this?.contentOrNull

    private inline fun <reified T> saveField(name: String, value: T) {
        val p = prefs().toMutableMap()
        p[name] = when (value) {
            is Boolean -> JsonPrimitive(value)
            is String -> JsonPrimitive(value)
            null -> JsonPrimitive(null)
            else -> JsonPrimitive(value.toString())
        }
        save(JsonObject(p))
    }

    actual fun loadEnabled(): Boolean? = (prefs()["enabled"] as? JsonPrimitive)?.bool()
    actual fun saveEnabled(enabled: Boolean) { saveField("enabled", enabled) }
    actual fun loadApiKey(): String? = (prefs()["apiKey"] as? JsonPrimitive)?.str()
    actual fun saveApiKey(apiKey: String) { saveField("apiKey", apiKey) }
    actual fun loadUseImdb(): Boolean? = (prefs()["useImdb"] as? JsonPrimitive)?.bool()
    actual fun saveUseImdb(enabled: Boolean) { saveField("useImdb", enabled) }
    actual fun loadUseTmdb(): Boolean? = (prefs()["useTmdb"] as? JsonPrimitive)?.bool()
    actual fun saveUseTmdb(enabled: Boolean) { saveField("useTmdb", enabled) }
    actual fun loadUseTomatoes(): Boolean? = (prefs()["useTomatoes"] as? JsonPrimitive)?.bool()
    actual fun saveUseTomatoes(enabled: Boolean) { saveField("useTomatoes", enabled) }
    actual fun loadUseMetacritic(): Boolean? = (prefs()["useMetacritic"] as? JsonPrimitive)?.bool()
    actual fun saveUseMetacritic(enabled: Boolean) { saveField("useMetacritic", enabled) }
    actual fun loadUseTrakt(): Boolean? = (prefs()["useTrakt"] as? JsonPrimitive)?.bool()
    actual fun saveUseTrakt(enabled: Boolean) { saveField("useTrakt", enabled) }
    actual fun loadUseLetterboxd(): Boolean? = (prefs()["useLetterboxd"] as? JsonPrimitive)?.bool()
    actual fun saveUseLetterboxd(enabled: Boolean) { saveField("useLetterboxd", enabled) }
    actual fun loadUseAudience(): Boolean? = (prefs()["useAudience"] as? JsonPrimitive)?.bool()
    actual fun saveUseAudience(enabled: Boolean) { saveField("useAudience", enabled) }
    actual fun exportToSyncPayload(): JsonObject = prefs()
    actual fun replaceFromSyncPayload(payload: JsonObject) { WebStorage.setString(KEY, Json.encodeToString(JsonObject.serializer(), payload)) }
}
