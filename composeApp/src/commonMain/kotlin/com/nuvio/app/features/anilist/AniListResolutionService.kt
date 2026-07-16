package com.nuvio.app.features.anilist

import co.touchlab.kermit.Logger
import com.nuvio.app.features.addons.httpGetText
import com.nuvio.app.features.addons.httpPostJsonWithHeaders
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlin.math.abs

data class ResolvedAniListMatch(
    val anilistId: Int,
    val anilistEpisode: Int,
    val title: String
)

object AniListResolutionService {
    private val log = Logger.withTag("AniListResolution")
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private const val ANILIST_API = "https://graphql.anilist.co"
    private const val ANIZIP_API = "https://api.ani.zip/mappings"
    private const val ARM_API = "https://arm.haglund.dev/api/v2"

    suspend fun resolveAniListId(
        imdbId: String,
        seasonNumber: Int,
        episodeNumber: Int,
        episodeReleaseDate: String?, // YYYY-MM-DD
        showTitle: String,
        videoId: String? = null,
        isMovie: Boolean = false,
        tmdbId: String? = null
    ): ResolvedAniListMatch? {
        var directAniListId: Int? = null
        var directEpisode: Int? = null

        // Try parsing from videoId first (e.g. "anilist:12345:6")
        if (videoId != null && videoId.startsWith("anilist:", ignoreCase = true)) {
            val parts = videoId.split(":")
            directAniListId = parts.getOrNull(1)?.toIntOrNull()
            directEpisode = parts.getOrNull(2)?.toIntOrNull()
        }

        // Try parsing from imdbId/parentMetaId next (e.g. "anilist:12345" or "anilist:12345:6")
        if (directAniListId == null && imdbId.startsWith("anilist:", ignoreCase = true)) {
            val parts = imdbId.split(":")
            directAniListId = parts.getOrNull(1)?.toIntOrNull()
            directEpisode = parts.getOrNull(2)?.toIntOrNull()
        }

        // Try raw integer from imdbId
        if (directAniListId == null && imdbId.toIntOrNull() != null) {
            directAniListId = imdbId.toInt()
        }

        if (directAniListId != null) {
            val matchedEp = directEpisode ?: episodeNumber
            log.i { "Direct AniList ID matched from metadata/addon: $directAniListId, Episode: $matchedEp" }
            return ResolvedAniListMatch(
                anilistId = directAniListId,
                anilistEpisode = matchedEp,
                title = showTitle
            )
        }

        val resolvedTmdbId = tmdbId ?: when {
            imdbId.startsWith("tmdb:", ignoreCase = true) -> imdbId.removePrefix("tmdb:")
            videoId != null && videoId.startsWith("tmdb:", ignoreCase = true) -> {
                videoId.split(":").getOrNull(1)
            }
            else -> null
        }

        if (isMovie) {
            log.d { "Movie mode detected. Querying ARM API directly for IMDb ID: $imdbId, TMDB: $resolvedTmdbId" }
            val armCandidates = fetchCandidatesFromArm(imdbId, resolvedTmdbId)
            val resolvedId = armCandidates.firstOrNull()
            if (resolvedId != null) {
                log.i { "SUCCESS (Movie): Resolved directly to AniList ID: $resolvedId via ARM API" }
                return ResolvedAniListMatch(
                    anilistId = resolvedId,
                    anilistEpisode = 1,
                    title = showTitle
                )
            }
            log.w { "Failed to resolve AniList ID for Movie: $showTitle (IMDb: $imdbId) via ARM API" }
            return null
        }

        if (episodeReleaseDate.isNullOrBlank()) return null
        
        log.d { "Starting AniList resolution for $showTitle (IMDb: $imdbId, S${seasonNumber}E${episodeNumber}, Date: $episodeReleaseDate)" }

        val dayIndex = calculateDayIndex(imdbId, seasonNumber, episodeNumber, episodeReleaseDate)

        // 1. Fetch direct candidate AniList IDs from ARM mappings
        val candidates = fetchCandidatesFromArm(imdbId, resolvedTmdbId).toMutableList()

        // 2. If ARM fails or returns nothing, fetch candidates via AniList Title Search
        if (candidates.isEmpty()) {
            log.d { "ARM candidate lookup returned empty. Falling back to AniList search." }
            candidates.addAll(fetchCandidatesFromAniList(showTitle))
        }

        log.d { "Evaluating candidates: $candidates" }

        // 3. Match dates using ani.zip
        for (candId in candidates) {
            val zipData = fetchAniZipMappings(candId) ?: continue
            val episodes = zipData.episodes ?: continue
            
            var matchedEpisodeEntry = episodes.entries.find { (_, ep) ->
                // Try matching by season and episode number first (TVDB alignment)
                ep.seasonNumber == seasonNumber && ep.episodeNumber == episodeNumber
            }

            if (matchedEpisodeEntry == null) {
                // Fallback: match by dates within 2 days tolerance
                val closeEpisodes = episodes.entries.filter { (_, ep) ->
                    val airDate = ep.airDate?.split("T")?.get(0)
                    val airDateUtc = ep.airDateUtc?.split("T")?.get(0)
                    areDatesClose(episodeReleaseDate, airDate, 2) || areDatesClose(episodeReleaseDate, airDateUtc, 2)
                }.sortedBy { it.key.toIntOrNull() ?: 0 }

                if (closeEpisodes.isNotEmpty()) {
                    val targetIdx = (dayIndex - 1).coerceIn(0, closeEpisodes.lastIndex)
                    matchedEpisodeEntry = closeEpisodes[targetIdx]
                }
            }

            if (matchedEpisodeEntry != null) {
                val aniEp = matchedEpisodeEntry.key.toIntOrNull() ?: 1
                val matchedTitle = zipData.titles?.get("en") ?: zipData.titles?.get("ja") ?: showTitle
                log.i { "SUCCESS: Matched $showTitle to AniList ID: $candId, Episode: $aniEp" }
                return ResolvedAniListMatch(
                    anilistId = candId,
                    anilistEpisode = aniEp,
                    title = matchedTitle
                )
            }
        }

        log.w { "No matching AniList ID could be resolved for $showTitle" }
        return null
    }

