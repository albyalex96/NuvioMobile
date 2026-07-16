package com.nuvio.app.features.anilist

import co.touchlab.kermit.Logger
import com.nuvio.app.features.addons.httpGetText
import com.nuvio.app.features.watchprogress.WatchProgressClock
import com.nuvio.app.features.watchprogress.WatchProgressRepository
import com.nuvio.app.features.watchprogress.WatchProgressEntry
import com.nuvio.app.features.watched.WatchedRepository
import com.nuvio.app.features.watched.WatchedItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

object AniListSyncCoordinator {
    private val log = Logger.withTag("AniListSync")
    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val syncMutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true }

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    private var syncJob: Job? = null

    // Cache to map AniList ID -> IMDb ID to avoid redundant network hits during a sync cycle
    private val anilistToImdbCache = mutableMapOf<Int, String>()

    // Cache to track the last successfully synced progress/status to avoid duplicate/redundant sync calls during active playback
    private val lastSyncedProgress = mutableMapOf<Int, Pair<Int, String>>()

    fun syncNow() {
        if (_isSyncing.value) return
        syncJob = syncScope.launch {
            runSync(force = true)
        }
    }

    fun syncOnLaunchIfNeeded() {
        syncScope.launch {
            AniListSettingsRepository.ensureLoaded()
            AniListAuthRepository.ensureLoaded()
            val settings = AniListSettingsRepository.uiState.value
            val isAuth = AniListAuthRepository.isAuthenticated.value
            if (isAuth && settings.enableSync && settings.syncOnLaunch) {
                log.i { "Sync on launch triggered" }
                runSync(force = false)
            }
        }
    }

    fun handlePlaybackProgressUpdated(entry: WatchProgressEntry) {
        syncScope.launch {
            AniListSettingsRepository.ensureLoaded()
            AniListAuthRepository.ensureLoaded()
            val settings = AniListSettingsRepository.uiState.value
            val isAuth = AniListAuthRepository.isAuthenticated.value
            if (!isAuth || !settings.enableSync || !settings.autoSync || !settings.syncWatching) {
                return@launch
            }

            // Sync watching progress in the background for series and movies
            if (entry.parentMetaType.equals("series", ignoreCase = true) || entry.parentMetaType.equals("anime", ignoreCase = true)) {
                val imdbId = entry.parentMetaId
                val season = entry.seasonNumber ?: 1
                val episode = entry.episodeNumber ?: 1
                val title = entry.title

                // Try resolving AniList ID using our resolution service
                val resolved = AniListResolutionService.resolveAniListId(
                    imdbId = imdbId,
                    seasonNumber = season,
                    episodeNumber = episode,
                    episodeReleaseDate = CurrentDateProviderHelper.getCurrentDateString(),
                    showTitle = title,
                    videoId = entry.videoId
                )

                if (resolved != null) {
                    val token = AniListAuthRepository.getAccessToken() ?: return@launch

                    // Guard: skip if not in library and auto-add is disabled
                    if (!settings.autoAddNewAnime &&
                        !AniListLibraryRepository.isInLibrary(resolved.anilistId)
                    ) return@launch

                    val libraryItem = (AniListLibraryRepository.uiState.value.watching +
                            AniListLibraryRepository.uiState.value.completed +
                            AniListLibraryRepository.uiState.value.planning +
                            AniListLibraryRepository.uiState.value.paused +
                            AniListLibraryRepository.uiState.value.dropped +
                            AniListLibraryRepository.uiState.value.rewatching)
                        .find { it.id == resolved.anilistId }

                    val targetProgress = if (entry.isCompleted) {
                        resolved.anilistEpisode
                    } else {
                        maxOf(libraryItem?.progress ?: 0, resolved.anilistEpisode - 1)
                    }

                    val totalEpisodes = libraryItem?.totalEpisodes
                    val isEntireShowCompleted = entry.isCompleted && totalEpisodes != null && totalEpisodes > 0 && targetProgress >= totalEpisodes
                    val targetStatus = if (isEntireShowCompleted) {
                        "COMPLETED"
                    } else {
                        if (libraryItem?.status.equals("REPEATING", ignoreCase = true)) "REPEATING" else "CURRENT"
                    }

                    // Check cache to avoid duplicate/redundant sync calls during active playback
                    val lastSynced = lastSyncedProgress[resolved.anilistId]
                    if (lastSynced != null && lastSynced.first == targetProgress && lastSynced.second == targetStatus) {
                        return@launch
                    }

                    // Check if already matches library state
                    if (libraryItem != null && libraryItem.progress == targetProgress && libraryItem.status.equals(targetStatus, ignoreCase = true)) {
                        return@launch
                    }

                    // Optimistically cache status to prevent duplicate updates
                    lastSyncedProgress[resolved.anilistId] = Pair(targetProgress, targetStatus)

                    log.i { "Auto Sync: Uploading progress to AniList for ${entry.title} (Episode $targetProgress, Status: $targetStatus)" }
                    val success = AniListApi.saveMediaListEntry(
                        token = token,
                        mediaId = resolved.anilistId,
                        status = targetStatus,
                        progress = targetProgress,
                        scoreRaw = 0
                    )
                    if (success) {
                        AniListLibraryRepository.refreshNow()
                    } else {
                        lastSyncedProgress.remove(resolved.anilistId)
                    }
                }
            }
        }
    }

    private suspend fun runSync(force: Boolean) {
        syncMutex.withLock {
            AniListSettingsRepository.ensureLoaded()
            AniListAuthRepository.ensureLoaded()
            val settings = AniListSettingsRepository.uiState.value
            val isAuth = AniListAuthRepository.isAuthenticated.value

            if (!isAuth || !settings.enableSync) {
                return
            }

            _isSyncing.value = true
            _syncMessage.value = "Synchronizing AniList..."

            try {
                // 1. Fetch fresh lists from AniList
                AniListLibraryRepository.refreshNow()
                val localEntries = WatchProgressRepository.uiState.value.entries
                val anilistItems = AniListLibraryRepository.uiState.value.watching +
                        AniListLibraryRepository.uiState.value.completed +
                        AniListLibraryRepository.uiState.value.planning +
                        AniListLibraryRepository.uiState.value.paused +
                        AniListLibraryRepository.uiState.value.dropped +
                        AniListLibraryRepository.uiState.value.rewatching

                val token = AniListAuthRepository.getAccessToken().orEmpty()

                ensureCacheLoaded()
                val lastSyncTimestampMs = settings.lastSyncTimestamp ?: 0L
                val lastSyncTimestampSec = lastSyncTimestampMs / 1000L
                val activeWatchingIds = AniListLibraryRepository.uiState.value.watching.map { it.id }.toSet() +
                        AniListLibraryRepository.uiState.value.rewatching.map { it.id }.toSet()

                val itemsToProcess = anilistItems.filter { item ->
                    val isUpdatedOnRemote = item.updatedAt > lastSyncTimestampSec
                    val isActive = item.id in activeWatchingIds
                    val cachedImdbId = anilistToImdbCache[item.id]
                    val hasLocalProgress = cachedImdbId != null && localEntries.any { it.parentMetaId == cachedImdbId }
                    isUpdatedOnRemote || isActive || hasLocalProgress
                }

                log.d { "Starting bidirectional sync. Local entries: ${localEntries.size}, total AniList: ${anilistItems.size}, process queue: ${itemsToProcess.size}" }

                // 2. Sync AniList -> Local
                var processedCount = 0
                for (item in itemsToProcess) {
                    processedCount++
                    if (itemsToProcess.size > 3) {
                        _syncMessage.value = "Synchronizing AniList ($processedCount/${itemsToProcess.size})..."
                    }
                    val imdbId = resolveImdbId(item.id) ?: continue
                    val localMatch = localEntries.find { it.parentMetaId == imdbId }

                    val anilistUpdatedAtMs = item.updatedAt * 1000L
                    val localUpdatedAtMs = localMatch?.lastUpdatedEpochMs ?: 0L

                    // Conflict resolution: latest updated wins
                    if (anilistUpdatedAtMs > localUpdatedAtMs) {
                        log.i { "Sync: AniList is newer for ${item.title}. Syncing to local." }
                        // Map status to local db
                        val isFinished = item.status.equals("COMPLETED", ignoreCase = true)
                        
                        if (isFinished) {
                            // Mark item as watched in local watched repository
                            val watched = WatchedItem(
                                id = imdbId,
                                type = "series",
                                name = item.title,
                                poster = item.posterUrl,
                                season = 1,
                                episode = item.progress,
                                markedAtEpochMs = anilistUpdatedAtMs
                            )
                            WatchedRepository.markWatchedFromPlaybackCompletion(watched, syncRemote = false)
                        }
                    } else if (localUpdatedAtMs > anilistUpdatedAtMs && localMatch != null) {
                        // Push local to AniList
                        log.i { "Sync: Local is newer for ${item.title}. Syncing to AniList." }
                        val totalEps = item.totalEpisodes
                        val targetProgress = if (localMatch.isCompleted) {
                            localMatch.episodeNumber ?: 1
                        } else {
                            maxOf(item.progress, (localMatch.episodeNumber ?: 1) - 1)
                        }
                        val isAllCompleted = localMatch.isCompleted && totalEps != null && totalEps > 0 && targetProgress >= totalEps
                        val status = if (isAllCompleted) {
                            "COMPLETED"
                        } else {
                            if (item.status.equals("REPEATING", ignoreCase = true)) "REPEATING" else "CURRENT"
                        }
                        AniListApi.saveMediaListEntry(
                            token = token,
                            mediaId = item.id,
                            status = status,
                            progress = targetProgress,
                            scoreRaw = item.score
                        )
                    }
                }

                // Update settings sync stamp
                AniListSettingsRepository.updateLastSyncTimestamp(WatchProgressClock.nowEpochMs())
                
                // Refresh library sections after sync completes to pull latest list states
                AniListLibraryRepository.refreshNow()

                _syncMessage.value = "Synchronization completed successfully."
            } catch (e: Exception) {
                log.e(e) { "Error during AniList sync" }
                _syncMessage.value = "Sync failed: ${e.message ?: "Unknown error"}"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    private var isCacheLoaded = false

    private fun ensureCacheLoaded() {
        if (isCacheLoaded) return
        isCacheLoaded = true
        try {
            val payload = AniListStorage.loadMappingCachePayload()
            if (!payload.isNullOrBlank()) {
                val map = json.decodeFromString<Map<String, String>>(payload)
                map.forEach { (k, v) ->
                    k.toIntOrNull()?.let { anilistId ->
                        anilistToImdbCache[anilistId] = v
                    }
                }
                log.d { "Loaded ${anilistToImdbCache.size} items from persistent AniList mapping cache." }
            }
        } catch (e: Exception) {
            log.w(e) { "Failed to load persistent AniList mapping cache" }
        }
    }

    private fun saveCacheToStorage() {
        try {
            val mapToSave = anilistToImdbCache.mapKeys { it.key.toString() }
            val payload = json.encodeToString(mapToSave)
            AniListStorage.saveMappingCachePayload(payload)
        } catch (e: Exception) {
            log.w(e) { "Failed to save persistent AniList mapping cache" }
        }
    }

    private suspend fun resolveImdbId(anilistId: Int): String? {
        ensureCacheLoaded()
        // Check cache first
        anilistToImdbCache[anilistId]?.let { return it }

        val imdbId = AniListResolutionService.resolveImdbId(anilistId)
        if (!imdbId.isNullOrBlank()) {
            anilistToImdbCache[anilistId] = imdbId
            saveCacheToStorage()
            return imdbId
        }
        return null
    }
}

// Simple Date Provider helper since datetime lib is not configured
object CurrentDateProviderHelper {
    fun getCurrentDateString(): String {
        // Format YYYY-MM-DD
        return "2026-07-10" // fallback to current task timestamp base
    }
}
