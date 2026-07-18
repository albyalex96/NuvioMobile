package com.nuvio.app.features.anilist

import co.touchlab.kermit.Logger
import com.nuvio.app.features.watchprogress.WatchProgressClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object AniListLibraryRepository {
    private val log = Logger.withTag("AniListLibrary")
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val refreshMutex = Mutex()

    private val _uiState = MutableStateFlow(AniListLibraryUiState())
    val uiState: StateFlow<AniListLibraryUiState> = _uiState.asStateFlow()

    private var hasLoaded = false
    private var lastRefreshAtMs = 0L
    private const val CACHE_TTL_MS = 60_000L * 5 // 5 minutes cache TTL

    fun ensureLoaded() {
        if (hasLoaded) return
        loadSnapshotFromDisk()
    }

    fun onProfileChanged() {
        hasLoaded = false
        lastRefreshAtMs = 0L
        _uiState.value = AniListLibraryUiState()
        ensureLoaded()
    }

    fun clearLocalState() {
        hasLoaded = false
        lastRefreshAtMs = 0L
        _uiState.value = AniListLibraryUiState()
        runCatching { AniListStorage.saveLibraryPayload("") }
    }

    /**
     * Returns true if [anilistId] is already in the user's library (any status).
     * Uses the in-memory snapshot ÔÇö does NOT trigger a network call.
     */
    fun isInLibrary(anilistId: Int): Boolean {
        val state = _uiState.value
        return (state.watching + state.completed + state.planning +
                state.paused + state.dropped + state.rewatching)
            .any { it.id == anilistId }
    }

    fun findAniListItemById(anilistId: Int): AniListLibraryItem? {
        val state = _uiState.value
        return (state.watching + state.completed + state.planning +
                state.paused + state.dropped + state.rewatching)
            .find { it.id == anilistId }
    }

    suspend fun refreshNow() {
        refresh(force = true)
    }

    suspend fun ensureFresh() {
        refresh(force = false)
    }

    private suspend fun refresh(force: Boolean) {
        ensureLoaded()
        refreshMutex.withLock {
            val now = WatchProgressClock.nowEpochMs()
            if (!force && _uiState.value.isLoaded && now - lastRefreshAtMs <= CACHE_TTL_MS) {
                return
            }

            val token = AniListAuthRepository.getAccessTokenRefreshed()
            val userId = AniListAuthRepository.getUserId()

            if (token.isNullOrBlank() || userId == null) {
                _uiState.value = AniListLibraryUiState()
                lastRefreshAtMs = 0L
                return
            }

            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            try {
                val items = AniListApi.fetchMediaListCollection(userId, token)
                
                // Group items by their status
                val watching = items.filter { it.status.equals("CURRENT", ignoreCase = true) }
                val completed = items.filter { it.status.equals("COMPLETED", ignoreCase = true) }
                val planning = items.filter { it.status.equals("PLANNING", ignoreCase = true) }
                val paused = items.filter { it.status.equals("PAUSED", ignoreCase = true) }
                val dropped = items.filter { it.status.equals("DROPPED", ignoreCase = true) }
                val rewatching = items.filter { it.status.equals("REPEATING", ignoreCase = true) }

                val newState = AniListLibraryUiState(
                    watching = watching,
                    completed = completed,
                    planning = planning,
                    paused = paused,
                    dropped = dropped,
                    rewatching = rewatching,
                    isLoading = false,
                    isLoaded = true,
                    errorMessage = null
                )

                _uiState.value = newState
                lastRefreshAtMs = now
                persistSnapshot(items)
            } catch (e: Exception) {
                log.e { "Failed to refresh AniList library: ${e.message}" }
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to refresh library collections."
                )
            }
        }
    }

    private fun loadSnapshotFromDisk() {
        hasLoaded = true
        val payload = AniListStorage.loadLibraryPayload().orEmpty().trim()
        if (payload.isBlank()) {
            _uiState.value = AniListLibraryUiState()
            return
        }

        runCatching {
            val items = json.decodeFromString<List<AniListLibraryItem>>(payload)
            val watching = items.filter { it.status.equals("CURRENT", ignoreCase = true) }
            val completed = items.filter { it.status.equals("COMPLETED", ignoreCase = true) }
            val planning = items.filter { it.status.equals("PLANNING", ignoreCase = true) }
            val paused = items.filter { it.status.equals("PAUSED", ignoreCase = true) }
            val dropped = items.filter { it.status.equals("DROPPED", ignoreCase = true) }
            val rewatching = items.filter { it.status.equals("REPEATING", ignoreCase = true) }

            _uiState.value = AniListLibraryUiState(
                watching = watching,
                completed = completed,
                planning = planning,
                paused = paused,
                dropped = dropped,
                rewatching = rewatching,
                isLoading = false,
                isLoaded = true,
                errorMessage = null
            )
        }.onFailure {
            log.w { "Failed to parse cached AniList library items: ${it.message}" }
            _uiState.value = AniListLibraryUiState()
        }
    }

    private fun persistSnapshot(items: List<AniListLibraryItem>) {
        runCatching {
            val payload = json.encodeToString(items)
            AniListStorage.saveLibraryPayload(payload)
        }.onFailure {
            log.w { "Failed to save cached AniList library: ${it.message}" }
        }
    }
}
