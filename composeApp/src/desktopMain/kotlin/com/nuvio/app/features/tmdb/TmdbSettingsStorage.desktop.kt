package com.nuvio.app.features.tmdb

import com.nuvio.app.core.storage.DesktopStorage
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal actual object TmdbSettingsStorage {
    private val store = DesktopStorage.store("nuvio_tmdb_settings")

    private fun bool(key: String): Boolean? =
        if (store.contains(key)) store.getBoolean(key) else null

    actual fun loadEnabled(): Boolean? = bool("tmdb_enabled")

    actual fun saveEnabled(enabled: Boolean) { store.putBoolean("tmdb_enabled", enabled) }

    actual fun loadApiKey(): String? = store.getString("tmdb_api_key")

    actual fun saveApiKey(apiKey: String) { store.putString("tmdb_api_key", apiKey) }

    actual fun loadLanguage(): String? = store.getString("tmdb_language")

    actual fun saveLanguage(language: String) { store.putString("tmdb_language", language) }

    actual fun loadUseTrailers(): Boolean? = bool("tmdb_use_trailers")

    actual fun saveUseTrailers(enabled: Boolean) { store.putBoolean("tmdb_use_trailers", enabled) }

    actual fun loadUseArtwork(): Boolean? = bool("tmdb_use_artwork")

    actual fun saveUseArtwork(enabled: Boolean) { store.putBoolean("tmdb_use_artwork", enabled) }

    actual fun loadUseBasicInfo(): Boolean? = bool("tmdb_use_basic_info")

    actual fun saveUseBasicInfo(enabled: Boolean) { store.putBoolean("tmdb_use_basic_info", enabled) }

    actual fun loadUseDetails(): Boolean? = bool("tmdb_use_details")

    actual fun saveUseDetails(enabled: Boolean) { store.putBoolean("tmdb_use_details", enabled) }

    actual fun loadUseCredits(): Boolean? = bool("tmdb_use_credits")

    actual fun saveUseCredits(enabled: Boolean) { store.putBoolean("tmdb_use_credits", enabled) }

    actual fun loadUseProductions(): Boolean? = bool("tmdb_use_productions")

    actual fun saveUseProductions(enabled: Boolean) { store.putBoolean("tmdb_use_productions", enabled) }

    actual fun loadUseNetworks(): Boolean? = bool("tmdb_use_networks")

    actual fun saveUseNetworks(enabled: Boolean) { store.putBoolean("tmdb_use_networks", enabled) }

    actual fun loadUseEpisodes(): Boolean? = bool("tmdb_use_episodes")

    actual fun saveUseEpisodes(enabled: Boolean) { store.putBoolean("tmdb_use_episodes", enabled) }

    actual fun loadUseSeasonPosters(): Boolean? = bool("tmdb_use_season_posters")

    actual fun saveUseSeasonPosters(enabled: Boolean) { store.putBoolean("tmdb_use_season_posters", enabled) }

    actual fun loadUseMoreLikeThis(): Boolean? = bool("tmdb_use_more_like_this")

    actual fun saveUseMoreLikeThis(enabled: Boolean) { store.putBoolean("tmdb_use_more_like_this", enabled) }

    actual fun loadUseCollections(): Boolean? = bool("tmdb_use_collections")

    actual fun saveUseCollections(enabled: Boolean) { store.putBoolean("tmdb_use_collections", enabled) }

    actual fun exportToSyncPayload(): JsonObject = buildJsonObject {
        loadEnabled()?.let { put("tmdb_enabled", it) }
        loadApiKey()?.let { put("tmdb_api_key", it) }
        loadLanguage()?.let { put("tmdb_language", it) }
        loadUseTrailers()?.let { put("tmdb_use_trailers", it) }
        loadUseArtwork()?.let { put("tmdb_use_artwork", it) }
        loadUseBasicInfo()?.let { put("tmdb_use_basic_info", it) }
        loadUseDetails()?.let { put("tmdb_use_details", it) }
        loadUseCredits()?.let { put("tmdb_use_credits", it) }
        loadUseProductions()?.let { put("tmdb_use_productions", it) }
        loadUseNetworks()?.let { put("tmdb_use_networks", it) }
        loadUseEpisodes()?.let { put("tmdb_use_episodes", it) }
        loadUseSeasonPosters()?.let { put("tmdb_use_season_posters", it) }
        loadUseMoreLikeThis()?.let { put("tmdb_use_more_like_this", it) }
        loadUseCollections()?.let { put("tmdb_use_collections", it) }
    }

    private fun extractBoolean(element: JsonElement?): Boolean? =
        (element as? JsonPrimitive)?.content?.toBooleanStrictOrNull()

    private fun extractString(element: JsonElement?): String? =
        (element as? JsonPrimitive)?.content

    actual fun replaceFromSyncPayload(payload: JsonObject) {
        extractBoolean(payload["tmdb_enabled"])?.let(::saveEnabled)
        extractString(payload["tmdb_api_key"])?.let(::saveApiKey)
        extractString(payload["tmdb_language"])?.let(::saveLanguage)
        extractBoolean(payload["tmdb_use_trailers"])?.let(::saveUseTrailers)
        extractBoolean(payload["tmdb_use_artwork"])?.let(::saveUseArtwork)
        extractBoolean(payload["tmdb_use_basic_info"])?.let(::saveUseBasicInfo)
        extractBoolean(payload["tmdb_use_details"])?.let(::saveUseDetails)
        extractBoolean(payload["tmdb_use_credits"])?.let(::saveUseCredits)
        extractBoolean(payload["tmdb_use_productions"])?.let(::saveUseProductions)
        extractBoolean(payload["tmdb_use_networks"])?.let(::saveUseNetworks)
        extractBoolean(payload["tmdb_use_episodes"])?.let(::saveUseEpisodes)
        extractBoolean(payload["tmdb_use_season_posters"])?.let(::saveUseSeasonPosters)
        extractBoolean(payload["tmdb_use_more_like_this"])?.let(::saveUseMoreLikeThis)
        extractBoolean(payload["tmdb_use_collections"])?.let(::saveUseCollections)
    }
}
