package com.nuvio.app.features.plugins.cloudstream

import android.util.Log
import com.lagradost.cloudstream3.AnimeLoadResponse
import com.lagradost.cloudstream3.Episode
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.LiveStreamLoadResponse
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.MovieLoadResponse
import com.lagradost.cloudstream3.MovieSearchResponse
import com.lagradost.cloudstream3.TvSeriesSearchResponse
import com.lagradost.cloudstream3.AnimeSearchResponse
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.TvSeriesLoadResponse
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.metaproviders.TmdbLink
import com.lagradost.cloudstream3.metaproviders.TmdbProvider
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.app
import com.nuvio.app.features.plugins.PluginRuntimeResult
import com.nuvio.app.features.plugins.PluginSubtitleResult
import com.nuvio.app.features.tmdb.TmdbService
import com.nuvio.app.features.tmdb.TmdbSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

private const val TAG = "ExtExtensionRunner"
private const val EXECUTION_TIMEOUT_MS = 120_000L
private const val LOADLINKS_TIMEOUT_MS = 60_000L
private const val MIN_TITLE_SIMILARITY = 0.5
private const val MAX_ALT_TITLES = 8

data class TmdbTitleInfo(
    val title: String,
    val originalTitle: String?,
    val year: Int?,
    val alternativeTitles: List<String>,
)

object ExternalExtensionRunner {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private fun extractMissingClass(e: Error): String? {
        val msg = e.message ?: return null
        val match = Regex("""(?:L?)([\w/.]+)(?:;)?""").find(msg)
        return match?.groupValues?.get(1)?.replace('/', '.')
    }

    suspend fun execute(
        scraperId: String,
        tmdbId: String,
        mediaType: String,
        season: Int?,
        episode: Int?
    ): List<PluginRuntimeResult> = withContext(Dispatchers.IO) {
        ExternalExtensionLoader.ensureExtractorsLoaded(listOf(scraperId))

        val api = ExternalExtensionLoader.getApi(scraperId)
        if (api == null) {
            Log.e(TAG, "No API loaded for scraper: $scraperId")
            return@withContext emptyList()
        }

        try {
            executeInternal(api, tmdbId, mediaType, season, episode)
        } catch (e: Exception) {
            Log.e(TAG, "Extension ${api.name} failed: ${e.javaClass.simpleName}: ${e.message}", e)
            emptyList()
        } catch (e: Error) {
            val missing = extractMissingClass(e)
            if (missing != null) {
                Log.e(TAG, "Extension ${api.name} MISSING CLASS: $missing", e)
            } else {
                Log.e(TAG, "Extension ${api.name} linkage error: ${e.javaClass.simpleName}: ${e.message}", e)
            }
            emptyList()
        }
    }

    private suspend fun executeInternal(
        api: MainAPI,
        tmdbId: String,
        mediaType: String,
        season: Int?,
        episode: Int?
    ): List<PluginRuntimeResult> {
        if (api is TmdbProvider) {
            return executeTmdbProvider(api, tmdbId, mediaType, season, episode)
        }
        return executeSearchBased(api, tmdbId, mediaType, season, episode)
    }

    private suspend fun getTmdbTitleInfo(tmdbId: String, mediaType: String): TmdbTitleInfo? {
        val apiKey = TmdbSettingsRepository.snapshot().apiKey
        if (apiKey.isBlank()) return null

        val resolvedId = resolveTmdbId(tmdbId, mediaType, apiKey)
        val numericId = resolvedId?.toIntOrNull() ?: return null
        val type = if (mediaType.lowercase() == "movie") "movie" else "tv"

        return try {
            val url = "https://api.themoviedb.org/3/$type/$numericId?api_key=$apiKey&language=en&append_to_response=alternative_titles,translations"
            val request = Request.Builder().url(url).build()
            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) return null

            val body = response.body?.string()?.let { JSONObject(it) } ?: return null

            val title = body.optString("title", body.optString("name", ""))
            val originalTitle = body.optString("original_title", body.optString("original_name", null))
            val year = try {
                val date = body.optString("release_date", body.optString("first_air_date", ""))
                date.take(4).toIntOrNull()
            } catch (_: Exception) { null }

            val altTitles = mutableListOf<String>()
            val altJson = body.optJSONObject("alternative_titles")
            if (altJson != null) {
                val results = altJson.optJSONArray("titles") ?: altJson.optJSONArray("results")
                if (results != null) {
                    for (i in 0 until results.length()) {
                        val entry = results.getJSONObject(i)
                        val altTitle = entry.optString("title", "")
                        if (altTitle.isNotBlank() && !altTitle.equals(title, ignoreCase = true)) {
                            altTitles.add(altTitle)
                        }
                    }
                }
            }

            TmdbTitleInfo(
                title = title,
                originalTitle = originalTitle?.takeIf { it.isNotBlank() },
                year = year,
                alternativeTitles = altTitles.take(MAX_ALT_TITLES),
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch TMDB title info for $tmdbId: ${e.message}")
            null
        }
    }

