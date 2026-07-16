package com.nuvio.app.features.streams

import co.touchlab.kermit.Logger
import com.nuvio.app.core.build.AppFeaturePolicy
import com.nuvio.app.features.addons.AddonRepository
import com.nuvio.app.features.addons.buildAddonResourceUrl
import com.nuvio.app.features.addons.enabledAddons
import com.nuvio.app.features.addons.httpGetText
import com.nuvio.app.features.debrid.DirectDebridStreamPreparer
import com.nuvio.app.features.debrid.DebridSettingsRepository
import com.nuvio.app.features.debrid.DebridStreamPresentation
import com.nuvio.app.features.debrid.LocalDebridAvailabilityService
import com.nuvio.app.features.details.MetaDetailsRepository
import com.nuvio.app.features.player.PlayerSettingsRepository
import com.nuvio.app.features.cloudstream.CloudStreamRepository
import com.nuvio.app.features.cloudstream.parseCloudStreamRouteId
import com.nuvio.app.features.cloudstream.toStreamItem
import com.nuvio.app.features.plugins.PluginRepository
import com.nuvio.app.features.plugins.pluginContentId
import com.nuvio.app.features.plugins.PluginsUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.getString
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

object StreamsRepository {
    private val log = Logger.withTag("StreamsRepo")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _uiState = MutableStateFlow(StreamsUiState())
    val uiState: StateFlow<StreamsUiState> = _uiState.asStateFlow()

    private var activeJob: Job? = null
    private var activeRequestKey: String? = null

    fun requestToken(
        type: String,
        videoId: String,
        season: Int? = null,
        episode: Int? = null,
        manualSelection: Boolean = false,
    ): String =
        "$type::$videoId::$season::$episode::$manualSelection"

    fun load(type: String, videoId: String, parentMetaId: String? = null, season: Int? = null, episode: Int? = null, manualSelection: Boolean = false) {
        load(
            type = type,
            videoId = videoId,
            parentMetaId = parentMetaId,
            season = season,
            episode = episode,
            manualSelection = manualSelection,
            forceRefresh = false,
        )
    }

    fun reload(type: String, videoId: String, parentMetaId: String? = null, season: Int? = null, episode: Int? = null, manualSelection: Boolean = false) {
        load(
            type = type,
            videoId = videoId,
            parentMetaId = parentMetaId,
            season = season,
            episode = episode,
            manualSelection = manualSelection,
            forceRefresh = true,
        )
    }

