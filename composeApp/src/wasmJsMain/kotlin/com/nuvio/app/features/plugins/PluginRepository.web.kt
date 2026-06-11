package com.nuvio.app.features.plugins

import co.touchlab.kermit.Logger
import com.nuvio.app.features.addons.httpGetText
import com.nuvio.app.features.profiles.ProfileRepository
import com.nuvio.app.features.tmdb.TmdbService
import com.nuvio.app.features.plugins.runtime.PluginRuntime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.plugins_error_enter_repo_url
import nuvio.composeapp.generated.resources.plugins_error_enter_valid_url
import nuvio.composeapp.generated.resources.plugins_error_provider_not_found
import nuvio.composeapp.generated.resources.plugins_repository_already_installed
import nuvio.composeapp.generated.resources.plugins_repository_install_failed
import nuvio.composeapp.generated.resources.plugins_repository_refresh_failed
import org.jetbrains.compose.resources.getString

actual object PluginRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val log = Logger.withTag("PluginRepository")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val _uiState = MutableStateFlow(PluginsUiState())
    actual val uiState: StateFlow<PluginsUiState> = _uiState.asStateFlow()

    private var initialized = false
    private var currentProfileId = 1
    private val activeRefreshJobs = mutableMapOf<String, Job>()

    actual fun initialize() {
        ensureStateLoadedForProfile(currentProfileId)
    }

    actual fun onProfileChanged(profileId: Int) {
        if (profileId == currentProfileId && initialized) return
        cancelActiveRefreshes()
        currentProfileId = profileId
        initialized = false
        _uiState.value = PluginsUiState()
    }

    actual fun clearLocalState() {
        cancelActiveRefreshes()
        currentProfileId = 1
        initialized = false
        _uiState.value = PluginsUiState()
    }

    actual suspend fun pullFromServer(profileId: Int) {
        val effectiveProfileId = resolveEffectiveProfileId(profileId)
        ensureStateLoadedForProfile(effectiveProfileId)
    }

    actual suspend fun addRepository(rawUrl: String): AddPluginRepositoryResult {
        ensureStateLoadedForProfile(currentProfileId)
        val manifestUrl = try {
            normalizeManifestUrl(rawUrl)
        } catch (error: IllegalArgumentException) {
            return AddPluginRepositoryResult.Error(error.message ?: getString(Res.string.plugins_error_enter_valid_url))
        }

        if (_uiState.value.repositories.any { it.manifestUrl == manifestUrl }) {
            return AddPluginRepositoryResult.Error(getString(Res.string.plugins_repository_already_installed))
        }

        return try {
            val previousById = _uiState.value.scrapers.associateBy { it.id }
            val (repo, scrapers) = fetchRepositoryData(
                manifestUrl = manifestUrl,
                previousScrapers = previousById,
            )
            _uiState.update { state ->
                state.copy(
                    repositories = state.repositories + repo,
                    scrapers = state.scrapers.filterNot { it.repositoryUrl == manifestUrl } + scrapers,
                )
            }
            persist()
            AddPluginRepositoryResult.Success(repo)
        } catch (error: Throwable) {
            AddPluginRepositoryResult.Error(error.message ?: getString(Res.string.plugins_repository_install_failed))
        }
    }

    actual fun removeRepository(manifestUrl: String) {
        ensureStateLoadedForProfile(currentProfileId)
        _uiState.update { state ->
            state.copy(
                repositories = state.repositories.filterNot { it.manifestUrl == manifestUrl },
                scrapers = state.scrapers.filterNot { it.repositoryUrl == manifestUrl },
            )
        }
        persist()
    }

    actual fun refreshAll() {
        ensureStateLoadedForProfile(currentProfileId)
        _uiState.value.repositories.forEach { repo ->
            refreshRepositoryInternal(repo.manifestUrl, ensureInitialized = false)
        }
    }

    actual fun refreshRepository(manifestUrl: String, pushAfterRefresh: Boolean) {
        refreshRepositoryInternal(manifestUrl, ensureInitialized = true)
    }

    private fun refreshRepositoryInternal(manifestUrl: String, ensureInitialized: Boolean) {
        if (ensureInitialized) {
            ensureStateLoadedForProfile(currentProfileId)
        }
        val existingJob = activeRefreshJobs[manifestUrl]
        if (existingJob?.isActive == true) return

        markRefreshing(manifestUrl)
        var refreshJob: Job? = null
        refreshJob = scope.launch {
            try {
                val result = runCatching {
                    val previous = _uiState.value.scrapers.associateBy { it.id }
                    fetchRepositoryData(manifestUrl, previous)
                }

                val refreshFailed = getString(Res.string.plugins_repository_refresh_failed)
                _uiState.update { state ->
                    result.fold(
                        onSuccess = { (repo, scrapers) ->
                            val updatedRepos = state.repositories.map { existing ->
                                if (existing.manifestUrl == manifestUrl) repo else existing
                            }
                            state.copy(
                                repositories = updatedRepos,
                                scrapers = state.scrapers.filterNot { it.repositoryUrl == manifestUrl } + scrapers,
                            )
                        },
                        onFailure = { error ->
                            state.copy(
                                repositories = state.repositories.map { existing ->
                                    if (existing.manifestUrl == manifestUrl) {
                                        existing.copy(
                                            isRefreshing = false,
                                            errorMessage = error.message ?: refreshFailed,
                                        )
                                    } else {
                                        existing
                                    }
                                },
                            )
                        },
                    )
                }
                persist()
            } finally {
                if (activeRefreshJobs[manifestUrl] === refreshJob) {
                    activeRefreshJobs.remove(manifestUrl)
                }
            }
        }
        activeRefreshJobs[manifestUrl] = refreshJob
    }

    actual fun toggleScraper(scraperId: String, enabled: Boolean) {
        ensureStateLoadedForProfile(currentProfileId)
        _uiState.update { state ->
            state.copy(
                scrapers = state.scrapers.map { scraper ->
                    if (scraper.id == scraperId) {
                        scraper.copy(enabled = if (scraper.manifestEnabled) enabled else false)
                    } else {
                        scraper
                    }
                },
            )
        }
        persist()
    }

    actual fun setPluginsEnabled(enabled: Boolean) {
        ensureStateLoadedForProfile(currentProfileId)
        _uiState.update { it.copy(pluginsEnabled = enabled) }
        persist()
    }

    actual fun setGroupStreamsByRepository(enabled: Boolean) {
        ensureStateLoadedForProfile(currentProfileId)
        _uiState.update { it.copy(groupStreamsByRepository = enabled) }
        persist()
    }

    actual fun getEnabledScrapersForType(type: String): List<PluginScraper> {
        ensureStateLoadedForProfile(currentProfileId)
        if (!_uiState.value.pluginsEnabled) return emptyList()
        return _uiState.value.scrapers.filter { scraper ->
            scraper.enabled && scraper.supportsType(type)
        }
    }

    actual suspend fun testScraper(scraperId: String): Result<List<PluginRuntimeResult>> {
        ensureStateLoadedForProfile(currentProfileId)
        val scraper = _uiState.value.scrapers.find { it.id == scraperId }
            ?: return Result.failure(IllegalArgumentException(getString(Res.string.plugins_error_provider_not_found)))

        val mediaType = if (scraper.supportsType("movie")) "movie" else "tv"
        val season = if (mediaType == "tv") 1 else null
        val episode = if (mediaType == "tv") 1 else null
        return executeScraper(
            scraper = scraper,
            tmdbId = "603",
            mediaType = mediaType,
            season = season,
            episode = episode,
        )
    }

    actual suspend fun executeScraper(
        scraper: PluginScraper,
        tmdbId: String,
        mediaType: String,
        season: Int?,
        episode: Int?,
    ): Result<List<PluginRuntimeResult>> {
        val resolvedTmdbId = resolvePluginTmdbId(
            tmdbId = tmdbId,
            mediaType = mediaType,
        )

        return runCatching {
            PluginRuntime.executePlugin(
                code = scraper.code,
                tmdbId = resolvedTmdbId,
                mediaType = normalizePluginType(mediaType),
                season = season,
                episode = episode,
                scraperId = scraper.id,
            )
        }
    }

    private suspend fun resolvePluginTmdbId(tmdbId: String, mediaType: String): String {
        val trimmed = tmdbId.trim()
        if (trimmed.isBlank()) return tmdbId
        return TmdbService.ensureTmdbId(
            videoId = trimmed,
            mediaType = mediaType,
        ) ?: trimmed
    }

    private suspend fun fetchRepositoryData(
        manifestUrl: String,
        previousScrapers: Map<String, PluginScraper>,
    ): Pair<PluginRepositoryItem, List<PluginScraper>> = withContext(Dispatchers.Default) {
        val payload = httpGetText(manifestUrl)
        val manifest = PluginManifestParser.parse(payload)
        val baseUrl = manifestUrl.substringBefore("?").removeSuffix("/manifest.json")

        val scrapers = manifest.scrapers
            .filter { scraper -> scraper.isSupportedOnCurrentPlatform() }
            .mapNotNull { info ->
                val codeUrl = if (info.filename.startsWith("http://") || info.filename.startsWith("https://")) {
                    info.filename
                } else {
                    "$baseUrl/${info.filename.trimStart('/')}"
                }
                runCatching {
                    val code = httpGetText(codeUrl)
                    val scraperId = "${manifestUrl.lowercase()}:${info.id}"
                    val previous = previousScrapers[scraperId]
                    val enabled = when {
                        !info.enabled -> false
                        previous != null -> previous.enabled
                        else -> info.enabled
                    }

                    PluginScraper(
                        id = scraperId,
                        repositoryUrl = manifestUrl,
                        name = info.name,
                        description = info.description.orEmpty(),
                        version = info.version,
                        filename = info.filename,
                        supportedTypes = info.supportedTypes,
                        enabled = enabled,
                        manifestEnabled = info.enabled,
                        hasSettings = info.hasSettings,
                        logo = info.logo,
                        contentLanguage = info.contentLanguage ?: emptyList(),
                        formats = info.formats ?: info.supportedFormats,
                        code = code,
                    )
                }.getOrNull()
            }

        val repo = PluginRepositoryItem(
            manifestUrl = manifestUrl,
            name = manifest.name,
            description = manifest.description,
            version = manifest.version,
            scraperCount = scrapers.size,
            lastUpdated = currentEpochMillis(),
            isRefreshing = false,
            errorMessage = null,
        )
        repo to scrapers
    }

    private fun PluginManifestScraper.isSupportedOnCurrentPlatform(): Boolean {
        val platform = currentPluginPlatform().lowercase()
        val supported = supportedPlatforms?.map { it.lowercase() }?.toSet().orEmpty()
        val disabled = disabledPlatforms?.map { it.lowercase() }?.toSet().orEmpty()
        if (supported.isNotEmpty() && platform !in supported) return false
        if (platform in disabled) return false
        return true
    }

    private fun markRefreshing(manifestUrl: String) {
        _uiState.update { state ->
            state.copy(
                repositories = state.repositories.map { repo ->
                    if (repo.manifestUrl == manifestUrl) {
                        repo.copy(isRefreshing = true, errorMessage = null)
                    } else {
                        repo
                    }
                },
            )
        }
    }

    private fun persist() {
        val state = _uiState.value
        val payload = StoredPluginsState(
            pluginsEnabled = state.pluginsEnabled,
            groupStreamsByRepository = state.groupStreamsByRepository,
            repositories = state.repositories.map { repo ->
                StoredPluginRepository(
                    manifestUrl = repo.manifestUrl,
                    name = repo.name,
                    description = repo.description,
                    version = repo.version,
                    scraperCount = repo.scraperCount,
                    lastUpdated = repo.lastUpdated,
                )
            },
            scrapers = state.scrapers.map { scraper ->
                StoredPluginScraper(
                    id = scraper.id,
                    repositoryUrl = scraper.repositoryUrl,
                    name = scraper.name,
                    description = scraper.description,
                    version = scraper.version,
                    filename = scraper.filename,
                    supportedTypes = scraper.supportedTypes,
                    enabled = scraper.enabled,
                    manifestEnabled = scraper.manifestEnabled,
                    hasSettings = scraper.hasSettings,
                    logo = scraper.logo,
                    contentLanguage = scraper.contentLanguage,
                    formats = scraper.formats,
                    code = scraper.code,
                )
            },
        )
        PluginStorage.saveState(currentProfileId, json.encodeToString(payload))
    }

    private fun loadStoredState(profileId: Int): StoredPluginsState? {
        val raw = PluginStorage.loadState(profileId)?.trim().orEmpty()
        if (raw.isBlank()) return null
        return runCatching {
            json.decodeFromString<StoredPluginsState>(raw)
        }.getOrNull()
    }

    private fun cancelActiveRefreshes() {
        activeRefreshJobs.values.forEach(Job::cancel)
        activeRefreshJobs.clear()
    }

    private fun ensureStateLoadedForProfile(profileId: Int) {
        if (initialized && currentProfileId == profileId) return
        if (currentProfileId != profileId) {
            cancelActiveRefreshes()
        }
        currentProfileId = profileId
        _uiState.value = loadStateAsUiState(profileId)
        initialized = true
    }

    private fun loadStateAsUiState(profileId: Int): PluginsUiState {
        val stored = loadStoredState(profileId)
        return PluginsUiState(
            pluginsEnabled = stored?.pluginsEnabled ?: true,
            groupStreamsByRepository = stored?.groupStreamsByRepository ?: false,
            repositories = stored?.repositories
                ?.map {
                    PluginRepositoryItem(
                        manifestUrl = it.manifestUrl,
                        name = it.name,
                        description = it.description,
                        version = it.version,
                        scraperCount = it.scraperCount,
                        lastUpdated = it.lastUpdated,
                        isRefreshing = false,
                        errorMessage = null,
                    )
                }
                ?: emptyList(),
            scrapers = stored?.scrapers
                ?.map {
                    PluginScraper(
                        id = it.id,
                        repositoryUrl = it.repositoryUrl,
                        name = it.name,
                        description = it.description,
                        version = it.version,
                        filename = it.filename,
                        supportedTypes = it.supportedTypes,
                        enabled = it.enabled,
                        manifestEnabled = it.manifestEnabled,
                        hasSettings = it.hasSettings,
                        logo = it.logo,
                        contentLanguage = it.contentLanguage,
                        formats = it.formats,
                        code = it.code,
                    )
                }
                ?: emptyList(),
        )
    }

    private suspend fun normalizeManifestUrl(rawUrl: String): String {
        val trimmed = rawUrl.trim()
        val msg = getString(Res.string.plugins_error_enter_repo_url)
        require(trimmed.isNotEmpty()) { msg }

        val normalizedScheme = when {
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
            else -> "https://$trimmed"
        }

        val withoutFragment = normalizedScheme.substringBefore("#")
        val query = withoutFragment.substringAfter("?", "")
        val path = withoutFragment.substringBefore("?").trimEnd('/')
        val manifestPath = if (path.endsWith("/manifest.json")) path else "$path/manifest.json"
        return if (query.isEmpty()) manifestPath else "$manifestPath?$query"
    }

    private fun resolveEffectiveProfileId(profileId: Int): Int {
        val active = ProfileRepository.state.value.activeProfile
        return if (active != null && !active.id.isBlank() && active.usesPrimaryPlugins) 1 else profileId
    }
}