    private suspend fun resolveTmdbId(inputId: String, mediaType: String, apiKey: String): String? {
        if (inputId.startsWith("tt")) {
            val normalizedType = if (mediaType.lowercase() == "movie") "movie" else "tv"
            return try {
                val findUrl = "https://api.themoviedb.org/3/find/$inputId?api_key=$apiKey&external_source=imdb_id"
                val request = Request.Builder().url(findUrl).build()
                val response = httpClient.newCall(request).execute()
                if (!response.isSuccessful) return null
                val body = response.body?.string()?.let { JSONObject(it) } ?: return null
                val results = if (normalizedType == "movie") {
                    body.optJSONArray("movie_results")
                } else {
                    body.optJSONArray("tv_results")
                }
                val id = results?.optJSONObject(0)?.optInt("id", -1)
                id?.takeIf { it > 0 }?.toString()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to resolve IMDB ID $inputId to TMDB: ${e.message}")
                null
            }
        }
        return inputId
    }

    private suspend fun executeTmdbProvider(
        api: MainAPI,
        tmdbId: String,
        mediaType: String,
        season: Int?,
        episode: Int?
    ): List<PluginRuntimeResult> {
        val tmdbIdInt = tmdbId.toIntOrNull()
        val isMovie = mediaType.lowercase() == "movie"
        val type = if (isMovie) "movie" else "tv"

        val loadJson = """{"id":$tmdbIdInt,"type":"$type"}"""

        Log.d(TAG, "TmdbProvider ${api.name}: load($loadJson)")
        val loadResponse = try {
            api.load(loadJson)
        } catch (e: Exception) {
            Log.w(TAG, "TmdbProvider ${api.name} load(json) threw: ${e.javaClass.simpleName}: ${e.message?.take(100)}")
            null
        } catch (e: Error) {
            val missing = extractMissingClass(e)
            Log.w(TAG, "TmdbProvider ${api.name} load(json) error: ${missing ?: e.message?.take(100)}")
            null
        }

        if (loadResponse != null) {
            Log.d(TAG, "TmdbProvider ${api.name}: loaded ${loadResponse.javaClass.simpleName}")
            val data = extractData(loadResponse, mediaType, season, episode)
            if (data != null) {
                Log.d(TAG, "TmdbProvider ${api.name}: loadLinks data=${data.take(200)}")
                return executeTmdbLoadLinks(api, data)
            }
            Log.w(TAG, "TmdbProvider ${api.name}: no data for S${season}E${episode}")
        }

        val tmdbUrl = if (isMovie) {
            "https://www.themoviedb.org/movie/$tmdbId"
        } else {
            "https://www.themoviedb.org/tv/$tmdbId"
        }
        Log.d(TAG, "TmdbProvider ${api.name}: fallback load($tmdbUrl)")
        val fallbackResponse = try {
            api.load(tmdbUrl)
        } catch (e: Exception) {
            Log.w(TAG, "TmdbProvider ${api.name} fallback load(url) threw: ${e.javaClass.simpleName}: ${e.message?.take(100)}")
            null
        } catch (e: Error) { null }

        if (fallbackResponse != null) {
            val data = extractData(fallbackResponse, mediaType, season, episode)
            if (data != null) {
                Log.d(TAG, "TmdbProvider ${api.name}: fallback loadLinks data=${data.take(200)}")
                return executeTmdbLoadLinks(api, data)
            }
        }

        Log.w(TAG, "TmdbProvider ${api.name}: both load() paths failed")
        return emptyList()
    }

