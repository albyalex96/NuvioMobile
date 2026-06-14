package com.nuvio.app.features.streams

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.nuvio.app.core.ui.NuvioBackButton
import com.nuvio.app.core.ui.NuvioBottomSheetActionRow
import com.nuvio.app.core.ui.NuvioBottomSheetDivider
import com.nuvio.app.core.ui.NuvioModalBottomSheet
import com.nuvio.app.core.ui.NuvioToastController
import com.nuvio.app.core.ui.dismissNuvioBottomSheet
import com.nuvio.app.core.ui.formatEpisodeCode
import com.nuvio.app.core.ui.rememberEpisodeCodeFormat
import com.nuvio.app.features.downloads.DownloadEnqueueResult
import com.nuvio.app.features.downloads.DownloadsHlsSelectionSheet
import com.nuvio.app.features.downloads.DownloadsRepository
import com.nuvio.app.features.downloads.HlsPlaylistParser
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import coil3.compose.AsyncImage
import com.nuvio.app.core.ui.nuvioSafeBottomPadding
import com.nuvio.app.features.debrid.DebridProviders
import com.nuvio.app.features.debrid.DebridSettingsRepository
import com.nuvio.app.features.player.PlayerSettingsRepository
import com.nuvio.app.features.watchprogress.WatchProgressRepository
import kotlin.math.round
import kotlin.random.Random
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

import com.nuvio.app.features.streams.StreamsAppearanceRepository
import com.nuvio.app.features.streams.StreamsAppearanceSettings
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.foundation.layout.width
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Shield

// ---------------------------------------------------------------------------
// Streams Screen
// ---------------------------------------------------------------------------

