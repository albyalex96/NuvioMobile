package com.nuvio.app.features.tmdb

import com.nuvio.app.WebStorage
import kotlinx.serialization.json.*

internal actual object TmdbSettingsStorage {
    private const val KEY = "nuvio_tmdb_settings"

    private fun prefs(): JsonObject {
        val raw = WebStorage.getString(KEY) ?: return JsonObject(emptyMap())
        return Json.parseToJsonElement(raw).jsonObject
    }
    private fun save(obj: JsonObject) { WebStorage.setString(KEY, Json.encodeToString(JsonObject.serializer(), obj)) }

    private fun JsonObject.bool(key: String): Boolean? = this[key]?.jsonPrimitive?.booleanOrNull
    private fun JsonObject.str(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull

    actual fun loadEnabled(): Boolean? = prefs().bool("enabled")
    actual fun saveEnabled(enabled: Boolean) { val p = prefs().toMutableMap(); p["enabled"] = JsonPrimitive(enabled); save(JsonObject(p)) }
    actual fun loadApiKey(): String? = prefs().str("apiKey")
    actual fun saveApiKey(apiKey: String) { val p = prefs().toMutableMap(); p["apiKey"] = JsonPrimitive(apiKey); save(JsonObject(p)) }
    actual fun loadLanguage(): String? = prefs().str("language")
    actual fun saveLanguage(language: String) { val p = prefs().toMutableMap(); p["language"] = JsonPrimitive(language); save(JsonObject(p)) }
    actual fun loadUseTrailers(): Boolean? = prefs().bool("useTrailers")
    actual fun saveUseTrailers(enabled: Boolean) { val p = prefs().toMutableMap(); p["useTrailers"] = JsonPrimitive(enabled); save(JsonObject(p)) }
    actual fun loadUseArtwork(): Boolean? = prefs().bool("useArtwork")
    actual fun saveUseArtwork(enabled: Boolean) { val p = prefs().toMutableMap(); p["useArtwork"] = JsonPrimitive(enabled); save(JsonObject(p)) }
    actual fun loadUseBasicInfo(): Boolean? = prefs().bool("useBasicInfo")
    actual fun saveUseBasicInfo(enabled: Boolean) { val p = prefs().toMutableMap(); p["useBasicInfo"] = JsonPrimitive(enabled); save(JsonObject(p)) }
    actual fun loadUseDetails(): Boolean? = prefs().bool("useDetails")
    actual fun saveUseDetails(enabled: Boolean) { val p = prefs().toMutableMap(); p["useDetails"] = JsonPrimitive(enabled); save(JsonObject(p)) }
    actual fun loadUseCredits(): Boolean? = prefs().bool("useCredits")
    actual fun saveUseCredits(enabled: Boolean) { val p = prefs().toMutableMap(); p["useCredits"] = JsonPrimitive(enabled); save(JsonObject(p)) }
    actual fun loadUseProductions(): Boolean? = prefs().bool("useProductions")
    actual fun saveUseProductions(enabled: Boolean) { val p = prefs().toMutableMap(); p["useProductions"] = JsonPrimitive(enabled); save(JsonObject(p)) }
    actual fun loadUseNetworks(): Boolean? = prefs().bool("useNetworks")
    actual fun saveUseNetworks(enabled: Boolean) { val p = prefs().toMutableMap(); p["useNetworks"] = JsonPrimitive(enabled); save(JsonObject(p)) }
    actual fun loadUseEpisodes(): Boolean? = prefs().bool("useEpisodes")
    actual fun saveUseEpisodes(enabled: Boolean) { val p = prefs().toMutableMap(); p["useEpisodes"] = JsonPrimitive(enabled); save(JsonObject(p)) }
    actual fun loadUseSeasonPosters(): Boolean? = prefs().bool("useSeasonPosters")
    actual fun saveUseSeasonPosters(enabled: Boolean) { val p = prefs().toMutableMap(); p["useSeasonPosters"] = JsonPrimitive(enabled); save(JsonObject(p)) }
    actual fun loadUseMoreLikeThis(): Boolean? = prefs().bool("useMoreLikeThis")
    actual fun saveUseMoreLikeThis(enabled: Boolean) { val p = prefs().toMutableMap(); p["useMoreLikeThis"] = JsonPrimitive(enabled); save(JsonObject(p)) }
    actual fun loadUseCollections(): Boolean? = prefs().bool("useCollections")
    actual fun saveUseCollections(enabled: Boolean) { val p = prefs().toMutableMap(); p["useCollections"] = JsonPrimitive(enabled); save(JsonObject(p)) }
    actual fun exportToSyncPayload(): JsonObject = prefs()
    actual fun replaceFromSyncPayload(payload: JsonObject) { WebStorage.setString(KEY, Json.encodeToString(JsonObject.serializer(), payload)) }
}