    private suspend fun executeTmdbLoadLinks(
        api: MainAPI,
        data: String
    ): List<PluginRuntimeResult> {
        val links = java.util.Collections.synchronizedList(mutableListOf<ExtractorLink>())
        val subtitles = java.util.Collections.synchronizedList(mutableListOf<SubtitleFile>())

        val completed = withTimeoutOrNull(LOADLINKS_TIMEOUT_MS) {
            try {
                api.loadLinks(
                    data = data,
                    isCasting = false,
                    subtitleCallback = { subtitles.add(it) },
                    callback = { links.add(it) }
                )
                true
            } catch (e: Exception) {
                Log.w(TAG, "TmdbProvider ${api.name} loadLinks threw: ${e.javaClass.simpleName} (${links.size} links collected)")
                false
            } catch (e: Error) {
                val missing = extractMissingClass(e)
                Log.w(TAG, "TmdbProvider ${api.name} loadLinks error: ${missing ?: e.message} (${links.size} links collected)")
                false
            }
        }
        if (completed == null) {
            Log.w(TAG, "TmdbProvider ${api.name} loadLinks timed out at ${LOADLINKS_TIMEOUT_MS}ms (${links.size} links collected so far)")
        }

        if (links.isEmpty()) {
            Log.w(TAG, "TmdbProvider ${api.name}: 0 links collected")
            return emptyList()
        }

        Log.d(TAG, "TmdbProvider ${api.name}: ${links.size} links, ${subtitles.size} subs")
        return links.filterValid().map { link -> link.toPluginRuntimeResult(api.name) }
    }