    private suspend fun fetchCandidatesFromArm(imdbId: String, tmdbId: String? = null): List<Int> {
        val candidates = mutableListOf<Int>()
        
        // 1. Query by IMDb ID if it is a valid IMDb ID (starts with "tt")
        if (imdbId.startsWith("tt", ignoreCase = true)) {
            val url = "$ARM_API/imdb?id=$imdbId&include=anilist"
            try {
                val text = httpGetText(url)
                val jsonElement = json.parseToJsonElement(text)
                if (jsonElement is JsonArray) {
                    val list = jsonElement.mapNotNull { element ->
                        val obj = element as? JsonObject
                        obj?.get("anilist")?.jsonPrimitive?.intOrNull
                    }
                    candidates.addAll(list)
                }
            } catch (e: Exception) {
                log.w(e) { "ARM imdb lookup failed" }
            }
        }
        
        // 2. Query by TMDB ID as fallback/additional if candidates are empty and TMDB ID is available
        if (candidates.isEmpty() && !tmdbId.isNullOrBlank()) {
            val url = "$ARM_API/themoviedb?id=$tmdbId&include=anilist"
            try {
                val text = httpGetText(url)
                val jsonElement = json.parseToJsonElement(text)
                if (jsonElement is JsonArray) {
                    val list = jsonElement.mapNotNull { element ->
                        val obj = element as? JsonObject
                        obj?.get("anilist")?.jsonPrimitive?.intOrNull
                    }
                    candidates.addAll(list)
                }
            } catch (e: Exception) {
                log.w(e) { "ARM themoviedb lookup failed for $tmdbId" }
            }
        }
        
        return candidates
    }

    private suspend fun fetchCandidatesFromAniList(title: String): List<Int> {
        val graphQuery = """
            query (${'$'}search: String) {
              Page (page: 1, perPage: 25) {
                media (search: ${'$'}search, type: ANIME) {
                  id
                }
              }
            }
        """.trimIndent()

        val request = AniListGraphQLRequest(
            query = graphQuery,
            variables = kotlinx.serialization.json.buildJsonObject {
                put("search", kotlinx.serialization.json.JsonPrimitive(title))
            }
        )
        
        return try {
            val body = json.encodeToString(AniListGraphQLRequest.serializer(), request)
            val text = httpPostJsonWithHeaders(ANILIST_API, body, mapOf("Content-Type" to "application/json"))
            val response = json.decodeFromString<AniListResponse>(text)
            response.data?.Page?.media?.map { it.id } ?: emptyList()
        } catch (e: Exception) {
            log.w(e) { "AniList query failed" }
            emptyList()
        }
    }