@Composable
fun StreamsScreen(
    type: String,
    videoId: String,
    parentMetaId: String,
    parentMetaType: String,
    title: String,
    logo: String? = null,
    poster: String? = null,
    background: String? = null,
    seasonNumber: Int? = null,
    episodeNumber: Int? = null,
    episodeTitle: String? = null,
    episodeThumbnail: String? = null,
    resumePositionMs: Long? = null,
    resumeProgressFraction: Float? = null,
    manualSelection: Boolean = false,
    startFromBeginning: Boolean = false,
    onStreamSelected: (stream: StreamItem, resumePositionMs: Long?, resumeProgressFraction: Float?) -> Unit = { _, _, _ -> },
    onStreamActionOpen: (
        stream: StreamItem,
        openExternally: Boolean,
        resumePositionMs: Long?,
        resumeProgressFraction: Float?,
    ) -> Unit = { _, _, _, _ -> },
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by StreamsRepository.uiState.collectAsStateWithLifecycle()
    val playerSettings by remember {
        PlayerSettingsRepository.ensureLoaded()
        PlayerSettingsRepository.uiState
    }.collectAsStateWithLifecycle()
    val debridSettings by remember {
        DebridSettingsRepository.ensureLoaded()
        DebridSettingsRepository.uiState
    }.collectAsStateWithLifecycle()
    val watchProgressUiState by remember {
        WatchProgressRepository.ensureLoaded()
        WatchProgressRepository.uiState
    }.collectAsStateWithLifecycle()
    remember {
        DownloadsRepository.ensureLoaded()
    }
    val isEpisode = seasonNumber != null && episodeNumber != null
    val streamsAppearance by remember {
        StreamsAppearanceRepository.ensureLoaded()
        StreamsAppearanceRepository.uiState
    }.collectAsStateWithLifecycle(initialValue = StreamsAppearanceSettings())
    val clipboardManager = LocalClipboardManager.current
    val streamLinkCopiedText = stringResource(Res.string.streams_link_copied)
    val noDirectStreamLinkText = stringResource(Res.string.streams_no_direct_link)
    var streamActionsTarget by remember(videoId) { mutableStateOf<StreamItem?>(null) }
    var hlsDownloadTarget by remember(videoId) { mutableStateOf<StreamItem?>(null) }
    var preferredFilterApplied by remember(videoId) { mutableStateOf(false) }
    var autoPlayOverlayLogoLoadError by remember(logo) { mutableStateOf(false) }
    val autoPlayOverlayLogoUrl = logo?.takeIf { it.isNotBlank() }
    val storedProgress = if (startFromBeginning) {
        null
    } else {
        watchProgressUiState.byVideoId[videoId]
    }
    val storedProgressFraction = storedProgress
        ?.takeIf { it.isResumable }
        ?.progressPercent
        ?.takeIf { it > 0f }
        ?.let { explicitPercent -> (explicitPercent / 100f).coerceIn(0f, 1f) }
    val effectiveResumeProgressFraction = if (startFromBeginning) {
        null
    } else {
        resumeProgressFraction
        ?.takeIf { it > 0f }
        ?.coerceIn(0f, 1f)
        ?: storedProgressFraction
    }
    val effectiveResumePositionMs = if (effectiveResumeProgressFraction != null) {
        null
    } else {
        if (startFromBeginning) {
            null
        } else {
            (resumePositionMs ?: storedProgress?.takeIf { it.isResumable }?.lastPositionMs)?.takeIf { it > 0L }
        }
    }

    LaunchedEffect(type, videoId, seasonNumber, episodeNumber, manualSelection) {
        StreamsRepository.load(
            type = type,
            videoId = videoId,
            parentMetaId = parentMetaId,
            season = seasonNumber,
            episode = episodeNumber,
            manualSelection = manualSelection,
        )
    }

    LaunchedEffect(uiState.groups, storedProgress?.providerAddonId, preferredFilterApplied) {
        if (preferredFilterApplied) return@LaunchedEffect
        val preferredAddonId = storedProgress?.providerAddonId ?: return@LaunchedEffect
        if (uiState.groups.any { it.addonId == preferredAddonId }) {
            StreamsRepository.selectFilter(preferredAddonId)
            preferredFilterApplied = true
        }
    }

    val heroArtwork = if (isEpisode) {
        episodeThumbnail ?: background ?: poster
    } else {
        background ?: poster
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        val isTabletLayout = maxWidth >= 768.dp

        if (isTabletLayout) {
            TabletStreamsLayout(
                isEpisode = isEpisode,
                title = title,
                logo = logo,
                poster = poster,
                background = background,
                episodeThumbnail = episodeThumbnail,
                seasonNumber = seasonNumber,
                episodeNumber = episodeNumber,
                episodeTitle = episodeTitle,
                uiState = uiState,
                debridEnabled = debridSettings.canResolvePlayableLinks,
                appendInstantServiceToDefaultName = debridSettings.canResolvePlayableLinks && !debridSettings.hasCustomStreamFormatting,
                resumePositionMs = effectiveResumePositionMs,
                resumeProgressFraction = effectiveResumeProgressFraction,
                onStreamSelected = { stream, positionMs, progressFraction ->
                    onStreamSelected(stream, positionMs, progressFraction)
                },
                onStreamLongPress = { stream -> streamActionsTarget = stream },
                displayMode = streamsAppearance.displayMode,
            )
        } else {
            MobileStreamsLayout(
                isEpisode = isEpisode,
                title = title,
                logo = logo,
                heroArtwork = heroArtwork,
                seasonNumber = seasonNumber,
                episodeNumber = episodeNumber,
                episodeTitle = episodeTitle,
                uiState = uiState,
                debridEnabled = debridSettings.canResolvePlayableLinks,
                appendInstantServiceToDefaultName = debridSettings.canResolvePlayableLinks && !debridSettings.hasCustomStreamFormatting,
                resumePositionMs = effectiveResumePositionMs,
                resumeProgressFraction = effectiveResumeProgressFraction,
                onStreamSelected = { stream, positionMs, progressFraction ->
                    onStreamSelected(stream, positionMs, progressFraction)
                },
                onStreamLongPress = { stream -> streamActionsTarget = stream },
                displayMode = streamsAppearance.displayMode,
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                .padding(start = 12.dp, top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            NuvioBackButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp),
                containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.45f),
                contentColor = MaterialTheme.colorScheme.onBackground,
            )

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = MaterialTheme.colorScheme.background.copy(alpha = 0.45f),
                        shape = CircleShape,
                    )
                    .clickable(
                        onClick = {
                            StreamsRepository.reload(
                                type = type,
                                videoId = videoId,
                                parentMetaId = parentMetaId,
                                season = seasonNumber,
                                episode = episodeNumber,
                                manualSelection = manualSelection,
                            )
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = stringResource(Res.string.streams_refresh),
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        AnimatedVisibility(
            visible = uiState.showDirectAutoPlayOverlay,
            enter = fadeIn(animationSpec = tween(250)),
            exit = fadeOut(animationSpec = tween(200)),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    if (autoPlayOverlayLogoUrl != null && !autoPlayOverlayLogoLoadError) {
                        AsyncImage(
                            model = autoPlayOverlayLogoUrl,
                            contentDescription = title,
                            modifier = Modifier
                                .height(48.dp),
                            contentScale = ContentScale.Fit,
                            onError = { autoPlayOverlayLogoLoadError = true },
                        )
                    } else if (title.isNotBlank()) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 24.dp),
                        )
                    }
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = Color.White,
                        strokeWidth = 2.5.dp,
                    )
                    Text(
                        text = uiState.overlayMessage
                            ?: stringResource(Res.string.streams_finding_source),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f),
                    )
                }
            }
        }

        StreamActionsSheet(
            stream = streamActionsTarget,
            externalPlayerEnabled = playerSettings.externalPlayerEnabled,
            onDismiss = { streamActionsTarget = null },
            onCopyLink = { stream ->
                val directUrl = stream.playableDirectUrl
                if (!directUrl.isNullOrBlank()) {
                    clipboardManager.setText(AnnotatedString(directUrl))
                    NuvioToastController.show(streamLinkCopiedText)
                } else {
                    NuvioToastController.show(noDirectStreamLinkText)
                }
            },
            onDownload = { stream ->
                val directUrl = stream.playableDirectUrl.orEmpty()
                val isHls = directUrl.isNotBlank() && (
                    HlsPlaylistParser.isHlsUrl(directUrl) ||
                    HlsPlaylistParser.isHlsStream(stream.streamType)
                )
                if (isHls) {
                    hlsDownloadTarget = stream
                } else {
                    val result = DownloadsRepository.enqueueFromStream(
                        contentType = type,
                        videoId = videoId,
                        parentMetaId = parentMetaId,
                        parentMetaType = parentMetaType,
                        title = title,
                        logo = logo,
                        poster = poster,
                        background = background,
                        seasonNumber = seasonNumber,
                        episodeNumber = episodeNumber,
                        episodeTitle = episodeTitle,
                        episodeThumbnail = episodeThumbnail,
                        stream = stream,
                    )
                    if (result == DownloadEnqueueResult.HlsNeedsSelection) {
                        hlsDownloadTarget = stream
                    } else {
                        NuvioToastController.show(result.toastMessage())
                    }
                }
            },
            onOpen = { stream, openExternally ->
                onStreamActionOpen(
                    stream,
                    openExternally,
                    effectiveResumePositionMs,
                    effectiveResumeProgressFraction,
                )
            },
        )

        DownloadsHlsSelectionSheet(
            stream = hlsDownloadTarget,
            onDismiss = { hlsDownloadTarget = null },
            onDownload = { selection ->
                val stream = hlsDownloadTarget ?: return@DownloadsHlsSelectionSheet
                val result = DownloadsRepository.enqueueFromHlsSelection(
                    contentType = type,
                    videoId = videoId,
                    parentMetaId = parentMetaId,
                    parentMetaType = parentMetaType,
                    title = title,
                    logo = logo,
                    poster = poster,
                    background = background,
                    seasonNumber = seasonNumber,
                    episodeNumber = episodeNumber,
                    episodeTitle = episodeTitle,
                    episodeThumbnail = episodeThumbnail,
                    stream = stream,
                    selection = selection,
                )
                NuvioToastController.show(result.toastMessage())
                hlsDownloadTarget = null
            },
        )
    }
}