    private suspend fun executeSearchBased(
        api: MainAPI,
        tmdbId: String,
        mediaType: String,
        season: Int?,
        episode: Int?
    ): List<PluginRuntimeResult> {
        val titleInfo = getTmdbTitleInfo(tmdbId, mediaType)
        if (titleInfo == null) {
            Log.e(TAG, "Failed to fetch TMDB title info for $tmdbId")
            return emptyList()
        }

        val title = titleInfo.title
        val year = titleInfo.year

        val candidateTitles = buildList {
            add(title)
            titleInfo.originalTitle
                ?.takeIf { it.isNotBlank() && !it.equals(title, ignoreCase = true) }
                ?.let(::add)
            titleInfo.alternativeTitles
                .asSequence()
                .filter { it.isNotBlank() && isLatinScript(it) }
                .distinctBy { it.lowercase() }
                .filter { alt -> none { it.equals(alt, ignoreCase = true) } }
                .take(MAX_ALT_TITLES)
                .forEach(::add)
        }

        Log.d(TAG, "SearchBased ${api.name}: searching for \"$title\" (${candidateTitles.size} candidates)")

        var outcome = trySearch(api, title)
        var searchResults = outcome.items
        var hostDead = outcome.hostUnreachable
        var unsupported = outcome.unsupported

        if (searchResults.isNullOrEmpty() && !hostDead && !unsupported && title.contains(Regex("[:\\-–—]"))) {
            val simplified = title.replace(Regex("[:\\-–—]"), " ").replace(Regex("\\s+"), " ").trim()
            Log.d(TAG, "SearchBased ${api.name}: retrying with simplified \"$simplified\"")
            outcome = trySearch(api, simplified)
            searchResults = outcome.items
            if (outcome.hostUnreachable) hostDead = true
            if (outcome.unsupported) unsupported = true
        }

        if (searchResults.isNullOrEmpty() && !hostDead && !unsupported) {
            val alts = candidateTitles.drop(1)
            if (alts.isNotEmpty()) {
                Log.d(TAG, "SearchBased ${api.name}: trying ${alts.size} alt titles in parallel")
                val altOutcomes = coroutineScope {
                    alts.map { alt -> async { alt to trySearch(api, alt) } }.awaitAll()
                }
                altOutcomes.firstOrNull { it.second.hostUnreachable }?.let { hostDead = true }
                altOutcomes.firstOrNull { it.second.unsupported }?.let { unsupported = true }
                if (!hostDead && !unsupported) {
                    altOutcomes.firstOrNull { !it.second.items.isNullOrEmpty() }?.let { (alt, o) ->
                        Log.d(TAG, "SearchBased ${api.name}: alt title \"$alt\" returned ${o.items?.size ?: 0} results")
                        searchResults = o.items
                    }
                }
            }
        }

        if (searchResults.isNullOrEmpty()) {
            when {
                hostDead -> Log.w(TAG, "SearchBased ${api.name}: host unreachable, skipping (primary=\"$title\")")
                unsupported -> Log.w(TAG, "SearchBased ${api.name}: search() unsupported, skipping (primary=\"$title\")")
                else -> Log.w(TAG, "SearchBased ${api.name}: 0 search results for any of ${candidateTitles.size} titles (primary=\"$title\")")
            }
            return emptyList()
        }
        Log.d(TAG, "SearchBased ${api.name}: ${searchResults.size} results")

        val bestMatch = findBestMatch(searchResults, candidateTitles, year, mediaType)
        if (bestMatch == null) {
            Log.d(TAG, "No suitable match in ${api.name} results for: $title ($year) [candidates=${candidateTitles.size}]")
            searchResults.take(5).forEachIndexed { i, r ->
                val sim = candidateTitles.maxOf { calculateSimilarity(r.name, it) }
                Log.d(TAG, "  [$i] \"${r.name}\" (sim=${String.format("%.2f", sim)}, type=${r.type})")
            }
            return emptyList()
        }
        Log.d(TAG, "Best match from ${api.name}: ${bestMatch.name} (${bestMatch.url})")

        val loadResponse = try {
            api.load(bestMatch.url)
        } catch (e: Exception) {
            Log.e(TAG, "SearchBased ${api.name} load() threw: ${e.javaClass.simpleName}: ${e.message}", e)
            null
        } catch (e: Error) {
            val missing = extractMissingClass(e)
            Log.e(TAG, "SearchBased ${api.name} load() error: ${missing ?: e.message}", e)
            null
        }
        if (loadResponse == null) {
            Log.w(TAG, "SearchBased ${api.name}: load(${bestMatch.url}) returned null")
            return emptyList()
        }
        Log.d(TAG, "SearchBased ${api.name}: loaded ${loadResponse.javaClass.simpleName}")

        val data = extractData(loadResponse, mediaType, season, episode)
        if (data == null) {
            Log.d(TAG, "No data extracted from ${api.name} for S${season}E${episode}")
            return emptyList()
        }

        val links = java.util.Collections.synchronizedList(mutableListOf<ExtractorLink>())
        val subtitles = java.util.Collections.synchronizedList(mutableListOf<SubtitleFile>())

        val success = withTimeoutOrNull(LOADLINKS_TIMEOUT_MS) {
            try {
                api.loadLinks(
                    data = data,
                    isCasting = false,
                    subtitleCallback = { subtitles.add(it) },
                    callback = { links.add(it) }
                )
            } catch (e: Exception) {
                Log.e(TAG, "SearchBased ${api.name} loadLinks threw: ${e.javaClass.simpleName}: ${e.message}", e)
                false
            } catch (e: Error) {
                val missing = extractMissingClass(e)
                Log.e(TAG, "SearchBased ${api.name} loadLinks error: ${missing ?: e.message}", e)
                false
            }
        }
        if (success == null) {
            Log.w(TAG, "SearchBased ${api.name} loadLinks timed out at ${LOADLINKS_TIMEOUT_MS}ms (${links.size} links collected so far)")
        }

        if (success != true && links.isEmpty()) {
            Log.w(TAG, "SearchBased ${api.name}: loadLinks returned false/null, 0 links")
            return emptyList()
        }

        Log.d(TAG, "SearchBased ${api.name}: ${links.size} links, ${subtitles.size} subs")
        return links.filterValid().map { link -> link.toPluginRuntimeResult(api.name) }
    }