    private suspend fun fetchAniZipMappings(anilistId: Int): AniZipResponse? {
        val url = "$ANIZIP_API?anilist_id=$anilistId"
        return try {
            val text = httpGetText(url)
            json.decodeFromString<AniZipResponse>(text)
        } catch (e: Exception) {
            log.w(e) { "ani.zip fetch failed for $anilistId" }
            null
        }
    }

    internal fun areDatesClose(dateA: String?, dateB: String?, toleranceDays: Int): Boolean {
        if (dateA.isNullOrBlank() || dateB.isNullOrBlank()) return false
        return try {
            val d1 = parseDateString(dateA)
            val d2 = parseDateString(dateB)
            val diff = abs(d1 - d2)
            diff <= toleranceDays * 24 * 60 * 60 * 1000L
        } catch (_: Exception) {
            false
        }
    }

    internal fun parseDateString(dateStr: String): Long {
        val parts = dateStr.trim().split("-")
        if (parts.size < 3) return 0L
        val year = parts[0].toIntOrNull() ?: 1970
        val month = parts[1].toIntOrNull() ?: 1
        val dayPart = parts[2].takeWhile { it.isDigit() }
        val day = dayPart.toIntOrNull() ?: 1
        
        var days = (year - 1970) * 365 + (year - 1969) / 4
        val monthDays = intArrayOf(0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) {
            monthDays[2] = 29
        }
        for (i in 1 until month) {
            days += monthDays[i]
        }
        days += (day - 1)
        return days * 24 * 60 * 60 * 1000L
    }

    private suspend fun calculateDayIndex(
        imdbId: String,
        seasonNumber: Int,
        episodeNumber: Int,
        episodeReleaseDate: String?
    ): Int {
        if (episodeReleaseDate.isNullOrBlank()) return 1
        val cleanTargetDate = episodeReleaseDate.split("T")[0]
        
        try {
            val url = "https://v3-cinemeta.strem.io/meta/series/$imdbId.json"
            val text = httpGetText(url)
            val jsonElement = json.parseToJsonElement(text) as? JsonObject
            val meta = jsonElement?.get("meta") as? JsonObject
            val videos = meta?.get("videos") as? JsonArray ?: return 1
            
            // Filter all videos that belong to the same season and share the same release date
            val sameDayVideos = videos.mapNotNull { element ->
                val obj = element as? JsonObject ?: return@mapNotNull null
                val epSeason = obj["season"]?.jsonPrimitive?.intOrNull ?: return@mapNotNull null
                val releasedRaw = obj["released"]?.jsonPrimitive?.content ?: return@mapNotNull null
                val releasedClean = releasedRaw.split("T")[0]
                
                if (epSeason == seasonNumber && releasedClean == cleanTargetDate) {
                    obj["episode"]?.jsonPrimitive?.intOrNull
                } else {
                    null
                }
            }.sorted()
            
            val targetIndex = sameDayVideos.indexOf(episodeNumber)
            if (targetIndex != -1) {
                return targetIndex + 1
            }
        } catch (e: Exception) {
            log.w(e) { "Failed to calculate day index from Cinemeta for $imdbId" }
        }
        return 1
    }

    /**
     * Resolves the IMDb ID for a given AniList ID.
     * Uses the ARM API mapping first, then falls back to ani.zip.
     * Returns null if not found or on network/parse failure.
     */
    suspend fun resolveImdbId(anilistId: Int): String? {
        // 1. Try ARM API mapping
        try {
            val url = "$ARM_API/ids?source=anilist&id=$anilistId"
            val text = httpGetText(url)
            val jsonElement = json.parseToJsonElement(text) as? JsonObject
            val imdbId = jsonElement?.get("imdb")?.jsonPrimitive?.content
            if (!imdbId.isNullOrBlank()) {
                return imdbId
            }
        } catch (e: Exception) {
            log.w(e) { "resolveImdbId: ARM API failed for anilist:$anilistId" }
        }

        // 2. Fallback to ani.zip
        val url = "$ANIZIP_API?anilist_id=$anilistId"
        return try {
            val text = httpGetText(url)
            val parsed = json.decodeFromString<AniZipResponse>(text)
            parsed.mappings?.imdbId?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            log.w(e) { "resolveImdbId: ani.zip failed for anilist:$anilistId" }
            null
        }
    }
}
