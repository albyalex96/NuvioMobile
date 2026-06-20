package com.nuvio.app.features.mal

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MalAuthState(
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val tokenType: String? = null,
    val createdAt: Long? = null,
    val expiresIn: Int? = null,
    val username: String? = null,
    val pendingAuthorizationState: String? = null,
    val pendingAuthorizationStartedAtMillis: Long? = null,
) {
    val isAuthenticated: Boolean
        get() = !accessToken.isNullOrBlank() && !refreshToken.isNullOrBlank()
}

enum class MalConnectionMode {
    DISCONNECTED,
    AWAITING_APPROVAL,
    CONNECTED,
}

data class MalAuthUiState(
    val mode: MalConnectionMode = MalConnectionMode.DISCONNECTED,
    val credentialsConfigured: Boolean = true,
    val isLoading: Boolean = false,
    val username: String? = null,
    val tokenExpiresAtMillis: Long? = null,
    val pendingAuthorizationStartedAtMillis: Long? = null,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
)

@Serializable
data class MalTokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("token_type") val tokenType: String,
    @SerialName("expires_in") val expiresIn: Int,
)

@Serializable
data class MalUserResponse(
    val id: Long? = null,
    val name: String? = null,
)

@Serializable
data class MalSearchResponse(
    val data: List<MalAnimeNode> = emptyList(),
    val paging: MalPaging? = null,
)

@Serializable
data class MalAnimeNode(
    val node: MalAnime? = null,
)

@Serializable
data class MalAnime(
    val id: Long? = null,
    val title: String? = null,
    @SerialName("main_picture") val mainPicture: MalPicture? = null,
    val synopsis: String? = null,
    val mean: Double? = null,
    val rank: Int? = null,
    val popularity: Int? = null,
    @SerialName("num_episodes") val numEpisodes: Int? = null,
    val status: String? = null,
    val genres: List<MalGenre>? = null,
    val mediaType: String? = null,
    val averageEpisodeDuration: Int? = null,
    val ratings: MalRatings? = null,
)

@Serializable
data class MalPicture(
    val medium: String? = null,
    val large: String? = null,
)

@Serializable
data class MalGenre(
    val id: Int? = null,
    val name: String? = null,
)

@Serializable
data class MalRatings(
    val mean: Double? = null,
    val rank: Int? = null,
)

@Serializable
data class MalPaging(
    val next: String? = null,
    val previous: String? = null,
)

// User anime list (GET /users/{name}/animelist)
@Serializable
data class MalUserAnimeListResponse(
    val data: List<MalUserAnimeEntry> = emptyList(),
    val paging: MalPaging? = null,
)

@Serializable
data class MalUserAnimeEntry(
    val node: MalAnime? = null,
    @SerialName("list_status") val listStatus: MalAnimeListStatus? = null,
)

@Serializable
data class MalAnimeListStatus(
    val status: String? = null,
    val score: Int? = null,
    @SerialName("num_episodes_watched") val numEpisodesWatched: Int? = null,
    @SerialName("is_rewatching") val isRewatching: Boolean? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

data class MalLibraryUiState(
    val entriesByStatus: Map<String, List<MalLibraryItem>> = emptyMap(),
    val allItems: List<MalLibraryItem> = emptyList(),
    val isLoading: Boolean = false,
    val hasLoaded: Boolean = false,
    val errorMessage: String? = null,
)

data class MalLibraryItem(
    val id: Long,
    val title: String,
    val posterUrl: String? = null,
    val synopsis: String? = null,
    val meanScore: Double? = null,
    val numEpisodes: Int? = null,
    val status: String? = null,
    val genres: List<String> = emptyList(),
    val mediaType: String? = null,
    val listStatus: String,
    val userScore: Int? = null,
    val episodesWatched: Int? = null,
    val updatedAtEpochMs: Long? = null,
)