    private fun getSearchResponseYear(result: SearchResponse): Int? = when (result) {
        is MovieSearchResponse -> result.year
        is TvSeriesSearchResponse -> result.year
        is AnimeSearchResponse -> result.year
        else -> null
    }

    private fun findBestMatch(
        results: List<SearchResponse>,
        candidateTitles: List<String>,
        targetYear: Int?,
        mediaType: String
    ): SearchResponse? {
        val isMovie = mediaType.lowercase() == "movie"
        val movieTypes = setOf(TvType.Movie, TvType.AnimeMovie, TvType.Documentary)
        val tvTypes = setOf(TvType.TvSeries, TvType.Anime, TvType.OVA, TvType.Cartoon, TvType.AsianDrama)
        val catchAllTvTypes = setOf(TvType.Anime, TvType.OVA, TvType.AsianDrama)

        return results
            .mapNotNull { result ->
                val resultType = result.type
                val resultYear = getSearchResponseYear(result)
                val titleSimilarity = candidateTitles.maxOf { calculateSimilarity(result.name, it) }
                val isExactTitle = titleSimilarity >= 0.95

                if (resultType != null && !isExactTitle) {
                    val typeOk = if (isMovie) resultType in movieTypes else resultType in tvTypes
                    if (!typeOk) return@mapNotNull null
                }
                if (!isMovie && resultType in catchAllTvTypes && titleSimilarity < 0.9) {
                    return@mapNotNull null
                }
                if (targetYear != null && resultYear != null &&
                    kotlin.math.abs(targetYear - resultYear) > 1) {
                    return@mapNotNull null
                }

                val yearBonus = if (targetYear != null && resultYear == targetYear) 0.15 else 0.0
                val typeBonus = when {
                    resultType == null -> 0.0
                    isMovie && resultType in movieTypes -> 0.05
                    !isMovie && resultType in tvTypes -> 0.05
                    else -> 0.0
                }
                val score = titleSimilarity + yearBonus + typeBonus
                result to score
            }
            .filter { it.second >= MIN_TITLE_SIMILARITY }
            .maxByOrNull { it.second }
            ?.first
    }

    private data class SearchOutcome(
        val items: List<SearchResponse>?,
        val hostUnreachable: Boolean = false,
        val unsupported: Boolean = false,
    )

    private suspend fun trySearch(api: MainAPI, query: String): SearchOutcome = try {
        SearchOutcome(api.search(query, 1)?.items)
    } catch (e: java.net.UnknownHostException) {
        Log.e(TAG, "SearchBased ${api.name} search(\"$query\") DNS fail: ${e.message}")
        SearchOutcome(null, hostUnreachable = true)
    } catch (e: NotImplementedError) {
        Log.e(TAG, "SearchBased ${api.name}: search() not implemented; skipping provider")
        SearchOutcome(null, unsupported = true)
    } catch (e: Exception) {
        Log.e(TAG, "SearchBased ${api.name} search(\"$query\") threw: ${e.javaClass.simpleName}: ${e.message}", e)
        SearchOutcome(null)
    } catch (e: Error) {
        val missing = extractMissingClass(e)
        Log.e(TAG, "SearchBased ${api.name} search(\"$query\") error: ${missing ?: e.message}", e)
        SearchOutcome(null)
    }

    private fun isLatinScript(s: String): Boolean {
        val letters = s.filter(Char::isLetter)
        if (letters.isEmpty()) return true
        val latinCount = letters.count { c ->
            when (Character.UnicodeBlock.of(c)) {
                Character.UnicodeBlock.BASIC_LATIN,
                Character.UnicodeBlock.LATIN_1_SUPPLEMENT,
                Character.UnicodeBlock.LATIN_EXTENDED_A,
                Character.UnicodeBlock.LATIN_EXTENDED_B,
                Character.UnicodeBlock.LATIN_EXTENDED_ADDITIONAL -> true
                else -> false
            }
        }
        return latinCount.toDouble() / letters.length >= 0.7
    }