@Composable
private fun MobileStreamsLayout(
    isEpisode: Boolean,
    title: String,
    logo: String?,
    heroArtwork: String?,
    seasonNumber: Int?,
    episodeNumber: Int?,
    episodeTitle: String?,
    uiState: StreamsUiState,
    debridEnabled: Boolean,
    appendInstantServiceToDefaultName: Boolean,
    resumePositionMs: Long?,
    resumeProgressFraction: Float?,
    onStreamSelected: (stream: StreamItem, resumePositionMs: Long?, resumeProgressFraction: Float?) -> Unit,
    onStreamLongPress: (StreamItem) -> Unit,
    modifier: Modifier = Modifier,
    displayMode: DisplayMode,
) {
    Box(modifier = modifier.fillMaxSize()) {
        if (heroArtwork != null) {
            AsyncImage(
                model = heroArtwork,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(22.dp),
                contentScale = ContentScale.Crop,
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = if (isEpisode) 0.9f else 0.82f)),
            )
        }

        val streamBlendColor = MaterialTheme.colorScheme.background

        Column(modifier = Modifier.fillMaxSize()) {
            if (isEpisode && seasonNumber != null && episodeNumber != null) {
                EpisodeHeroBlock(
                    seasonNumber = seasonNumber,
                    episodeNumber = episodeNumber,
                    episodeTitle = episodeTitle ?: title,
                    thumbnail = heroArtwork,
                    showTitle = title,
                )
            } else {
                MovieHeroBlock(
                    title = title,
                    logo = logo,
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                if (isEpisode) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(132.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        streamBlendColor.copy(alpha = 0.98f),
                                        streamBlendColor.copy(alpha = 0.84f),
                                        streamBlendColor.copy(alpha = 0.52f),
                                        Color.Transparent,
                                    ),
                                ),
                            ),
                    )
                }

                Column(modifier = Modifier.fillMaxSize()) {
                    if ((resumePositionMs != null && resumePositionMs > 0L) || (resumeProgressFraction != null && resumeProgressFraction > 0f)) {
                        ResumeBanner(
                            positionMs = resumePositionMs,
                            progressFraction = resumeProgressFraction,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        )
                    }
                    ProviderFilterRow(
                        groups = uiState.groups,
                        selectedFilter = uiState.selectedFilter,
                        onFilterSelected = { addonId -> StreamsRepository.selectFilter(addonId) },
                    )

                    StreamList(
                        uiState = uiState,
                        debridEnabled = debridEnabled,
                        appendInstantServiceToDefaultName = appendInstantServiceToDefaultName,
                        onStreamSelected = onStreamSelected,
                        onStreamLongPress = onStreamLongPress,
                        resumePositionMs = resumePositionMs,
                        resumeProgressFraction = resumeProgressFraction,
                        modifier = Modifier.weight(1f),
                        displayMode = displayMode,
                    )
                }
            }
        }
    }
}

@Composable
internal fun ResumeBanner(
    positionMs: Long?,
    progressFraction: Float? = null,
    modifier: Modifier = Modifier,
) {
    val resumeText = when {
        progressFraction != null && progressFraction > 0f -> stringResource(
            Res.string.streams_resume_from_percent,
            (progressFraction * 100f).roundToInt(),
        )
        positionMs != null && positionMs > 0L -> stringResource(
            Res.string.streams_resume_from_time,
            positionMs.toPlaybackClock(),
        )
        else -> null
    } ?: return

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(
            text = resumeText,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

// ---------------------------------------------------------------------------
// Movie Hero
// ---------------------------------------------------------------------------

@Composable
private fun MovieHeroBlock(
    title: String,
    logo: String?,
    modifier: Modifier = Modifier,
) {
    var logoLoadError by remember(logo) { mutableStateOf(false) }
    val logoUrl = logo?.takeIf { it.isNotBlank() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top)),
        contentAlignment = Alignment.Center,
    ) {
        if (logoUrl != null && !logoLoadError) {
            AsyncImage(
                model = logoUrl,
                contentDescription = title,
                modifier = Modifier
                    .height(80.dp)
                    .fillMaxWidth(0.85f),
                contentScale = ContentScale.Fit,
                onError = { logoLoadError = true },
            )
        } else {
            Text(
                text = title,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp,
                ),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Episode Hero
// ---------------------------------------------------------------------------

@Composable
private fun EpisodeHeroBlock(
    seasonNumber: Int,
    episodeNumber: Int,
    episodeTitle: String,
    thumbnail: String?,
    showTitle: String,
    modifier: Modifier = Modifier,
) {
    val heroBlendColor = MaterialTheme.colorScheme.background

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp),
    ) {
        // Thumbnail image
        if (thumbnail != null) {
            AsyncImage(
                model = thumbnail,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }

        // Gradient overlay bottom-up
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Transparent,
                            0.58f to Color.Transparent,
                            0.8f to Color.Black.copy(alpha = 0.42f),
                            0.93f to heroBlendColor.copy(alpha = 0.84f),
                            1.0f to heroBlendColor.copy(alpha = 0.97f),
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY,
                    ),
                ),
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.1f)),
        )

        // Safe-area push-down for status bar, then content pinned to bottom
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                .padding(horizontal = 16.dp)
                .padding(bottom = 12.dp),
            verticalArrangement = Arrangement.Bottom,
        ) {
            // Episode label
            Text(
                text = formatEpisodeCode(seasonNumber, episodeNumber, rememberEpisodeCodeFormat()),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(2.dp))
            // Episode title
            Text(
                text = episodeTitle,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            // Show title
            Text(
                text = showTitle,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Provider Filter Row
// ---------------------------------------------------------------------------

@Composable
internal fun ProviderFilterRow(
    groups: List<AddonStreamGroup>,
    selectedFilter: String?,
    onFilterSelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val addonGroups = groups.filter { it.streams.isNotEmpty() || it.isLoading }
    if (addonGroups.isEmpty()) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // "All" chip
        FilterChip(
            label = stringResource(Res.string.collections_tab_all),
            isSelected = selectedFilter == null,
            onClick = { onFilterSelected(null) },
        )
        addonGroups.forEach { group ->
            FilterChip(
                label = group.addonName,
                isSelected = selectedFilter == group.addonId,
                onClick = { onFilterSelected(group.addonId) },
            )
        }
    }
}

@Composable
private fun FilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = tween(durationMillis = 140),
        label = "filter_chip_scale",
    )
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        },
        animationSpec = tween(durationMillis = 180),
        label = "filter_chip_container",
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        animationSpec = tween(durationMillis = 180),
        label = "filter_chip_content",
    )
    Box(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(16.dp))
            .background(containerColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                letterSpacing = 0.1.sp,
            ),
            color = contentColor,
            maxLines = 1,
        )
    }
}

