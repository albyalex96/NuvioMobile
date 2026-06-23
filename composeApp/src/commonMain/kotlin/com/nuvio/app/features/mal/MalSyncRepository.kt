package com.nuvio.app.features.mal

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object MalSyncRepository {
    private val log = Logger.withTag("MalSync")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun extractMalId(contentId: String): Long? {
        val normalized = contentId.trim()
        if (!normalized.startsWith("mal:", ignoreCase = true)) return null
        val numericPart = normalized.substringAfter("mal:", missingDelimiterValue = "")
            .substringBefore(':')
            .trim()
        return numericPart.toLongOrNull()
    }

    fun updateAnimeStatus(
        malId: Long,
        status: String? = null,
        numWatchedEpisodes: Int? = null,
        score: Int? = null,
        onSuccess: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null,
    ) {
        scope.launch {
            runCatching {
                MalAuthRepository.ensureLoaded()
                if (!MalAuthRepository.isAuthenticated.value) {
                    throw Exception("Not authenticated with MyAnimeList")
                }
                MalAuthRepository.refreshTokenIfNeeded(force = false)
                val token = MalAuthRepository.currentAccessToken()
                    ?: throw Exception("No access token available")

                MalApiClient.updateAnimeListStatus(
                    accessToken = token,
                    animeId = malId,
                    status = status,
                    score = score,
                    numWatchedEpisodes = numWatchedEpisodes,
                )
                MalLibraryRepository.refreshNow()
            }
                .onFailure { e ->
                    if (e is CancellationException) throw e
                    log.w(e) { "Failed to update MAL anime status" }
                    onError?.invoke(e.message ?: "Failed to update MAL status")
                }
                .onSuccess {
                    onSuccess?.invoke()
                }
        }
    }

    suspend fun resolveMalId(
        contentId: String,
        name: String,
        releaseInfo: String?,
        mediaType: String,
    ): Long? = MalIdResolver.resolve(contentId, name, releaseInfo, mediaType)

    fun syncWatchedEpisodeCount(
        contentId: String,
        currentEpisodeNumber: Int,
        name: String? = null,
        releaseInfo: String? = null,
        mediaType: String? = null,
    ) {
        scope.launch {
            val malId = if (name != null && mediaType != null) {
                resolveMalId(contentId, name, releaseInfo, mediaType)
            } else {
                extractMalId(contentId)
            } ?: return@launch
            MalAuthRepository.ensureLoaded()
            if (!MalAuthRepository.isAuthenticated.value) return@launch

            val libraryState = MalLibraryRepository.uiState.value
            val existingEntry = libraryState.allItems.firstOrNull { it.id == malId }
            val currentMalWatched = existingEntry?.episodesWatched ?: 0
            val totalEpisodes = existingEntry?.numEpisodes

            if (currentEpisodeNumber <= currentMalWatched) return@launch

            val newStatus = if (totalEpisodes != null && currentEpisodeNumber >= totalEpisodes) {
                "completed"
            } else if (existingEntry?.listStatus == "plan_to_watch" || existingEntry == null) {
                "watching"
            } else {
                existingEntry.listStatus
            }

            updateAnimeStatus(
                malId = malId,
                status = newStatus,
                numWatchedEpisodes = currentEpisodeNumber,
            )
        }
    }
}
