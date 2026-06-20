package com.nuvio.app.features.mal

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object MalLibraryRepository {
    private const val LIST_FETCH_LIMIT = 1000
    private const val REFRESH_DEDUP_MS = 10_000L
    private const val STATUS_WATCHING = "watching"
    private const val STATUS_COMPLETED = "completed"
    private const val STATUS_ON_HOLD = "on_hold"
    private const val STATUS_DROPPED = "dropped"
    private const val STATUS_PLAN_TO_WATCH = "plan_to_watch"

    private val log = Logger.withTag("MalLibrary")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _uiState = MutableStateFlow(MalLibraryUiState())
    val uiState: StateFlow<MalLibraryUiState> = _uiState.asStateFlow()

    private var hasLoaded = false
    private val refreshMutex = Mutex()
    private var lastRefreshAtMs: Long = 0L

    fun ensureLoaded() {
        if (hasLoaded) return
        hasLoaded = true
        loadFromDisk()
    }

    fun onProfileChanged() {
        hasLoaded = false
        lastRefreshAtMs = 0L
        _uiState.value = MalLibraryUiState()
        ensureLoaded()
    }

    fun clearLocalState() {
        hasLoaded = false
        lastRefreshAtMs = 0L
        _uiState.value = MalLibraryUiState()
        MalLibraryStorage.savePayload("")
    }

    suspend fun refreshNow() {
        refresh(force = true)
    }

    private suspend fun refresh(force: Boolean) {
        ensureLoaded()
        refreshMutex.withLock {
            val now = MalPlatformClock.nowEpochMs()
            val current = _uiState.value
            if (current.hasLoaded && current.errorMessage == null && now - lastRefreshAtMs <= REFRESH_DEDUP_MS && !force) {
                return
            }

            MalAuthRepository.ensureLoaded()
            if (!MalAuthRepository.isAuthenticated.value) {
                _uiState.value = MalLibraryUiState(hasLoaded = true)
                lastRefreshAtMs = 0L
                return
            }

            val username = MalAuthRepository.snapshot().username?.takeIf { it.isNotBlank() } ?: return
            MalAuthRepository.refreshTokenIfNeeded(force = false)
            val token = MalAuthRepository.currentAccessToken() ?: return

            fetchAndPublish(token, username)
            lastRefreshAtMs = MalPlatformClock.nowEpochMs()
        }
    }

    private suspend fun fetchAndPublish(token: String, username: String) {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

        val result = runCatching {
            fetchAllUserAnimeList(token, username)
        }

        result.exceptionOrNull()?.let { error ->
            if (error is CancellationException) throw error
            log.w(error) { "Failed to fetch MAL anime list" }
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                hasLoaded = true,
                errorMessage = error.message?.takeIf { it.isNotBlank() } ?: "Failed to load MAL library",
            )
            return
        }

        val entries = result.getOrThrow()
        val entriesByStatus = entries.groupBy { it.listStatus }
        val allItems = entries.sortedByDescending { it.updatedAtEpochMs ?: 0L }

        _uiState.value = MalLibraryUiState(
            entriesByStatus = entriesByStatus,
            allItems = allItems,
            isLoading = false,
            hasLoaded = true,
            errorMessage = null,
        )
        persistToDisk()
    }

    private suspend fun fetchAllUserAnimeList(token: String, username: String): List<MalLibraryItem> =
        withContext(Dispatchers.Default) {
            val allEntries = mutableListOf<MalLibraryItem>()
            var offset = 0

            while (true) {
                val response = MalApiClient.getUserAnimeList(
                    accessToken = token,
                    userName = username,
                    limit = LIST_FETCH_LIMIT,
                    offset = offset,
                )

                val mapped = response.data.mapNotNull { entry ->
                    val anime = entry.node ?: return@mapNotNull null
                    val listStatus = entry.listStatus ?: return@mapNotNull null
                    val statusValue = listStatus.status ?: return@mapNotNull null

                    MalLibraryItem(
                        id = anime.id ?: return@mapNotNull null,
                        title = anime.title ?: "Unknown",
                        posterUrl = anime.mainPicture?.large ?: anime.mainPicture?.medium,
                        synopsis = anime.synopsis,
                        meanScore = anime.mean,
                        numEpisodes = anime.numEpisodes,
                        status = anime.status,
                        genres = anime.genres?.mapNotNull { it.name }.orEmpty(),
                        mediaType = anime.mediaType,
                        listStatus = statusValue,
                        userScore = listStatus.score,
                        episodesWatched = listStatus.numEpisodesWatched,
                        updatedAtEpochMs = listStatus.updatedAt?.let { parseMalDateTime(it) },
                    )
                }

                allEntries.addAll(mapped)
                val paging = response.paging
                if (paging?.next == null || mapped.size < LIST_FETCH_LIMIT) break
                offset += LIST_FETCH_LIMIT
            }

            allEntries
        }

    private fun parseMalDateTime(dateTime: String): Long? {
        return runCatching {
            java.time.Instant.parse(dateTime).toEpochMilli()
        }.getOrNull()
    }

    private fun loadFromDisk() {
        val payload = MalLibraryStorage.loadPayload().orEmpty().trim()
        if (payload.isBlank()) return

        val cached = runCatching {
            json.decodeFromString<StoredMalLibraryPayload>(payload)
        }.getOrNull() ?: return

        _uiState.value = MalLibraryUiState(
            entriesByStatus = cached.entriesByStatus,
            allItems = cached.allItems,
            isLoading = false,
            hasLoaded = true,
            errorMessage = null,
        )
    }

    private fun persistToDisk() {
        val state = _uiState.value
        val payload = StoredMalLibraryPayload(
            entriesByStatus = state.entriesByStatus,
            allItems = state.allItems,
        )
        MalLibraryStorage.savePayload(json.encodeToString(payload))
    }
}

private data class StoredMalLibraryPayload(
    val entriesByStatus: Map<String, List<MalLibraryItem>> = emptyMap(),
    val allItems: List<MalLibraryItem> = emptyList(),
)