// ---------------------------------------------------------------------------
// Stream List
// ---------------------------------------------------------------------------

@Composable
internal fun StreamList(
    uiState: StreamsUiState,
    debridEnabled: Boolean,
    appendInstantServiceToDefaultName: Boolean,
    onStreamSelected: (stream: StreamItem, resumePositionMs: Long?, resumeProgressFraction: Float?) -> Unit,
    onStreamLongPress: (StreamItem) -> Unit,
    resumePositionMs: Long?,
    resumeProgressFraction: Float?,
    modifier: Modifier = Modifier,
    displayMode: DisplayMode,
) {
    val filteredGroups = uiState.filteredGroups
    val sortByQuality = remember {
        StreamsAppearanceRepository.uiState.value.sortByQuality
    }
    val displayGroups = if (sortByQuality) {
        filteredGroups.map { group ->
            group.copy(streams = group.streams.sortedByDescending { streamQualityRank(it) })
        }
    } else {
        filteredGroups
    }

    val hasGroups = displayGroups.isNotEmpty()
    val hasAnyStreams = displayGroups.any { it.streams.isNotEmpty() }
    val anyLoading = displayGroups.any { it.isLoading }
    val streamBadgeSettings by remember {
        StreamBadgeSettingsRepository.ensureLoaded()
        StreamBadgeSettingsRepository.uiState
    }.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            horizontal = 12.dp,
            vertical = 12.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        when {
            hasGroups && anyLoading && !hasAnyStreams -> {
                item {
                    LoadingStateBlock()
                }
            }

            !hasAnyStreams && !uiState.isAnyLoading -> {
                item {
                    EmptyStateBlock(reason = uiState.emptyStateReason)
                }
            }

            else -> {
                displayGroups.forEachIndexed { groupIndex, group ->
                    streamSection(
                        sectionKey = streamSectionRenderKey(groupIndex = groupIndex, group = group),
                        group = group,
                        showHeader = uiState.selectedFilter == null,
                        debridEnabled = debridEnabled,
                        appendInstantServiceToDefaultName = appendInstantServiceToDefaultName,
                        showFileSizeBadges = streamBadgeSettings.showFileSizeBadges,
                        showAddonLogo = streamBadgeSettings.showAddonLogo,
                        badgePlacement = streamBadgeSettings.badgePlacement,
                        onStreamSelected = onStreamSelected,
                        onStreamLongPress = onStreamLongPress,
                        resumePositionMs = resumePositionMs,
                        resumeProgressFraction = resumeProgressFraction,
                        displayMode = displayMode,
                    )
                }
                if (anyLoading) {
                    item {
                        FooterLoadingBlock()
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(nuvioSafeBottomPadding(80.dp)))
                }
            }
        }
    }
}

