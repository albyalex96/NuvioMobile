package com.nuvio.app.features.anilist

import co.touchlab.kermit.Logger
import com.nuvio.app.features.addons.httpPostJsonWithHeaders
import com.nuvio.app.features.watchprogress.WatchProgressClock
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object AniListApi {
    private val log = Logger.withTag("AniListApi")
    private const val GRAPHQL_URL = "https://graphql.anilist.co/"
    private const val TOKEN_URL = "https://anilist.co/api/v2/oauth/token"

    private const val MIN_REQUEST_INTERVAL_MS = 1600L
    private const val RATE_LIMIT_BASE_DELAY_MS = 5000L

    private var lastRequestTimeMs = 0L

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        coerceInputValues = true
    }

    private suspend fun enforceMinInterval() {
        val now = currentTimeMs()
        val elapsed = now - lastRequestTimeMs
        if (elapsed < MIN_REQUEST_INTERVAL_MS) {
            delay(MIN_REQUEST_INTERVAL_MS - elapsed)
        }
    }

    private fun currentTimeMs(): Long =
        WatchProgressClock.nowEpochMs()

    private suspend fun executeGraphQL(
        token: String,
        query: String,
        variables: kotlinx.serialization.json.JsonObject? = null,
        retryCount: Int = 3
    ): String {
        val request = AniListGraphQLRequest(query = query, variables = variables)
        val body = json.encodeToString(request)
        val headers = mapOf(
            "Authorization" to "Bearer $token",
            "Content-Type" to "application/json",
            "Accept" to "application/json"
        )

        var attempt = 0
        while (true) {
            attempt++
            try {
                enforceMinInterval()
                val result = httpPostJsonWithHeaders(GRAPHQL_URL, body, headers)
                lastRequestTimeMs = currentTimeMs()
                return result
            } catch (e: Exception) {
                val msg = e.message.orEmpty()
                log.w { "GraphQL Request failed (attempt $attempt/$retryCount): $msg" }
                val isRateLimit = msg.contains("429")
                if (attempt >= retryCount) {
                    if (isRateLimit) {
                        log.e { "Rate limited after $retryCount attempts. Waiting longer before giving up." }
                    }
                    throw e
                }
                if (isRateLimit) {
                    val backoff = RATE_LIMIT_BASE_DELAY_MS * attempt
                    log.w { "Rate limited. Backing off ${backoff}ms before retry." }
                    delay(backoff)
                } else {
                    delay(attempt * 1000L)
                }
            }
        }
    }

    suspend fun fetchViewer(token: String): AniListUserViewer? {
        val query = """
            query {
                Viewer {
                    id
                    name
                    avatar {
                        large
                    }
                }
            }
        """.trimIndent()

        return try {
            val response = executeGraphQL(token, query)
            val parsed = json.decodeFromString<AniListUserResponse>(response)
            parsed.data?.viewer
        } catch (e: Exception) {
            log.e { "Failed to fetch AniList viewer: ${e.message}" }
            null
        }
    }

    suspend fun fetchMediaListCollection(userId: Int, token: String): List<AniListLibraryItem> {
        val query = """
            query (${'$'}userId: Int) {
                MediaListCollection(userId: ${'$'}userId, type: ANIME) {
                    lists {
                        status
                        entries {
                            id
                            status
                            progress
                            score(format: POINT_100)
                            updatedAt
                            media {
                                id
                                idMal
                                episodes
                                status
                                format
                                title {
                                    english
                                    romaji
                                    userPreferred
                                }
                                coverImage {
                                    extraLarge
                                    large
                                    medium
                                }
                            }
                        }
                    }
                }
            }
        """.trimIndent()

        val variables = buildJsonObject {
            put("userId", userId)
        }

        return try {
            val response = executeGraphQL(token, query, variables)
            val parsed = json.decodeFromString<AniListCollectionResponse>(response)
            val lists = parsed.data?.collection?.lists ?: return emptyList()

            lists.flatMap { listGroup ->
                val groupStatus = listGroup.status.orEmpty()
                val entries = listGroup.entries ?: return@flatMap emptyList()
                entries.mapNotNull { entry ->
                    val media = entry.media ?: return@mapNotNull null
                    AniListLibraryItem(
                        id = media.id,
                        title = media.title?.userPreferred ?: media.title?.english ?: media.title?.romaji ?: "Unknown Title",
                        posterUrl = media.coverImage?.extraLarge ?: media.coverImage?.large ?: media.coverImage?.medium,
                        progress = entry.progress,
                        totalEpisodes = media.episodes,
                        score = entry.score,
                        airingStatus = media.status,
                        status = groupStatus,
                        updatedAt = entry.updatedAt,
                        entryId = entry.id,
                        format = media.format
                    )
                }
            }
        } catch (e: Exception) {
            log.e { "Failed to fetch AniList collection: ${e.message}" }
            emptyList()
        }
    }

    suspend fun saveMediaListEntry(
        token: String,
        mediaId: Int,
        status: String,
        progress: Int,
        scoreRaw: Int?
    ): Boolean {
        val query = """
            mutation (${'$'}mediaId: Int, ${'$'}status: MediaListStatus, ${'$'}progress: Int, ${'$'}scoreRaw: Int) {
                SaveMediaListEntry(mediaId: ${'$'}mediaId, status: ${'$'}status, progress: ${'$'}progress, scoreRaw: ${'$'}scoreRaw) {
                    id
                    status
                    progress
                    score
                }
            }
        """.trimIndent()

        val variables = buildJsonObject {
            put("mediaId", mediaId)
            // AniList GraphQL expects CURRENT for "Watching" ÔÇö all other statuses map literally.
            put("status", status.toAniListStatus())
            put("progress", progress)
            if (scoreRaw != null && scoreRaw > 0) {
                put("scoreRaw", scoreRaw)
            }
        }

        return try {
            executeGraphQL(token, query, variables)
            true
        } catch (e: Exception) {
            log.e { "Failed to save AniList media entry (id: $mediaId): ${e.message}" }
            false
        }
    }

    suspend fun deleteMediaListEntry(token: String, mediaListEntryId: Int): Boolean {
        val query = """
            mutation (${'$'}id: Int) {
                DeleteMediaListEntry(id: ${'$'}id) {
                    deleted
                }
            }
        """.trimIndent()

        val variables = buildJsonObject {
            put("id", mediaListEntryId)
        }

        return try {
            executeGraphQL(token, query, variables)
            true
        } catch (e: Exception) {
            log.e { "Failed to delete AniList entry (id: $mediaListEntryId): ${e.message}" }
            false
        }
    }

    @Serializable
    private data class TokenExchangeRequest(
        val grant_type: String,
        val client_id: String,
        val client_secret: String,
        val redirect_uri: String,
        val code: String
    )

    @Serializable
    private data class TokenExchangeResponse(
        val access_token: String? = null,
        val refresh_token: String? = null,
        val expires_in: Long? = null
    )

    data class TokenResult(
        val accessToken: String,
        val refreshToken: String?,
        val expiresInSeconds: Long
    )

    suspend fun exchangeCodeForToken(code: String): TokenResult? {
        val requestBody = json.encodeToString(
            TokenExchangeRequest(
                grant_type = "authorization_code",
                client_id = AniListConfig.CLIENT_ID,
                client_secret = AniListConfig.CLIENT_SECRET,
                redirect_uri = AniListConfig.REDIRECT_URI,
                code = code
            )
        )

        return try {
            val response = httpPostJsonWithHeaders(
                url = TOKEN_URL,
                body = requestBody,
                headers = mapOf("Content-Type" to "application/json")
            )
            val parsed = json.decodeFromString<TokenExchangeResponse>(response)
            val token = parsed.access_token
            if (!token.isNullOrBlank()) {
                TokenResult(
                    accessToken = token,
                    refreshToken = parsed.refresh_token,
                    expiresInSeconds = parsed.expires_in ?: 31536000L
                )
            } else {
                null
            }
        } catch (e: Exception) {
            log.e { "Failed to exchange AniList oauth code: ${e.message}" }
            null
        }
    }

    suspend fun refreshAccessToken(refreshToken: String): TokenResult? {
        val body = json.encodeToString(
            buildJsonObject {
                put("grant_type", "refresh_token")
                put("refresh_token", refreshToken)
                put("client_id", AniListConfig.CLIENT_ID)
                put("client_secret", AniListConfig.CLIENT_SECRET)
            }
        )

        return try {
            val response = httpPostJsonWithHeaders(
                url = TOKEN_URL,
                body = body,
                headers = mapOf("Content-Type" to "application/json")
            )
            val parsed = json.decodeFromString<TokenExchangeResponse>(response)
            val token = parsed.access_token
            if (!token.isNullOrBlank()) {
                TokenResult(
                    accessToken = token,
                    refreshToken = parsed.refresh_token ?: refreshToken,
                    expiresInSeconds = parsed.expires_in ?: 31536000L
                )
            } else {
                null
            }
        } catch (e: Exception) {
            log.e { "Failed to refresh AniList access token: ${e.message}" }
            null
        }
    }
}

/**
 * Maps display/internal status strings to the AniList GraphQL MediaListStatus enum.
 * AniList uses CURRENT for "Watching" ÔÇö all other statuses are their literal uppercase name.
 */
private fun String.toAniListStatus(): String = when (uppercase()) {
    "WATCHING", "CURRENT" -> "CURRENT"
    "REPEATING"           -> "REPEATING"
    else                  -> uppercase() // COMPLETED, PLANNING, PAUSED, DROPPED
}