    private fun load(type: String, videoId: String, parentMetaId: String?, season: Int?, episode: Int?, manualSelection: Boolean, forceRefresh: Boolean) {
        val pluginUiState = if (AppFeaturePolicy.pluginsEnabled) {
            PluginRepository.initialize()
            PluginRepository.uiState.value
        } else {
            PluginsUiState(pluginsEnabled = false)
        }
        val cloudStreamSearchRequest = buildCloudStreamSearchRequest(
            type = type,
            videoId = videoId,
            parentMetaId = parentMetaId,
            parentMetaType = parentMetaId?.let { type },
            season = season,
            episode = episode,
            searchTitle = null,
        )
        val cloudStreamProviderGroups = if (AppFeaturePolicy.pluginsEnabled) {
            cloudStreamProviderGroupsForRequest(type, cloudStreamSearchRequest)
        } else {
            emptyList()
        }
        val cloudStreamRegistryRevision = if (AppFeaturePolicy.pluginsEnabled) {
            CloudStreamRepository.uiState.value.registryRevision
        } else {
            0L
        }
        val requestToken = requestToken(
            type = type,
            videoId = videoId,
            season = season,
            episode = episode,
            manualSelection = manualSelection,
        )
        val requestKey = "$requestToken::pluginsGrouped=${pluginUiState.groupStreamsByRepository}" +
            "::cloudstream=$cloudStreamRegistryRevision" +
            "::cloudTarget=${cloudStreamSearchRequest?.cacheKey.orEmpty()}"
        val currentState = _uiState.value
        if (
            !forceRefresh &&
            activeRequestKey == requestKey &&
            (currentState.groups.isNotEmpty() || currentState.emptyStateReason != null || currentState.isAnyLoading)
        ) {
            log.d { "Skipping stream reload for unchanged request type=$type id=$videoId" }
            return
        }

        val cloudStreamRoute = parseCloudStreamRouteId(videoId)
        if (cloudStreamRoute != null) {
            CloudStreamRepository.initialize()
            val providerItem = CloudStreamRepository.uiState.value.plugins
                .firstOrNull { it.metadata.id.value == cloudStreamRoute.providerId }
            val providerName = providerItem?.metadata?.name ?: "CloudStream"
            val providerAddonId = cloudStreamAddonId(cloudStreamRoute.providerId)
            _uiState.value = StreamsUiState(
                requestToken = requestToken,
                groups = listOf(
                    AddonStreamGroup(
                        addonName = providerName,
                        addonId = providerAddonId,
                        streams = emptyList(),
                        isLoading = true,
                    ),
                ),
                activeAddonIds = setOf(providerAddonId),
                isAnyLoading = true,
            )
            activeJob = scope.launch {
                CloudStreamRepository.loadLinks(cloudStreamRoute.providerId, cloudStreamRoute.data)
                    .fold(
                        onSuccess = { sources ->
                            val streams = cloudStreamSourcesToStreamItems(
                                providerId = cloudStreamRoute.providerId,
                                providerName = providerName,
                                sources = sources,
                            )
                            _uiState.value = StreamsUiState(
                                requestToken = requestToken,
                                groups = listOf(
                                    AddonStreamGroup(
                                        addonName = providerName,
                                        addonId = providerAddonId,
                                        streams = streams,
                                        isLoading = false,
                                        error = if (streams.isEmpty()) "No links found" else null,
                                    ),
                                ),
                                activeAddonIds = setOf(providerAddonId),
                                isAnyLoading = false,
                                emptyStateReason = if (streams.isEmpty()) StreamsEmptyStateReason.NoStreamsFound else null,
                            )
                        },
                        onFailure = { error ->
                            log.w(error) { "CloudStream link resolution failed provider=${cloudStreamRoute.providerId}" }
                            _uiState.value = StreamsUiState(
                                requestToken = requestToken,
                                groups = listOf(
                                    AddonStreamGroup(
                                        addonName = providerName,
                                        addonId = providerAddonId,
                                        streams = emptyList(),
                                        isLoading = false,
                                        error = error.message ?: "CloudStream link resolution failed",
                                    ),
                                ),
                                activeAddonIds = setOf(providerAddonId),
                                isAnyLoading = false,
                                emptyStateReason = StreamsEmptyStateReason.StreamFetchFailed,
                            )
                        },
                    )
            }
            return
        }

        activeRequestKey = requestKey
        activeJob?.cancel()
        _uiState.value = StreamsUiState(requestToken = requestToken)

        PlayerSettingsRepository.ensureLoaded()
        val playerSettings = PlayerSettingsRepository.uiState.value
        val debridSettings = DebridSettingsRepository.snapshot()
        val streamBadgeRules = StreamBadgeSettingsRepository.snapshot()
        val autoPlayMode = playerSettings.streamAutoPlayMode
        val isAutoPlayEnabled = !manualSelection && autoPlayMode != StreamAutoPlayMode.MANUAL &&
            !(autoPlayMode == StreamAutoPlayMode.REGEX_MATCH &&
                !StreamAutoPlayPolicy.isRegexSelectionConfigured(playerSettings.streamAutoPlayRegex))

        // Look up persisted binge group when both settings are enabled
        val persistedBingeGroup = if (
            playerSettings.streamAutoPlayPreferBingeGroup &&
            playerSettings.streamAutoPlayReuseBingeGroup
        ) {
            parentMetaId?.let { BingeGroupCacheRepository.get(it) }
        } else null

        // Enable direct auto-play flow if normal auto-play is enabled,
        // OR if we have a persisted binge group in MANUAL mode
        val bingeGroupDirectFlow = !manualSelection &&
            persistedBingeGroup != null &&
            autoPlayMode == StreamAutoPlayMode.MANUAL
        val isDirectAutoPlayFlow = isAutoPlayEnabled || bingeGroupDirectFlow

        if (isDirectAutoPlayFlow) {
            _uiState.value = StreamsUiState(
                requestToken = requestToken,
                isDirectAutoPlayFlow = true,
                showDirectAutoPlayOverlay = true,
            )
        }

        val embeddedStreams = MetaDetailsRepository.findEmbeddedStreams(videoId)
        if (embeddedStreams.isNotEmpty()) {
            log.d { "Using ${embeddedStreams.size} embedded streams for type=$type id=$videoId" }
            val group = AddonStreamGroup(
                addonName = embeddedStreams.first().addonName,
                addonId = "embedded",
                streams = embeddedStreams,
                isLoading = false,
            )
            val presentedGroup = StreamBadgePresentation.apply(
                groups = listOf(group),
                rules = streamBadgeRules,
            ).firstOrNull() ?: group
            _uiState.value = StreamsUiState(
                requestToken = requestToken,
                groups = listOf(presentedGroup),
                activeAddonIds = setOf("embedded"),
                isAnyLoading = false,
            )
            return
        }

        val installedAddons = AddonRepository.uiState.value.addons.enabledAddons()
        val pluginScrapers = if (AppFeaturePolicy.pluginsEnabled) {
            PluginRepository.getEnabledScrapersForType(type).also {
                log.i { "getEnabledScrapersForType($type) returned ${it.size} scrapers" }
            }
        } else {
            log.i { "Plugins disabled by AppFeaturePolicy" }
            emptyList()
        }
        val pluginProviderGroups = pluginScrapers.toPluginProviderGroups(
            repositories = pluginUiState.repositories,
            groupByRepository = pluginUiState.groupStreamsByRepository,
        )

        if (installedAddons.isEmpty() && pluginProviderGroups.isEmpty() && cloudStreamProviderGroups.isEmpty()) {
            _uiState.value = StreamsUiState(
                requestToken = requestToken,
                isAnyLoading = false,
                emptyStateReason = StreamsEmptyStateReason.NoAddonsInstalled,
            )
            return
        }

        val streamAddons = installedAddons
            .mapNotNull { addon ->
                val manifest = addon.manifest ?: return@mapNotNull null
                val supportsRequestedStream = manifest.resources.any { resource ->
                    resource.name == "stream" &&
                        resource.types.contains(type) &&
                        (resource.idPrefixes.isEmpty() ||
                            resource.idPrefixes.any { videoId.startsWith(it) })
                }
                if (!supportsRequestedStream) return@mapNotNull null

                InstalledStreamAddonTarget(
                    addonName = addon.displayTitle.ifBlank { manifest.name },
                    addonId = addon.streamAddonInstanceId(manifest.id),
                    manifest = manifest,
                )
            }

        log.d { "Found ${streamAddons.size} addons and ${cloudStreamProviderGroups.size} compatible CloudStream providers for stream type=$type id=$videoId" }

        if (streamAddons.isEmpty() && pluginProviderGroups.isEmpty() && cloudStreamProviderGroups.isEmpty()) {
            _uiState.value = StreamsUiState(
                requestToken = requestToken,
                isAnyLoading = false,
                emptyStateReason = StreamsEmptyStateReason.NoCompatibleAddons,
            )
            return
        }

        // Initialise loading placeholders
        val installedAddonOrder = streamAddons.map { it.addonName }
        val initialGroups = StreamAutoPlaySelector.orderAddonStreams(streamAddons.map { addon ->
            AddonStreamGroup(
                addonName = addon.addonName,
                addonId = addon.addonId,
                streams = emptyList(),
                isLoading = true,
            )
        } + pluginProviderGroups.map { providerGroup ->
            AddonStreamGroup(
                addonName = providerGroup.addonName,
                addonId = providerGroup.addonId,
                streams = emptyList(),
                isLoading = true,
            )
        } + cloudStreamProviderGroups.map { providerGroup ->
            AddonStreamGroup(
                addonName = providerGroup.addonName,
                addonId = providerGroup.addonId,
                streams = emptyList(),
                isLoading = true,
            )
        }, installedAddonOrder)
        val isInitiallyLoading = initialGroups.any { it.isLoading }
        _uiState.value = StreamsUiState(
            requestToken = requestToken,
            groups = initialGroups,
            activeAddonIds = initialGroups.map { it.addonId }.toSet(),
            isAnyLoading = isInitiallyLoading,
            emptyStateReason = null,
            isDirectAutoPlayFlow = isDirectAutoPlayFlow,
            showDirectAutoPlayOverlay = isDirectAutoPlayFlow,
        )

        activeJob = scope.launch {
            val completions = Channel<StreamLoadCompletion>(capacity = Channel.UNLIMITED)
            val pluginRemainingByAddonId = pluginProviderGroups
                .associate { it.addonId to it.scrapers.size }
                .toMutableMap()
            val pluginFirstErrorByAddonId = mutableMapOf<String, String>()
            val totalTasks = streamAddons.size +
                pluginProviderGroups.sumOf { it.scrapers.size } +
                cloudStreamProviderGroups.size

            val installedAddonNames = installedAddonOrder.toSet()
            val installedAddonIds = streamAddons.map { it.addonId }.toSet()
            val eligibleForDebridIds = installedAddonIds + pluginProviderGroups.map { it.addonId } + cloudStreamProviderGroups.map { it.addonId }
            val debridAvailabilityJobs = mutableListOf<Job>()
            var autoSelectTriggered = false
            var timeoutElapsed = false
            fun publishCompletion(completion: StreamLoadCompletion) {
                if (completions.trySend(completion).isFailure) {
                    log.w { "Stream completion dropped! Channel capacity exhausted or closed." }
                }
            }
            fun presentStreamGroup(group: AddonStreamGroup): AddonStreamGroup {
                val badgeGroup = StreamBadgePresentation.apply(
                    groups = listOf(group),
                    rules = streamBadgeRules,
                ).firstOrNull() ?: group
                return DebridStreamPresentation.apply(
                    groups = listOf(badgeGroup),
                    settings = debridSettings,
                ).firstOrNull() ?: badgeGroup
            }

            fun publishAddonGroup(group: AddonStreamGroup) {
                _uiState.update { current ->
                    val updated = StreamAutoPlaySelector.orderAddonStreams(
                        groups = current.groups.map { currentGroup ->
                            if (currentGroup.addonId == group.addonId) group else currentGroup
                        },
                        installedOrder = installedAddonOrder,
                    )
                    val anyLoading = updated.any { it.isLoading }
                    current.copy(
                        groups = updated,
                        isAnyLoading = anyLoading,
                        emptyStateReason = updated.toEmptyStateReason(anyLoading),
                    )
                }
            }

            fun publishAddonGroupAfterCacheCheck(group: AddonStreamGroup) {
                val isEligible = group.addonId in installedAddonIds ||
                                group.addonId.startsWith("plugin:") ||
                                group.addonId.startsWith("plugin-repo:")
                if (!isEligible || group.streams.isEmpty()) {
                    publishAddonGroup(presentStreamGroup(group))
                return
                }

                val eligibleGroupIds = setOf(group.addonId)
                val shouldWaitForCacheCheck = LocalDebridAvailabilityService.hasPendingCacheCheck(
                    groups = listOf(group),
                    eligibleGroupIds = eligibleGroupIds,
                )
                if (!shouldWaitForCacheCheck) {
                    publishAddonGroup(presentStreamGroup(group))
                    return
                }

                val checkingGroup = LocalDebridAvailabilityService.markChecking(
                    groups = listOf(group),
                    eligibleGroupIds = eligibleGroupIds,
                ).firstOrNull() ?: group

                val availabilityJob = launch {
                    val availabilityGroup = LocalDebridAvailabilityService.annotateCachedAvailability(
                        groups = listOf(checkingGroup),
                        eligibleGroupIds = eligibleGroupIds,
                    ).firstOrNull() ?: checkingGroup
                    publishAddonGroup(presentStreamGroup(availabilityGroup))

                    // Early binge-group match right after this addon's availability is resolved
                    if (isDirectAutoPlayFlow && !autoSelectTriggered && persistedBingeGroup != null && !timeoutElapsed) {
                        val allStreams = _uiState.value.groups.flatMap { it.streams }
                        if (allStreams.isNotEmpty()) {
                            val earlyMatch = StreamAutoPlaySelector.selectAutoPlayStream(
                                streams = allStreams,
                                mode = autoPlayMode,
                                regexPattern = playerSettings.streamAutoPlayRegex,
                                source = playerSettings.streamAutoPlaySource,
                                installedAddonNames = installedAddonNames,
                                selectedAddons = playerSettings.streamAutoPlaySelectedAddons,
                                selectedPlugins = playerSettings.streamAutoPlaySelectedPlugins,
                                preferredBingeGroup = persistedBingeGroup,
                                preferBingeGroupInSelection = true,
                                bingeGroupOnly = true,
                                debridEnabled = debridSettings.canResolvePlayableLinks,
                                activeResolverProviderId = debridSettings.activeResolverProviderId,
                            )
                            if (earlyMatch != null) {
                                autoSelectTriggered = true
                                _uiState.update { it.copy(autoPlayStream = earlyMatch) }
                            }
                        }
                    }
                }
                debridAvailabilityJobs += availabilityJob
            }

            val timeoutJob = if (isDirectAutoPlayFlow) {
                val timeoutSeconds = playerSettings.streamAutoPlayTimeoutSeconds
                val isUnlimitedTimeout = timeoutSeconds == Int.MAX_VALUE
                // Timeout semantics:
                // - 0 (instant): timeoutElapsed immediately, full select on each response
                // - 1-30 (bounded): wait the configured delay, then full select
                // - unlimited (Int.MAX_VALUE): timeoutElapsed immediately, full select on each response,
                //   with 60s hard fallback to stream picker
                if (timeoutSeconds <= 0 || isUnlimitedTimeout) {
                    timeoutElapsed = true
                    // For unlimited: launch a hard 60s fallback to dismiss overlay
                    if (isUnlimitedTimeout) {
                        launch {
                            delay(60_000L)
                            if (!autoSelectTriggered) {
                                autoSelectTriggered = true
                                val allStreams = _uiState.value.groups.flatMap { it.streams }
                                if (allStreams.isNotEmpty()) {
                                    val selected = StreamAutoPlaySelector.selectAutoPlayStream(
                                        streams = allStreams,
                                        mode = autoPlayMode,
                                        regexPattern = playerSettings.streamAutoPlayRegex,
                                        source = playerSettings.streamAutoPlaySource,
                                        installedAddonNames = installedAddonNames,
                                        selectedAddons = playerSettings.streamAutoPlaySelectedAddons,
                                        selectedPlugins = playerSettings.streamAutoPlaySelectedPlugins,
                                        preferredBingeGroup = persistedBingeGroup,
                                        preferBingeGroupInSelection = persistedBingeGroup != null,
                                        bingeGroupOnly = false,
                                        debridEnabled = debridSettings.canResolvePlayableLinks,
                                        activeResolverProviderId = debridSettings.activeResolverProviderId,
                                    )
                                    _uiState.update { it.copy(autoPlayStream = selected) }
                                }
                                if (_uiState.value.autoPlayStream == null) {
                                    _uiState.update {
                                        it.copy(
                                            isDirectAutoPlayFlow = false,
                                            showDirectAutoPlayOverlay = false,
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        null
                    }
                } else {
                    // Bounded timeout (1-30s)
                    launch {
                        delay(timeoutSeconds * 1_000L)
                        timeoutElapsed = true
                        if (!autoSelectTriggered) {
                            val allStreams = _uiState.value.groups.flatMap { it.streams }
                            if (allStreams.isNotEmpty()) {
                                val evaluation = StreamAutoPlaySelector.evaluateAutoPlayStream(
                                    streams = allStreams,
                                    mode = autoPlayMode,
                                    regexPattern = playerSettings.streamAutoPlayRegex,
                                    source = playerSettings.streamAutoPlaySource,
                                    installedAddonNames = installedAddonNames,
                                    selectedAddons = playerSettings.streamAutoPlaySelectedAddons,
                                    selectedPlugins = playerSettings.streamAutoPlaySelectedPlugins,
                                    preferredBingeGroup = persistedBingeGroup,
                                    preferBingeGroupInSelection = persistedBingeGroup != null,
                                    bingeGroupOnly = false,
                                    debridEnabled = debridSettings.canResolvePlayableLinks,
                                    activeResolverProviderId = debridSettings.activeResolverProviderId,
                                )
                                if (evaluation.stream != null || !evaluation.hasPendingDebridCandidate) {
                                    autoSelectTriggered = true
                                    _uiState.update {
                                        it.copy(
                                            autoPlayStream = evaluation.stream,
                                            autoPlayCandidates = evaluation.readyStreams,
                                        )
                                    }
                                }
                                if (evaluation.stream == null && !evaluation.hasPendingDebridCandidate) {
                                    _uiState.update {
                                        it.copy(
                                            isDirectAutoPlayFlow = false,
                                            showDirectAutoPlayOverlay = false,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                null
            }

            streamAddons.forEach { addon ->
                launch {
                    val url = buildAddonResourceUrl(
                        manifestUrl = addon.manifest.transportUrl,
                        resource = "stream",
                        type = type,
                        id = videoId,
                    )
                    log.d { "Fetching streams from: $url" }

                    val displayName = addon.addonName
                    val group = runCatchingUnlessCancelled {
                        val payload = httpGetText(url)
                        StreamParser.parse(
                            payload = payload,
                            addonName = displayName,
                            addonId = addon.addonId,
                            addonLogo = addon.manifest.logoUrl,
                        )
                    }.fold(
                        onSuccess = { streams ->
                            log.d { "Got ${streams.size} streams from ${displayName}" }
                            AddonStreamGroup(
                                addonName = displayName,
                                addonId = addon.addonId,
                                streams = streams,
                                isLoading = false,
                            )
                        },
                        onFailure = { err ->
                            log.w(err) { "Failed to fetch streams from ${displayName}" }
                            AddonStreamGroup(
                                addonName = displayName,
                                addonId = addon.addonId,
                                streams = emptyList(),
                                isLoading = false,
                                error = err.message,
                            )
                        },
                    )
                    publishCompletion(StreamLoadCompletion.Addon(group))
                }
            }

            log.i { "Plugin provider groups: ${pluginProviderGroups.size} groups, total scrapers: ${pluginProviderGroups.sumOf { it.scrapers.size }}" }
            pluginProviderGroups.forEach { providerGroup ->
                val includeScraperNameInSubtitle = false
                providerGroup.scrapers.forEach { scraper ->
                    log.i { "Launching plugin scraper: ${scraper.id} (group=${providerGroup.addonId})" }
                    launch {
                        val completion = PluginRepository.executeScraper(
                            scraper = scraper,
                            tmdbId = pluginContentId(
                                videoId = videoId,
                                season = season,
                                episode = episode,
                            ),
                            mediaType = type,
                            season = season,
                            episode = episode,
                        ).fold(
                            onSuccess = { results ->
                                val streamItems = results.map { result ->
                                    result.toStreamItem(
                                        scraper = scraper,
                                        addonName = providerGroup.addonName,
                                        addonId = providerGroup.addonId,
                                        includeScraperNameInSubtitle = includeScraperNameInSubtitle,
                                    )
                                }
                                log.i { "executeScraper OK: ${results.size} results -> ${streamItems.size} streamItems for ${providerGroup.addonId}" }
                                StreamLoadCompletion.PluginScraper(
                                    addonId = providerGroup.addonId,
                                    streams = streamItems,
                                    error = null,
                                )
                            },
                            onFailure = { error ->
                                StreamLoadCompletion.PluginScraper(
                                    addonId = providerGroup.addonId,
                                    streams = emptyList(),
                                    error = error.message ?: getString(Res.string.streams_failed_to_load_scraper, scraper.name),
                                )
                            },
                        )
                        publishCompletion(completion)
                    }
                }
            }

            val cloudStreamSemaphore = Semaphore(CLOUDSTREAM_STREAM_PROVIDER_CONCURRENCY)
            cloudStreamProviderGroups.forEach { providerGroup ->
                launch {
                    val group = withTimeoutOrNull(STREAM_PROVIDER_TIMEOUT_MS) {
                        cloudStreamSemaphore.withPermit {
                            resolveCloudStreamProviderStreams(
                                providerGroup = providerGroup,
                                request = cloudStreamSearchRequest,
                            )
                        }
                    } ?: AddonStreamGroup(
                        addonName = providerGroup.addonName,
                        addonId = providerGroup.addonId,
                        streams = emptyList(),
                        isLoading = false,
                        error = "${providerGroup.addonName} timed out",
                    )
                    publishCompletion(StreamLoadCompletion.Addon(group))
                }
            }

            repeat(totalTasks) {
                when (val completion = completions.receive()) {
                    is StreamLoadCompletion.Addon -> {
                        val result = completion.group
                        publishAddonGroupAfterCacheCheck(result)
                    }

                    is StreamLoadCompletion.PluginScraper -> {
                        val remaining = (pluginRemainingByAddonId[completion.addonId] ?: 1) - 1
                        pluginRemainingByAddonId[completion.addonId] = remaining.coerceAtLeast(0)
                        log.i { "PluginScraper completion for addonId=${completion.addonId}: ${completion.streams.size} streams, error=${completion.error}" }
                        if (!completion.error.isNullOrBlank() && pluginFirstErrorByAddonId[completion.addonId].isNullOrBlank()) {
                            pluginFirstErrorByAddonId[completion.addonId] = completion.error
                        }

                        _uiState.update { current ->
                            val updated = StreamAutoPlaySelector.orderAddonStreams(
                                groups = current.groups.map { group ->
                                    if (group.addonId != completion.addonId) {
                                        group
                                    } else {
                                        val mergedStreams = if (completion.streams.isEmpty()) {
                                            group.streams
                                        } else {
                                            (group.streams + completion.streams).sortedForGroupedDisplay()
                                        }
                                        val stillLoading = remaining > 0
                                        val finalError = if (mergedStreams.isEmpty() && !stillLoading) {
                                            pluginFirstErrorByAddonId[completion.addonId]
                                        } else {
                                            null
                                        }
                                        group.copy(
                                            streams = mergedStreams,
                                            isLoading = stillLoading,
                                            error = finalError,
                                        )
                                    }
                                },
                                installedOrder = installedAddonOrder,
                            )
                            val anyLoading = updated.any { it.isLoading }
                            current.copy(
                                groups = updated,
                                isAnyLoading = anyLoading,
                                emptyStateReason = updated.toEmptyStateReason(anyLoading),
                            )
                        }
                    }

                }
            }

            for (availabilityJob in debridAvailabilityJobs) {
                availabilityJob.join()

                // Early binge-group match after each availability job completes
                if (isDirectAutoPlayFlow && !autoSelectTriggered && persistedBingeGroup != null) {
                    val allStreams = _uiState.value.groups.flatMap { it.streams }
                    if (allStreams.isNotEmpty()) {
                        val earlyMatch = StreamAutoPlaySelector.selectAutoPlayStream(
                            streams = allStreams,
                            mode = autoPlayMode,
                            regexPattern = playerSettings.streamAutoPlayRegex,
                            source = playerSettings.streamAutoPlaySource,
                            installedAddonNames = installedAddonNames,
                            selectedAddons = playerSettings.streamAutoPlaySelectedAddons,
                            selectedPlugins = playerSettings.streamAutoPlaySelectedPlugins,
                            preferredBingeGroup = persistedBingeGroup,
                            preferBingeGroupInSelection = true,
                            bingeGroupOnly = !timeoutElapsed,
                            debridEnabled = debridSettings.canResolvePlayableLinks,
                            activeResolverProviderId = debridSettings.activeResolverProviderId,
                        )
                        if (earlyMatch != null) {
                            autoSelectTriggered = true
                            _uiState.update { it.copy(autoPlayStream = earlyMatch) }
                            break
                        }
                    }
                }
            }

            launch {
                DirectDebridStreamPreparer.prepare(
                    streams = _uiState.value.groups
                        .filter { it.addonId in eligibleForDebridIds }
                        .flatMap { it.streams },
                    season = season,
                    episode = episode,
                    playerSettings = playerSettings,
                    installedAddonNames = installedAddonNames,
                ) { original, prepared ->
                    _uiState.update { current ->
                        current.copy(
                            groups = DirectDebridStreamPreparer.replacePreparedStream(
                                groups = current.groups,
                                original = original,
                                prepared = prepared,
                                eligibleGroupIds = eligibleForDebridIds,
                            ),
                        )
                    }

                    // Early binge-group match after each debrid-prepared stream
                    if (isDirectAutoPlayFlow && !autoSelectTriggered && persistedBingeGroup != null) {
                        val allStreams = _uiState.value.groups.flatMap { it.streams }
                        if (allStreams.isNotEmpty()) {
                            val earlyMatch = StreamAutoPlaySelector.selectAutoPlayStream(
                                streams = allStreams,
                                mode = autoPlayMode,
                                regexPattern = playerSettings.streamAutoPlayRegex,
                                source = playerSettings.streamAutoPlaySource,
                                installedAddonNames = installedAddonNames,
                                selectedAddons = playerSettings.streamAutoPlaySelectedAddons,
                                selectedPlugins = playerSettings.streamAutoPlaySelectedPlugins,
                                preferredBingeGroup = persistedBingeGroup,
                                preferBingeGroupInSelection = true,
                                bingeGroupOnly = !timeoutElapsed,
                                debridEnabled = debridSettings.canResolvePlayableLinks,
                                activeResolverProviderId = debridSettings.activeResolverProviderId,
                            )
                            if (earlyMatch != null) {
                                autoSelectTriggered = true
                                _uiState.update { it.copy(autoPlayStream = earlyMatch) }
                            }
                        }
                    }
                }

                // Early match / timeout-elapsed auto-select on each addon response
                if (isDirectAutoPlayFlow && !autoSelectTriggered) {
                    val allStreams = _uiState.value.groups.flatMap { it.streams }
                    if (allStreams.isNotEmpty()) {
                        if (timeoutElapsed) {
                            // After timeout: full fallback (bingeGroupOnly = false)
                            val selected = StreamAutoPlaySelector.selectAutoPlayStream(
                                streams = allStreams,
                                mode = autoPlayMode,
                                regexPattern = playerSettings.streamAutoPlayRegex,
                                source = playerSettings.streamAutoPlaySource,
                                installedAddonNames = installedAddonNames,
                                selectedAddons = playerSettings.streamAutoPlaySelectedAddons,
                                selectedPlugins = playerSettings.streamAutoPlaySelectedPlugins,
                                preferredBingeGroup = persistedBingeGroup,
                                preferBingeGroupInSelection = persistedBingeGroup != null,
                                bingeGroupOnly = false,
                                debridEnabled = debridSettings.canResolvePlayableLinks,
                                activeResolverProviderId = debridSettings.activeResolverProviderId,
                            )
                            if (selected != null) {
                                autoSelectTriggered = true
                                _uiState.update { it.copy(autoPlayStream = selected) }
                            }
                        } else if (persistedBingeGroup != null) {
                            // Before timeout: try binge-group-only early match
                            val earlyMatch = StreamAutoPlaySelector.selectAutoPlayStream(
                                streams = allStreams,
                                mode = autoPlayMode,
                                regexPattern = playerSettings.streamAutoPlayRegex,
                                source = playerSettings.streamAutoPlaySource,
                                installedAddonNames = installedAddonNames,
                                selectedAddons = playerSettings.streamAutoPlaySelectedAddons,
                                selectedPlugins = playerSettings.streamAutoPlaySelectedPlugins,
                                preferredBingeGroup = persistedBingeGroup,
                                preferBingeGroupInSelection = true,
                                bingeGroupOnly = true,
                                debridEnabled = debridSettings.canResolvePlayableLinks,
                                activeResolverProviderId = debridSettings.activeResolverProviderId,
                            )
                            if (earlyMatch != null) {
                                autoSelectTriggered = true
                                _uiState.update { it.copy(autoPlayStream = earlyMatch) }
                            }
                        }
                    }
                }
            }

            // All addons finished — run final auto-select if not yet triggered
            if (isDirectAutoPlayFlow && !autoSelectTriggered) {
                autoSelectTriggered = true
                val allStreams = _uiState.value.groups.flatMap { it.streams }
                val evaluation = StreamAutoPlaySelector.evaluateAutoPlayStream(
                    streams = allStreams,
                    mode = autoPlayMode,
                    regexPattern = playerSettings.streamAutoPlayRegex,
                    source = playerSettings.streamAutoPlaySource,
                    installedAddonNames = installedAddonNames,
                    selectedAddons = playerSettings.streamAutoPlaySelectedAddons,
                    selectedPlugins = playerSettings.streamAutoPlaySelectedPlugins,
                    preferredBingeGroup = persistedBingeGroup,
                    preferBingeGroupInSelection = persistedBingeGroup != null,
                    bingeGroupOnly = false,
                    debridEnabled = debridSettings.canResolvePlayableLinks,
                    activeResolverProviderId = debridSettings.activeResolverProviderId,
                )
                _uiState.update {
                    it.copy(
                        autoPlayStream = evaluation.stream,
                        autoPlayCandidates = evaluation.readyStreams,
                    )
                }
            }
            if (isDirectAutoPlayFlow && _uiState.value.autoPlayStream == null) {
                _uiState.update {
                    it.copy(
                        isDirectAutoPlayFlow = false,
                        showDirectAutoPlayOverlay = false,
                    )
                }
            }
            timeoutJob?.cancel()
        }
    }

    fun selectFilter(addonId: String?) {
        _uiState.update { it.copy(selectedFilter = addonId) }
    }

    fun consumeAutoPlay() {
        activeRequestKey = null
        _uiState.update {
            it.copy(
                autoPlayStream = null,
                autoPlayCandidates = emptyList(),
                isDirectAutoPlayFlow = false,
                showDirectAutoPlayOverlay = false,
            )
        }
    }

    fun skipAutoPlayStream(stream: StreamItem): Boolean {
        var hasNext = false
        _uiState.update { current ->
            val failedIndex = current.autoPlayCandidates.indexOf(stream)
            val remaining = if (failedIndex >= 0) {
                current.autoPlayCandidates.drop(failedIndex + 1)
            } else {
                current.autoPlayCandidates.drop(1)
            }
            hasNext = remaining.isNotEmpty()
            current.copy(
                autoPlayStream = remaining.firstOrNull(),
                autoPlayCandidates = remaining,
                isDirectAutoPlayFlow = remaining.isNotEmpty(),
                showDirectAutoPlayOverlay = remaining.isNotEmpty(),
            )
        }
        return hasNext
    }

    fun cancelLoading() {
        activeJob?.cancel()
        activeJob = null
        _uiState.update { current ->
            if (!current.isAnyLoading && current.groups.none { it.isLoading }) {
                current
            } else {
                val updatedGroups = current.groups.map { group ->
                    if (group.isLoading) group.copy(isLoading = false) else group
                }
                current.copy(
                    groups = updatedGroups,
                    isAnyLoading = false,
                    emptyStateReason = if (updatedGroups.isEmpty()) {
                        current.emptyStateReason
                    } else {
                        updatedGroups.toEmptyStateReason(anyLoading = false)
                    },
                )
            }
        }
    }

    fun clear() {
        activeJob?.cancel()
        activeJob = null
        activeRequestKey = null
        _uiState.value = StreamsUiState()
    }

    fun setOverlayVisible(visible: Boolean, message: String? = null) {
        _uiState.update { it.copy(showDirectAutoPlayOverlay = visible, overlayMessage = message) }
    }
}

// Provider count must not be capped: a repository can contain many narrowly scoped
// providers, and an alphabetical cap silently skipped otherwise valid sources.
// Keep network and DEX work bounded with a semaphore instead.
private const val CLOUDSTREAM_STREAM_PROVIDER_CONCURRENCY = 12
private const val STREAM_PROVIDER_TIMEOUT_MS = 25_000L
private const val STREAM_TOTAL_TIMEOUT_MS = 35_000L

private fun buildCloudStreamSearchRequest(
    type: String,
    videoId: String,
    parentMetaId: String?,
    parentMetaType: String?,
    season: Int?,
    episode: Int?,
    searchTitle: String?,
): CloudStreamSearchRequest? {
    val metaLookupType = parentMetaType?.takeIf { it.isNotBlank() } ?: type
    val metaLookupId = parentMetaId?.takeIf { it.isNotBlank() } ?: videoId
    val meta = MetaDetailsRepository.peek(metaLookupType, metaLookupId)
    val title = searchTitle?.trim()?.takeIf { it.isNotBlank() }
        ?: meta?.name?.trim()?.takeIf { it.isNotBlank() }
        ?: return null
    val aliases = (listOfNotNull(meta?.name, searchTitle) + meta?.aliases.orEmpty())
        .cleanCloudStreamRequestAliases(primaryTitle = title)
    val year = meta?.releaseInfo.cloudStreamReleaseYear()
        ?: title.cloudStreamReleaseYear()
    val episodeTitle = meta
        ?.videos
        ?.firstOrNull { video ->
            (season == null || video.season == season) &&
                (episode == null || video.episode == episode)
        }
        ?.title
        ?.trim()
        ?.takeIf { it.isNotBlank() }
    val externalId = sequenceOf(videoId, parentMetaId, metaLookupId)
        .filterNotNull()
        .map(String::trim)
        .firstOrNull { id -> id.matches(Regex("^tt\\d{5,12}$", RegexOption.IGNORE_CASE)) }

    return CloudStreamSearchRequest(
        title = title,
        aliases = aliases,
        type = type,
        year = year,
        season = season,
        episode = episode,
        episodeTitle = episodeTitle,
        externalId = externalId,
        genres = meta?.genres.orEmpty(),
    )
}

private fun List<String>.cleanCloudStreamRequestAliases(primaryTitle: String): List<String> {
    val primary = primaryTitle.trim()
    return mapNotNull { title ->
        title.trim().takeIf(String::isNotBlank)
    }
        .filterNot { title -> primary.isNotBlank() && title.equals(primary, ignoreCase = true) }
        .distinctBy { title -> title.lowercase() }
}

