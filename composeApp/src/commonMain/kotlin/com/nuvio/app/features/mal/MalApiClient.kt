package com.nuvio.app.features.mal

import com.nuvio.app.features.addons.httpGetTextWithHeaders
import kotlinx.serialization.json.Json

object MalApiClient {
    private const val BASE_URL = "https://api.myanimelist.net/v2"
    private val json = Json { ignoreUnknownKeys = true }

    private fun publicHeaders(clientId: String): Map<String, String> = mapOf(
        "X-MAL-CLIENT-ID" to clientId,
    )

    private fun authHeaders(accessToken: String): Map<String, String> = mapOf(
        "Authorization" to "Bearer $accessToken",
    )

    suspend fun getUser(
        accessToken: String,
    ): MalUserResponse {
        val url = "$BASE_URL/users/@me"
        val response = httpGetTextWithHeaders(url, authHeaders(accessToken))
        return json.decodeFromString<MalUserResponse>(response)
    }

    suspend fun searchAnime(
        clientId: String,
        query: String,
        limit: Int = 10,
        offset: Int = 0,
        fields: String = "id,title,main_picture,synopsis,mean,rank,popularity,num_episodes,status,media_type,genres",
    ): MalSearchResponse {
        val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
        val url = "$BASE_URL/anime?q=$encodedQuery&limit=$limit&offset=$offset&fields=$fields"
        val response = httpGetTextWithHeaders(url, publicHeaders(clientId))
        return json.decodeFromString<MalSearchResponse>(response)
    }

    suspend fun getAnimeDetails(
        clientId: String,
        animeId: Long,
        fields: String = "id,title,main_picture,synopsis,mean,rank,popularity,num_episodes,status,media_type,genres,average_episode_duration,studios",
    ): MalAnime {
        val url = "$BASE_URL/anime/$animeId?fields=$fields"
        val response = httpGetTextWithHeaders(url, publicHeaders(clientId))
        return json.decodeFromString<MalAnime>(response)
    }

    suspend fun getUserAnimeList(
        accessToken: String,
        userName: String,
        status: String? = null,
        limit: Int = 1000,
        offset: Int = 0,
        fields: String = "id,title,main_picture,synopsis,mean,num_episodes,status,genres,media_type",
    ): MalUserAnimeListResponse {
        val url = buildString {
            append("$BASE_URL/users/$userName/animelist?limit=$limit&offset=$offset&fields=$fields")
            if (!status.isNullOrBlank()) {
                append("&status=").append(status)
            }
        }
        val response = httpGetTextWithHeaders(url, authHeaders(accessToken))
        return json.decodeFromString<MalUserAnimeListResponse>(response)
    }

    suspend fun getRanking(
        clientId: String,
        rankingType: String = "all",
        limit: Int = 10,
        offset: Int = 0,
        fields: String = "id,title,main_picture,synopsis,mean,rank,popularity,num_episodes,status,media_type",
    ): MalSearchResponse {
        val url = "$BASE_URL/anime/ranking?ranking_type=$rankingType&limit=$limit&offset=$offset&fields=$fields"
        val response = httpGetTextWithHeaders(url, publicHeaders(clientId))
        return json.decodeFromString<MalSearchResponse>(response)
    }
}