    private fun extractData(
        response: LoadResponse,
        mediaType: String,
        season: Int?,
        episode: Int?
    ): String? = when (response) {
        is MovieLoadResponse -> response.dataUrl
        is LiveStreamLoadResponse -> response.dataUrl
        is TvSeriesLoadResponse -> {
            findEpisode(response.episodes, season, episode)?.data
        }
        is AnimeLoadResponse -> {
            val allEpisodes = response.episodes.values.flatten()
            findEpisode(allEpisodes, season, episode)?.data
        }
        else -> null
    }

    private fun findEpisode(episodes: List<Episode>, season: Int?, episode: Int?): Episode? {
        if (episodes.isEmpty()) return null

        if (season != null && episode != null) {
            episodes.firstOrNull { it.season == season && it.episode == episode }?.let { return it }
        }

        if (episode != null) {
            episodes.firstOrNull { it.episode == episode && (it.season == null || it.season == season) }
                ?.let { return it }
        }

        if (season != null && episode != null) {
            val absoluteEpisode = episodes.indexOfFirst {
                (it.season == season || it.season == null) && it.episode == episode
            }
            if (absoluteEpisode >= 0) return episodes[absoluteEpisode]
        }

        return null
    }

    private fun calculateSimilarity(s1: String, s2: String): Double {
        val a = s1.lowercase().trim()
        val b = s2.lowercase().trim()
        if (a == b) return 1.0
        if (a.isEmpty() || b.isEmpty()) return 0.0

        val aNorm = normalizeTitleForMatch(a)
        val bNorm = normalizeTitleForMatch(b)
        if (aNorm == bNorm) return 0.95
        if (aNorm.isEmpty() || bNorm.isEmpty()) return 0.0

        if (aNorm.contains(bNorm) || bNorm.contains(aNorm)) {
            val shortLen = minOf(aNorm.length, bNorm.length).toDouble()
            val longLen = maxOf(aNorm.length, bNorm.length).toDouble()
            val ratio = shortLen / longLen
            if (ratio >= 0.8) return 0.85
        }

        val distance = levenshteinDistance(aNorm, bNorm)
        val maxLen = maxOf(aNorm.length, bNorm.length)
        return 1.0 - (distance.toDouble() / maxLen)
    }

    private fun normalizeTitleForMatch(lowered: String): String {
        return lowered
            .replace(Regex("\\(\\d{4}\\)"), " ")
            .replace(Regex("\\b\\d{4}\\b"), " ")
            .replace(Regex("\\b(temporada|season)\\s*\\d+\\b"), " ")
            .replace(Regex("\\b[st]\\d{1,2}\\b"), " ")
            .replace(Regex("\\b(part|parte)\\s*\\d+\\b"), " ")
            .replace(Regex("\\b(latino|castellano|subtitulado|sub\\s*espa(ñ|n)ol|espa(ñ|n)ol|dual|vose|vostfr|subbed|dubbed)\\b"), " ")
            .replace(Regex("[:\\-–—]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val m = s1.length
        val n = s2.length
        val dp = Array(m + 1) { IntArray(n + 1) }

        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j

        for (i in 1..m) {
            for (j in 1..n) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[m][n]
    }

    private fun List<ExtractorLink>.filterValid(): List<ExtractorLink> {
        return filter { link ->
            val url = link.url
            when {
                url.isBlank() -> false
                url == "error" || url == "null" -> false
                !url.startsWith("http://") && !url.startsWith("https://") -> false
                else -> true
            }.also { valid ->
                if (!valid) Log.w(TAG, "Filtered invalid link: source=${link.source}, url=${url.take(60)}")
            }
        }
    }

    private fun ExtractorLink.toPluginRuntimeResult(providerName: String): PluginRuntimeResult {
        val qualityStr = Qualities.getStringByInt(quality).ifEmpty { null }
        val streamType = when (type) {
            ExtractorLinkType.M3U8 -> "hls"
            ExtractorLinkType.DASH -> "dash"
            else -> null
        }
        val allHeaders = buildMap {
            putAll(headers)
            if (referer.isNotBlank()) put("Referer", referer)
        }

        return PluginRuntimeResult(
            title = name,
            name = source,
            url = url,
            quality = qualityStr,
            type = streamType,
            headers = allHeaders.ifEmpty { null },
            provider = providerName,
        )
    }
}