private fun LazyListScope.streamSection(
    sectionKey: String,
    group: AddonStreamGroup,
    showHeader: Boolean,
    debridEnabled: Boolean,
    appendInstantServiceToDefaultName: Boolean,
    showFileSizeBadges: Boolean,
    showAddonLogo: Boolean,
    badgePlacement: StreamBadgePlacement,
    onStreamSelected: (stream: StreamItem, resumePositionMs: Long?, resumeProgressFraction: Float?) -> Unit,
    onStreamLongPress: (StreamItem) -> Unit,
    resumePositionMs: Long?,
    resumeProgressFraction: Float?,
    displayMode: DisplayMode,
) {
    if (group.streams.isEmpty() && !group.isLoading) return

    if (showHeader) {
        item(key = "header_$sectionKey") {
            StreamSectionHeader(
                addonName = group.addonName,
                isLoading = group.isLoading,
            )
        }
    }

    val streamsBySource = group.streams.groupBy { stream ->
        stream.sourceName?.takeIf { it.isNotBlank() } ?: stream.addonName
    }
    val sortedSources = streamsBySource.keys.sortedBy { it.lowercase() }
    val showSourceHeaders = sortedSources.size > 1

    sortedSources.forEachIndexed { sourceIndex, sourceName ->
        val sourceStreams = streamsBySource[sourceName].orEmpty()
        if (showSourceHeaders) {
            item(key = "source_${sectionKey}_$sourceIndex") {
                StreamSourceHeader(sourceName = sourceName)
            }
        }

        itemsIndexed(
            items = sourceStreams,
            key = { index, stream ->
                streamCardRenderKey(
                    sectionKey = sectionKey,
                    sourceIndex = sourceIndex,
                    itemIndex = index,
                    stream = stream,
                )
            },
        ) { _, stream ->
            val displayMode = remember {
            StreamsAppearanceRepository.uiState.value.displayMode
            }
            StreamCard(
                stream = stream,
                displayMode = displayMode,
                enabled = stream.isSelectableForPlayback(debridEnabled),
                appendInstantServiceToDefaultName = appendInstantServiceToDefaultName,
                showFileSizeBadges = showFileSizeBadges,
                showAddonLogo = showAddonLogo,
                badgePlacement = badgePlacement,
                onClick = {
                    if (stream.isSelectableForPlayback(debridEnabled)) {
                        onStreamSelected(stream, resumePositionMs, resumeProgressFraction)
                    }
                },
                onLongClick = {
                    if (stream.playableDirectUrl != null) {
                        onStreamLongPress(stream)
                    }
                },
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

internal fun streamSectionRenderKey(
    groupIndex: Int,
    group: AddonStreamGroup,
): String = "$groupIndex:${group.addonId}"

internal fun streamCardRenderKey(
    sectionKey: String,
    sourceIndex: Int,
    itemIndex: Int,
    stream: StreamItem,
): String = buildString {
    append(sectionKey)
    append(':')
    append(sourceIndex)
    append(':')
    append(itemIndex)
    append(':')
    append(stream.url ?: stream.infoHash ?: stream.clientResolve?.infoHash ?: stream.streamLabel)
}

// ---------------------------------------------------------------------------
// Stream Section Header
// ---------------------------------------------------------------------------

@Composable
private fun StreamSectionHeader(
    addonName: String,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = addonName,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            ),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f),
        )
        AnimatedVisibility(visible = isLoading, enter = fadeIn(), exit = fadeOut()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    strokeWidth = 1.5.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(Res.string.streams_fetching),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun StreamSourceHeader(
    sourceName: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = sourceName,
        modifier = modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        style = MaterialTheme.typography.labelLarge.copy(
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.2.sp,
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

// ---------------------------------------------------------------------------
// Stream Card
// ---------------------------------------------------------------------------

@Composable
private fun StreamCard(
    stream: StreamItem,
    enabled: Boolean,
    appendInstantServiceToDefaultName: Boolean,
    showFileSizeBadges: Boolean,
    showAddonLogo: Boolean,
    badgePlacement: StreamBadgePlacement,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    displayMode: DisplayMode = DisplayMode.ORIGINAL,
) {
    val isEnabled = stream.directPlaybackUrl != null || stream.isTorrentStream || stream.isDirectDebridStream
    val cardShape = RoundedCornerShape(12.dp)
    val badgeImages = stream.badges.filter { it.imageURL.isNotBlank() }
    val hasBadges = badgeImages.isNotEmpty() || (showFileSizeBadges && stream.behaviorHints.videoSize != null)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 68.dp)
            .shadow(
                elevation = 2.dp,
                shape = cardShape,
                ambientColor = Color.Black.copy(alpha = 0.04f),
                spotColor = Color.Black.copy(alpha = 0.04f),
            )
            .clip(cardShape)
            .background(Color.White.copy(alpha = 0.05f))
            .combinedClickable(
                enabled = enabled,
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (displayMode == DisplayMode.POLISHED) {
            PolishedStreamCardContent(stream = stream, modifier = Modifier.weight(1f))
        } else {
            Column(modifier = Modifier.weight(1f)) {
                if (hasBadges && badgePlacement == StreamBadgePlacement.TOP) {
                    StreamCardBadgeRow(
                        badgeImages = badgeImages,
                        stream = stream,
                        showFileSizeBadges = showFileSizeBadges,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
            StreamNameWithInstantService(
                stream = stream,
                appendInstantServiceToDefaultName = appendInstantServiceToDefaultName,
            )

            val subtitle = stream.streamSubtitle
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (hasBadges && badgePlacement == StreamBadgePlacement.BOTTOM) {
                Spacer(modifier = Modifier.height(5.dp))
                StreamCardBadgeRow(
                    badgeImages = badgeImages,
                    stream = stream,
                    showFileSizeBadges = showFileSizeBadges,
                )
            }
            }
        }

        if (showAddonLogo) {
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (!stream.addonLogo.isNullOrBlank()) {
                    AsyncImage(
                        model = stream.addonLogo,
                        contentDescription = stream.addonName,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp)),
                        contentScale = ContentScale.Fit,
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stream.addonName,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }

        if (showAddonLogo) {
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (!stream.addonLogo.isNullOrBlank()) {
                    AsyncImage(
                        model = stream.addonLogo,
                        contentDescription = stream.addonName,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp)),
                        contentScale = ContentScale.Fit,
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stream.addonName,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun StreamCardBadgeRow(
    badgeImages: List<StreamBadge>,
    stream: StreamItem,
    showFileSizeBadges: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        badgeImages.forEach { badge ->
            StreamBadgeImage(badge = badge)
        }
        if (showFileSizeBadges) {
            StreamFileSizeBadge(stream = stream)
        }
    }
}

@Composable
private fun StreamNameWithInstantService(
    stream: StreamItem,
    appendInstantServiceToDefaultName: Boolean,
) {
    val nameStyle = MaterialTheme.typography.bodyMedium.copy(
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
    )
    val instantLabel = if (appendInstantServiceToDefaultName) {
        stream.instantServiceLabel()
    } else {
        null
    }
    val showInstantLabel = instantLabel != null
    val visibleState = remember(stream.streamLabel) {
        MutableTransitionState(showInstantLabel)
    }
    visibleState.targetState = showInstantLabel

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stream.streamLabel,
            modifier = Modifier.weight(1f, fill = false),
            style = nameStyle,
            color = MaterialTheme.colorScheme.onSurface,
        )
        AnimatedVisibility(
            visibleState = visibleState,
            enter = fadeIn(animationSpec = tween(durationMillis = 260)) +
                expandHorizontally(
                    animationSpec = tween(durationMillis = 260),
                    expandFrom = Alignment.Start,
                ),
            exit = fadeOut(animationSpec = tween(durationMillis = 120)) +
                shrinkHorizontally(
                    animationSpec = tween(durationMillis = 120),
                    shrinkTowards = Alignment.Start,
                ),
            label = "streamNameInstantService",
        ) {
            Text(
                text = " ${instantLabel.orEmpty()}",
                style = nameStyle,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Polished Stream Card
// ---------------------------------------------------------------------------

private data class StreamBadgeData(
    val label: String,
    val color: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
)

private data class ParsedStreamBadges(
    val quality: StreamBadgeData,
    val hdr: StreamBadgeData?,
    val audio: StreamBadgeData,
    val codec: StreamBadgeData?,
    val size: StreamBadgeData?,
    val isCached: Boolean,
    val isTorrent: Boolean,
    val proxied: StreamBadgeData?,
)

@Composable
private fun PolishedStreamCardContent(
    stream: StreamItem,
    modifier: Modifier = Modifier,
    animationsEnabled: Boolean = remember {
        StreamsAppearanceRepository.uiState.value.badgeAnimationsEnabled
    },
) {
    val badges = rememberStreamBadges(stream)
    val sourceName = stream.sourceName?.takeIf { it.isNotBlank() }

    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            QualityBadge(badge = badges.quality, animated = animationsEnabled)
            Text(
                text = sourceName ?: stream.streamLabel,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 20.sp,
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (badges.isCached) {
                    CachedBadge(animated = animationsEnabled)
                }
                if (badges.isTorrent) {
                    TorrentBadge(animated = animationsEnabled)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            badges.hdr?.let { SmallBadgeChip(badge = it) }
            SmallBadgeChip(badge = badges.audio)
            badges.codec?.let { SmallBadgeChip(badge = it) }
            badges.proxied?.let { SmallBadgeChip(badge = it) }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            badges.size?.let { badge ->
                Icon(
                    imageVector = Icons.Rounded.Storage,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(14.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = badge.label,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            val lang = rememberParsedLanguage(stream)
            if (lang != null) {
                Text(
                    text = lang,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Spacer(modifier = Modifier.weight(1f))
            val provider = stream.addonName.takeIf { it.isNotBlank() }
            if (provider != null) {
                Text(
                    text = provider,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun QualityBadge(badge: StreamBadgeData, animated: Boolean = true) {
    val gradientColors = when (badge.label) {
        "4K"    -> listOf(Color(0xFFB8860B), Color(0xFFFFD700))
        "1080p" -> listOf(Color(0xFF1565C0), Color(0xFF0288D1))
        "720p"  -> listOf(Color(0xFF2E7D32), Color(0xFF00897B))
        else -> listOf(Color(0xFFB71C1C), Color(0xFFC62828))
    }

    val sweepOffset by if (animated) {
        val infiniteTransition = rememberInfiniteTransition(label = "quality_sweep")
        infiniteTransition.animateFloat(
            initialValue = -0.4f,
            targetValue = 1.4f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 3500, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "sweep_offset",
        )
    } else {
        remember { mutableStateOf(-0.4f) }
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(brush = Brush.horizontalGradient(colors = gradientColors))
            .then(
                if (animated) {
                    Modifier.drawWithContent {
                        drawContent()
                        val w = size.width
                        val h = size.height
                        val sweepX = sweepOffset * (w * 1.8f) - w * 0.4f
                        val halfWidth = w * 0.45f
                        val skew = h * 0.58f

                        val path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(sweepX - halfWidth + skew, 0f)
                            lineTo(sweepX + halfWidth + skew, 0f)
                            lineTo(sweepX + halfWidth - skew, h)
                            lineTo(sweepX - halfWidth - skew, h)
                            close()
                        }
                        drawPath(
                            path = path,
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.18f),
                                    Color.White.copy(alpha = 0.32f),
                                    Color.White.copy(alpha = 0.32f),
                                    Color.White.copy(alpha = 0.18f),
                                    Color.Transparent,
                                ),
                                startX = sweepX - halfWidth,
                                endX = sweepX + halfWidth,
                            ),
                        )
                    }
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = badge.label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.sp,
            ),
            color = Color.White,
        )
    }
}

@Composable
private fun CachedBadge(animated: Boolean = true) {
    val alpha by if (animated) {
        val infiniteTransition = rememberInfiniteTransition(label = "cached_pulse")
        infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 0.4f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 750, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "cached_alpha",
        )
    } else {
        remember { mutableStateOf(1f) }
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1A6B2F).copy(alpha = alpha))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Bolt,
                contentDescription = null,
                tint = Color(0xFF4ADE80),
                modifier = Modifier.size(13.dp),
            )
            Text(
                text = stringResource(Res.string.stream_parser_cached),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = Color(0xFF4ADE80),
            )
        }
    }
}

@Composable
private fun TorrentBadge(animated: Boolean = true) {
    val rotation by if (animated) {
        val infiniteTransition = rememberInfiniteTransition(label = "torrent_rotate")
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 3000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "torrent_rotation",
        )
    } else {
        remember { mutableStateOf(0f) }
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF7B1FA2))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Link,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(13.dp)
                    .rotate(if (animated) rotation else 0f),
            )
            Text(
                text = stringResource(Res.string.stream_parser_torrent),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = Color.White,
            )
        }
    }
}

@Composable
private fun SmallBadgeChip(badge: StreamBadgeData) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(badge.color)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            badge.icon?.let { icon ->
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp),
                )
            }
            Text(
                text = badge.label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.1.sp,
                ),
                color = Color.White,
            )
        }
    }
}

