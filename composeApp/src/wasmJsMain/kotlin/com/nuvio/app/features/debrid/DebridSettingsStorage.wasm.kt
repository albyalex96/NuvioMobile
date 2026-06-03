package com.nuvio.app.features.debrid

import com.nuvio.app.WebStorage
import kotlinx.serialization.json.*

internal actual object DebridSettingsStorage {
    private const val KEY = "nuvio_debrid_settings"

    private fun prefs(): JsonObject {
        val raw = WebStorage.getString(KEY) ?: return JsonObject(emptyMap())
        return Json.parseToJsonElement(raw) as? JsonObject ?: JsonObject(emptyMap())
    }
    private fun save(obj: JsonObject) { WebStorage.setString(KEY, Json.encodeToString(JsonObject.serializer(), obj)) }

    private inline fun <reified T> saveField(name: String, value: T) {
        val p = prefs().toMutableMap()
        p[name] = when (value) {
            is Boolean -> JsonPrimitive(value)
            is Int -> JsonPrimitive(value)
            is String -> JsonPrimitive(value)
            null -> JsonPrimitive(null)
            else -> JsonPrimitive(value.toString())
        }
        save(JsonObject(p))
    }

    actual fun loadEnabled(): Boolean? = prefs()["enabled"]?.let { if (it is JsonPrimitive) it.booleanOrNull else null }
    actual fun saveEnabled(enabled: Boolean) { saveField("enabled", enabled) }
    actual fun loadCloudLibraryEnabled(): Boolean? = prefs()["cloudLibraryEnabled"]?.let { if (it is JsonPrimitive) it.booleanOrNull else null }
    actual fun saveCloudLibraryEnabled(enabled: Boolean) { saveField("cloudLibraryEnabled", enabled) }
    actual fun loadPreferredResolverProviderId(): String? = prefs()["preferredResolverProviderId"]?.let { if (it is JsonPrimitive) it.contentOrNull else null }
    actual fun savePreferredResolverProviderId(providerId: String) { saveField("preferredResolverProviderId", providerId) }
    actual fun loadProviderApiKey(providerId: String): String? {
        val p = prefs()
        val obj = p["providerApiKeys"]?.let { if (it is JsonObject) it else null } ?: return null
        return obj[providerId]?.let { if (it is JsonPrimitive) it.contentOrNull else null }
    }
    actual fun saveProviderApiKey(providerId: String, apiKey: String) {
        val p = prefs().toMutableMap()
        val keys = (p["providerApiKeys"] as? JsonObject)?.toMutableMap() ?: mutableMapOf()
        keys[providerId] = JsonPrimitive(apiKey)
        p["providerApiKeys"] = JsonObject(keys)
        save(JsonObject(p))
    }
    actual fun loadTorboxApiKey(): String? = prefs()["torboxApiKey"]?.let { if (it is JsonPrimitive) it.contentOrNull else null }
    actual fun saveTorboxApiKey(apiKey: String) { saveField("torboxApiKey", apiKey) }
    actual fun loadRealDebridApiKey(): String? = prefs()["realDebridApiKey"]?.let { if (it is JsonPrimitive) it.contentOrNull else null }
    actual fun saveRealDebridApiKey(apiKey: String) { saveField("realDebridApiKey", apiKey) }
    actual fun loadInstantPlaybackPreparationLimit(): Int? = prefs()["instantPlaybackPreparationLimit"]?.let { if (it is JsonPrimitive) it.intOrNull else null }
    actual fun saveInstantPlaybackPreparationLimit(limit: Int) { saveField("instantPlaybackPreparationLimit", limit) }
    actual fun loadStreamMaxResults(): Int? = prefs()["streamMaxResults"]?.let { if (it is JsonPrimitive) it.intOrNull else null }
    actual fun saveStreamMaxResults(maxResults: Int) { saveField("streamMaxResults", maxResults) }
    actual fun loadStreamSortMode(): String? = prefs()["streamSortMode"]?.let { if (it is JsonPrimitive) it.contentOrNull else null }
    actual fun saveStreamSortMode(mode: String) { saveField("streamSortMode", mode) }
    actual fun loadStreamMinimumQuality(): String? = prefs()["streamMinimumQuality"]?.let { if (it is JsonPrimitive) it.contentOrNull else null }
    actual fun saveStreamMinimumQuality(quality: String) { saveField("streamMinimumQuality", quality) }
    actual fun loadStreamDolbyVisionFilter(): String? = prefs()["streamDolbyVisionFilter"]?.let { if (it is JsonPrimitive) it.contentOrNull else null }
    actual fun saveStreamDolbyVisionFilter(filter: String) { saveField("streamDolbyVisionFilter", filter) }
    actual fun loadStreamHdrFilter(): String? = prefs()["streamHdrFilter"]?.let { if (it is JsonPrimitive) it.contentOrNull else null }
    actual fun saveStreamHdrFilter(filter: String) { saveField("streamHdrFilter", filter) }
    actual fun loadStreamCodecFilter(): String? = prefs()["streamCodecFilter"]?.let { if (it is JsonPrimitive) it.contentOrNull else null }
    actual fun saveStreamCodecFilter(filter: String) { saveField("streamCodecFilter", filter) }
    actual fun loadStreamPreferences(): String? = prefs()["streamPreferences"]?.let { if (it is JsonPrimitive) it.contentOrNull else null }
    actual fun saveStreamPreferences(preferences: String) { saveField("streamPreferences", preferences) }
    actual fun loadStreamNameTemplate(): String? = prefs()["streamNameTemplate"]?.let { if (it is JsonPrimitive) it.contentOrNull else null }
    actual fun saveStreamNameTemplate(template: String) { saveField("streamNameTemplate", template) }
    actual fun loadStreamDescriptionTemplate(): String? = prefs()["streamDescriptionTemplate"]?.let { if (it is JsonPrimitive) it.contentOrNull else null }
    actual fun saveStreamDescriptionTemplate(template: String) { saveField("streamDescriptionTemplate", template) }
    actual fun exportToSyncPayload(): JsonObject = prefs()
    actual fun replaceFromSyncPayload(payload: JsonObject) { WebStorage.setString(KEY, Json.encodeToString(JsonObject.serializer(), payload)) }
}
