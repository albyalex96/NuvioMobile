package com.nuvio.app.features.mdblist

import com.nuvio.app.core.storage.DesktopStorage
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

internal actual object MdbListSettingsStorage {
    private val store = DesktopStorage.store("nuvio_mdblist_settings")
    private val json = Json { ignoreUnknownKeys = true }

    actual fun loadEnabled(): Boolean? = store.getBoolean("mdblist_enabled")
    actual fun saveEnabled(enabled: Boolean) { store.putBoolean("mdblist_enabled", enabled) }
    actual fun loadApiKey(): String? = store.getString("mdblist_api_key")
    actual fun saveApiKey(apiKey: String) { store.putString("mdblist_api_key", apiKey) }
    actual fun loadUseImdb(): Boolean? = store.getBoolean("mdblist_use_imdb")
    actual fun saveUseImdb(enabled: Boolean) { store.putBoolean("mdblist_use_imdb", enabled) }
    actual fun loadUseTmdb(): Boolean? = store.getBoolean("mdblist_use_tmdb")
    actual fun saveUseTmdb(enabled: Boolean) { store.putBoolean("mdblist_use_tmdb", enabled) }
    actual fun loadUseTomatoes(): Boolean? = store.getBoolean("mdblist_use_tomatoes")
    actual fun saveUseTomatoes(enabled: Boolean) { store.putBoolean("mdblist_use_tomatoes", enabled) }
    actual fun loadUseMetacritic(): Boolean? = store.getBoolean("mdblist_use_metacritic")
    actual fun saveUseMetacritic(enabled: Boolean) { store.putBoolean("mdblist_use_metacritic", enabled) }
    actual fun loadUseTrakt(): Boolean? = store.getBoolean("mdblist_use_trakt")
    actual fun saveUseTrakt(enabled: Boolean) { store.putBoolean("mdblist_use_trakt", enabled) }
    actual fun loadUseLetterboxd(): Boolean? = store.getBoolean("mdblist_use_letterboxd")
    actual fun saveUseLetterboxd(enabled: Boolean) { store.putBoolean("mdblist_use_letterboxd", enabled) }
    actual fun loadUseAudience(): Boolean? = store.getBoolean("mdblist_use_audience")
    actual fun saveUseAudience(enabled: Boolean) { store.putBoolean("mdblist_use_audience", enabled) }

    actual fun exportToSyncPayload(): JsonObject {
        val map = mutableMapOf<String, String>()
        fun putOpt(key: String, value: Any?) { if (value != null) map[key] = value.toString() }
        putOpt("mdblist_enabled", loadEnabled())
        putOpt("mdblist_use_imdb", loadUseImdb())
        putOpt("mdblist_use_tmdb", loadUseTmdb())
        putOpt("mdblist_use_tomatoes", loadUseTomatoes())
        putOpt("mdblist_use_metacritic", loadUseMetacritic())
        putOpt("mdblist_use_trakt", loadUseTrakt())
        putOpt("mdblist_use_letterboxd", loadUseLetterboxd())
        putOpt("mdblist_use_audience", loadUseAudience())
        return json.decodeFromString(json.encodeToString(map))
    }

    actual fun replaceFromSyncPayload(payload: JsonObject) {
        fun bool(key: String) = payload[key]?.toString()?.toBooleanStrictOrNull()
        bool("mdblist_enabled")?.let { saveEnabled(it) }
        bool("mdblist_use_imdb")?.let { saveUseImdb(it) }
        bool("mdblist_use_tmdb")?.let { saveUseTmdb(it) }
        bool("mdblist_use_tomatoes")?.let { saveUseTomatoes(it) }
        bool("mdblist_use_metacritic")?.let { saveUseMetacritic(it) }
        bool("mdblist_use_trakt")?.let { saveUseTrakt(it) }
        bool("mdblist_use_letterboxd")?.let { saveUseLetterboxd(it) }
        bool("mdblist_use_audience")?.let { saveUseAudience(it) }
    }
}