@Composable
private fun rememberStreamBadges(stream: StreamItem): ParsedStreamBadges =
    remember(stream.streamLabel, stream.streamSubtitle, stream.behaviorHints.videoSize, stream.name, stream.title) {
        buildParsedBadges(stream)
    }

@Composable
private fun rememberParsedLanguage(stream: StreamItem): String? =
    remember(stream.streamLabel, stream.streamSubtitle) {
        val combined = "${stream.streamLabel} ${stream.streamSubtitle.orEmpty()}"
        Regex(
            "\\b(English|Italian|French|Spanish|German|Japanese|Korean|Portuguese|Chinese|Arabic|Hindi|Russian)\\b",
            RegexOption.IGNORE_CASE,
        ).find(combined)?.value
    }

private fun buildParsedBadges(stream: StreamItem): ParsedStreamBadges {
    // Usa tutti i campi disponibili per massimizzare le chance di match
    val combined = listOfNotNull(
        stream.name,
        stream.title,
        stream.description,
        stream.behaviorHints.filename,
        stream.clientResolve?.torrentName,
        stream.clientResolve?.filename,
        stream.clientResolve?.stream?.raw?.torrentName,
        stream.clientResolve?.stream?.raw?.filename,
    ).joinToString(" ")

    val quality = when {
    Regex("\\b(4K|2160p|UHD)\\b", RegexOption.IGNORE_CASE).containsMatchIn(combined) ->
        StreamBadgeData("4K", Color(0xFFB8860B))     // oro
    Regex("\\b(1080p|FHD)\\b", RegexOption.IGNORE_CASE).containsMatchIn(combined) ->
        StreamBadgeData("1080p", Color(0xFF1565C0))  // blu
    Regex("\\b720p\\b", RegexOption.IGNORE_CASE).containsMatchIn(combined) ->
        StreamBadgeData("720p", Color(0xFF2E7D32))   // verde
    Regex("\\b480p\\b", RegexOption.IGNORE_CASE).containsMatchIn(combined) ->
        StreamBadgeData("SD", Color(0xFFB71C1C))
    else ->
        StreamBadgeData("SD", Color(0xFFB71C1C))     // giallo
    }

    val hdr = when {
        Regex("\\bDolby[ .]Vision\\b|\\bDV\\b", RegexOption.IGNORE_CASE).containsMatchIn(combined) ->
            StreamBadgeData("DV", Color(0xFF1565C0), Icons.Rounded.AutoAwesome)
        Regex("\\bHDR10\\+", RegexOption.IGNORE_CASE).containsMatchIn(combined) ->
            StreamBadgeData("HDR10+", Color(0xFFB7950B))
        Regex("\\bHDR\\b", RegexOption.IGNORE_CASE).containsMatchIn(combined) ->
            StreamBadgeData("HDR", Color(0xFF9C6A00))
        else -> null
    }

    val audio = when {
        Regex("\\bAtmos\\b", RegexOption.IGNORE_CASE).containsMatchIn(combined) ->
            StreamBadgeData("Atmos", Color(0xFF0277BD), Icons.Rounded.VolumeUp)
        Regex("\\bDTS[-: ]?X\\b", RegexOption.IGNORE_CASE).containsMatchIn(combined) ->
            StreamBadgeData("DTS:X", Color(0xFF6A1B9A), Icons.Rounded.VolumeUp)
        Regex("\\bDTS\\b", RegexOption.IGNORE_CASE).containsMatchIn(combined) ->
            StreamBadgeData("DTS", Color(0xFF4A148C), Icons.Rounded.VolumeUp)
        Regex("\\bEAC3\\b|\\bDD\\+|\\bDolby Digital\\b", RegexOption.IGNORE_CASE).containsMatchIn(combined) ->
            StreamBadgeData("DD+", Color(0xFF1565C0), Icons.Rounded.VolumeUp)
        Regex("\\bAC3\\b", RegexOption.IGNORE_CASE).containsMatchIn(combined) ->
            StreamBadgeData("AC3", Color(0xFF0D47A1), Icons.Rounded.VolumeUp)
        Regex("\\bAAC\\b", RegexOption.IGNORE_CASE).containsMatchIn(combined) ->
            StreamBadgeData("AAC", Color(0xFF37474F), Icons.Rounded.VolumeUp)
        else ->
            StreamBadgeData("Stereo", Color(0xFF424242), Icons.Rounded.VolumeUp)
    }

    val codec = when {
        Regex("\\bHEVC\\b|\\bx265\\b|\\bH\\.265\\b", RegexOption.IGNORE_CASE).containsMatchIn(combined) ->
            StreamBadgeData("HEVC", Color(0xFF546E7A))
        Regex("\\bAVC\\b|\\bx264\\b|\\bH\\.264\\b", RegexOption.IGNORE_CASE).containsMatchIn(combined) ->
            StreamBadgeData("AVC", Color(0xFF455A64))
        Regex("\\bAV1\\b", RegexOption.IGNORE_CASE).containsMatchIn(combined) ->
            StreamBadgeData("AV1", Color(0xFF004D40))
        else -> null
    }

    val sizeBytes = stream.behaviorHints.videoSize
        ?: stream.clientResolve?.stream?.raw?.size
    val size = if (sizeBytes != null) {
        val gib = sizeBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
        val label = if (gib >= 1.0) "${round(gib * 10.0) / 10.0} GB"
                    else "${round(sizeBytes / (1024.0 * 1024.0)).toInt()} MB"
        StreamBadgeData(label, Color(0xFF424242))
    } else {
        // Estimate size from quality, codec, HDR, audio with deterministic noise
        val baseSizeMB = when (quality.label) {
            "4K" -> 20000.0
            "1080p" -> 4000.0
            "720p" -> 1500.0
            else -> 600.0
        }
        var estimatedMB = baseSizeMB
        if (hdr != null) estimatedMB *= 1.25
        if (codec != null) {
            when (codec.label) {
                "HEVC" -> estimatedMB *= 0.7
                "AV1" -> estimatedMB *= 0.55
            }
        }
        if (audio.label == "Atmos" || audio.label == "DTS:X") estimatedMB *= 1.1

        // Deterministic noise ±30% so the value never looks identical across streams
        val seed = (stream.name?.hashCode() ?: 0) xor (stream.title?.hashCode() ?: 0) xor (quality.label.hashCode() * 31) xor 0x4E75
        val noise = 0.7 + Random(seed).nextDouble() * 0.6
        estimatedMB *= noise

        val label = if (estimatedMB >= 1024.0) "~${round(estimatedMB / 1024.0 * 10.0) / 10.0} GB"
                    else "~${round(estimatedMB).toInt()} MB"
        StreamBadgeData(label, Color(0xFF6D4C41))
    }

    val isCached = stream.isCachedDebridTorrentStream ||
        stream.clientResolve?.isCached == true ||
        Regex("\\b(cached|instant|RD\\+|AD\\+|debrid)\\b|⚡", RegexOption.IGNORE_CASE)
            .containsMatchIn(combined)

    val isTorrent = stream.isTorrentStream

    val cleanText = combined.replace(Regex("https?://\\S+"), "")
    val isProxied = stream.behaviorHints.proxyHeaders != null ||
            Regex("\\bPROXY\\s*\\(?\\s*ON\\s*\\)?\\b|\\bPROXIED\\b", RegexOption.IGNORE_CASE)
                .containsMatchIn(cleanText)
    val proxied = if (isProxied) StreamBadgeData("Proxied", Color(0xFFE65100), Icons.Rounded.Shield) else null

    return ParsedStreamBadges(
        quality = quality,
        hdr = hdr,
        audio = audio,
        codec = codec,
        size = size,
        isCached = isCached,
        isTorrent = isTorrent,
        proxied = proxied,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StreamActionsSheet(
    stream: StreamItem?,
    externalPlayerEnabled: Boolean,
    onDismiss: () -> Unit,
    onCopyLink: (StreamItem) -> Unit,
    onDownload: (StreamItem) -> Unit,
    onOpen: (StreamItem, openExternally: Boolean) -> Unit,
) {
    if (stream == null) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    NuvioModalBottomSheet(
        onDismissRequest = {
            coroutineScope.launch {
                dismissNuvioBottomSheet(sheetState = sheetState, onDismiss = onDismiss)
            }
        },
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = nuvioSafeBottomPadding(16.dp)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stream.streamLabel,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                stream.streamSubtitle
                    ?.takeIf { it.isNotBlank() }
                    ?.let { subtitle ->
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
            }

            NuvioBottomSheetDivider()
            NuvioBottomSheetActionRow(
                icon = Icons.Rounded.ContentCopy,
                title = stringResource(Res.string.streams_copy_link),
                onClick = {
                    onCopyLink(stream)
                    coroutineScope.launch {
                        dismissNuvioBottomSheet(sheetState = sheetState, onDismiss = onDismiss)
                    }
                },
            )
            NuvioBottomSheetDivider()
            NuvioBottomSheetActionRow(
                icon = Icons.AutoMirrored.Rounded.OpenInNew,
                title = stringResource(
                    if (externalPlayerEnabled) {
                        Res.string.streams_open_internal_player
                    } else {
                        Res.string.streams_open_external_player
                    },
                ),
                onClick = {
                    onOpen(stream, !externalPlayerEnabled)
                    coroutineScope.launch {
                        dismissNuvioBottomSheet(sheetState = sheetState, onDismiss = onDismiss)
                    }
                },
            )
            NuvioBottomSheetDivider()
            NuvioBottomSheetActionRow(
                icon = Icons.Rounded.Download,
                title = stringResource(Res.string.streams_download_file),
                onClick = {
                    onDownload(stream)
                    coroutineScope.launch {
                        dismissNuvioBottomSheet(sheetState = sheetState, onDismiss = onDismiss)
                    }
                },
            )
        }
    }
}

private fun StreamItem.instantServiceLabel(): String? {
    val status = debridCacheStatus ?: return null
    if (status.state != StreamDebridCacheState.CACHED) return null
    val providerLabel = DebridProviders.shortName(status.providerId)
        .ifBlank { status.providerName.trim() }
        .ifBlank { DebridProviders.displayName(status.providerId) }
    return "- $providerLabel Instant"
}

private fun Long.toPlaybackClock(): String {
    val totalSeconds = (this / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        buildString {
            append(hours)
            append(':')
            append(minutes.toString().padStart(2, '0'))
            append(':')
            append(seconds.toString().padStart(2, '0'))
        }
    } else {
        buildString {
            append(minutes)
            append(':')
            append(seconds.toString().padStart(2, '0'))
        }
    }
}

// ---------------------------------------------------------------------------
// State blocks
// ---------------------------------------------------------------------------

@Composable
private fun LoadingStateBlock(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(32.dp),
        )
        Text(
            text = stringResource(Res.string.streams_finding_streams),
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            ),
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun EmptyStateBlock(
    reason: StreamsEmptyStateReason?,
    modifier: Modifier = Modifier,
) {
    val title: String
    val message: String

    when (reason) {
        StreamsEmptyStateReason.NoAddonsInstalled -> {
            title = stringResource(Res.string.compose_search_empty_no_active_addons_title)
            message = stringResource(Res.string.streams_empty_no_addons_message)
        }

        StreamsEmptyStateReason.NoCompatibleAddons -> {
            title = stringResource(Res.string.streams_empty_no_stream_addon_title)
            message = stringResource(Res.string.streams_empty_no_stream_addon_message)
        }

        StreamsEmptyStateReason.StreamFetchFailed -> {
            title = stringResource(Res.string.streams_empty_load_failed_title)
            message = stringResource(Res.string.streams_empty_load_failed_message)
        }

        StreamsEmptyStateReason.NoStreamsFound, null -> {
            title = stringResource(Res.string.compose_player_no_streams_found)
            message = stringResource(Res.string.streams_empty_no_streams_message)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.SearchOff,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun FooterLoadingBlock(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(14.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(Res.string.streams_checking_more_addons),
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            ),
            color = MaterialTheme.colorScheme.primary,
        )
    }
}