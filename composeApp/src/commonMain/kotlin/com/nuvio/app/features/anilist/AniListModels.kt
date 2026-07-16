package com.nuvio.app.features.anilist

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

// --- Authentication & Account State ---

@Serializable
enum class AniListConnectionMode {
    CONNECTED,
    DISCONNECTED,
    LOADING
}

@Serializable
data class AniListAuthState(
    val accessToken: String? = null,
    val username: String? = null,
    val avatarUrl: String? = null,
    val userId: Int? = null,
    val tokenExpiresAtEpochMs: Long? = null
) {
    val isAuthenticated: Boolean
        get() = !accessToken.isNullOrBlank()
}

data class AniListAuthUiState(
    val mode: AniListConnectionMode = AniListConnectionMode.DISCONNECTED,
    val username: String? = null,
    val avatarUrl: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

// --- Sync Settings State ---

@Serializable
data class AniListSectionSettings(
    val type: String,
    val enabled: Boolean = true
)

@Serializable
data class AniListSettingsUiState(
    val enableSync: Boolean = false,
    val syncWatching: Boolean = true,
    val autoSync: Boolean = true,
    val syncOnLaunch: Boolean = true,
    val lastSyncTimestamp: Long = 0L,
    val librarySections: List<AniListSectionSettings> = defaultLibrarySections,
    val markWatchedThreshold: Float = 0.90f,
    val autoAddNewAnime: Boolean = false
) {
    companion object {
        val defaultLibrarySections = listOf(
            AniListSectionSettings("Watching", true),
            AniListSectionSettings("Completed", true),
            AniListSectionSettings("Planning", true),
            AniListSectionSettings("Paused", true),
            AniListSectionSettings("Dropped", true),
            AniListSectionSettings("Rewatching", true),
            AniListSectionSettings("Favorites", true)
        )
    }
}

// --- Library List Items ---

@Serializable
data class AniListLibraryItem(
    val id: Int,                  // AniList Media ID
    val title: String,
    val posterUrl: String? = null,
    val progress: Int = 0,
    val totalEpisodes: Int? = null,
    val score: Int? = null,       // Score in 0-100 scale
    val airingStatus: String? = null,
    val status: String,           // Watching, Completed, Planning, Paused, Dropped, Repeating
    val updatedAt: Long = 0L,     // Epoch seconds from AniList
    val imdbId: String? = null,
    val entryId: Int? = null,
    val format: String? = null
)

data class AniListLibraryUiState(
    val watching: List<AniListLibraryItem> = emptyList(),
    val completed: List<AniListLibraryItem> = emptyList(),
    val planning: List<AniListLibraryItem> = emptyList(),
    val paused: List<AniListLibraryItem> = emptyList(),
    val dropped: List<AniListLibraryItem> = emptyList(),
    val rewatching: List<AniListLibraryItem> = emptyList(),
    val isLoading: Boolean = false,
    val isLoaded: Boolean = false,
    val errorMessage: String? = null
)

// --- GraphQL Request Wrapper ---

@Serializable
data class AniListGraphQLRequest(
    val query: String,
    val variables: JsonObject? = null
)

// --- GraphQL Response DTOs ---

@Serializable
data class AniListUserResponse(
    val data: AniListUserData? = null
)

@Serializable
data class AniListUserData(
    @SerialName("Viewer") val viewer: AniListUserViewer? = null
)

@Serializable
data class AniListUserViewer(
    val id: Int,
    val name: String,
    val avatar: AniListUserAvatar? = null
)

@Serializable
data class AniListUserAvatar(
    val large: String? = null
)

@Serializable
data class AniListCollectionResponse(
    val data: AniListCollectionData? = null
)

@Serializable
data class AniListCollectionData(
    @SerialName("MediaListCollection") val collection: AniListMediaListCollection? = null
)

@Serializable
data class AniListMediaListCollection(
    val lists: List<AniListMediaListGroup>? = null
)

@Serializable
data class AniListMediaListGroup(
    val status: String? = null,
    val entries: List<AniListMediaListEntry>? = null
)

@Serializable
data class AniListMediaListEntry(
    val id: Int? = null,
    val status: String? = null,
    val progress: Int = 0,
    val score: Int? = null,
    val updatedAt: Long = 0L,
    val media: AniListMediaDetails? = null
)

@Serializable
data class AniListMediaDetails(
    val id: Int,
    val idMal: Int? = null,
    val episodes: Int? = null,
    val title: AniListTitle? = null,
    val coverImage: AniListCoverImage? = null,
    val status: String? = null, // Airing status, e.g. FINISHED, RELEASING
    val nextAiringEpisode: AniListNextAiringEpisode? = null,
    val format: String? = null
)

@Serializable
data class AniListCoverImage(
    val extraLarge: String? = null,
    val large: String? = null,
    val medium: String? = null
)

@Serializable
data class AniListNextAiringEpisode(
    val timeUntilAiring: Int? = null,
    val episode: Int? = null
)

@Serializable
data class AniListSaveEntryResponse(
    val data: AniListSaveEntryData? = null
)

@Serializable
data class AniListSaveEntryData(
    @SerialName("SaveMediaListEntry") val entry: AniListMediaListEntry? = null
)

// --- Resolution Helper / Mappings Models ---

@Serializable
data class AniListGraphQLRequestWithSearch(
    val query: String,
    val variables: Map<String, String>
)

@Serializable
data class AniListResponse(
    val data: AniListData? = null
)

@Serializable
data class AniListData(
    val Page: AniListPage? = null
)

@Serializable
data class AniListPage(
    val media: List<AniListMedia>? = null
)

@Serializable
data class AniListMedia(
    val id: Int,
    val title: AniListTitle? = null,
    val format: String? = null,
    val startDate: AniListDate? = null,
    val endDate: AniListDate? = null
)

@Serializable
data class AniListTitle(
    val english: String? = null,
    val romaji: String? = null,
    val userPreferred: String? = null
)

@Serializable
data class AniListDate(
    val year: Int? = null,
    val month: Int? = null,
    val day: Int? = null
)

@Serializable
data class AniZipResponse(
    val titles: Map<String, String>? = null,
    val episodes: Map<String, AniZipEpisode>? = null,
    val mappings: AniZipMappings? = null
)

@Serializable
data class AniZipEpisode(
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val absoluteEpisodeNumber: Int? = null,
    val airDate: String? = null,
    val airDateUtc: String? = null
)

@Serializable
data class AniZipMappings(
    @SerialName("anilist_id") val anilistId: Int? = null,
    @SerialName("imdb_id") val imdbId: String? = null
)
