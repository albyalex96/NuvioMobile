package com.nuvio.app.features.streams

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import com.nuvio.app.core.i18n.localizedByteUnit
import com.nuvio.app.core.ui.NuvioBackButton
import com.nuvio.app.core.ui.NuvioBottomSheetActionRow
import com.nuvio.app.core.ui.NuvioBottomSheetDivider
import com.nuvio.app.core.ui.NuvioModalBottomSheet
import com.nuvio.app.core.ui.NuvioToastController
import com.nuvio.app.core.ui.dismissNuvioBottomSheet
import com.nuvio.app.features.downloads.DownloadsRepository
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import coil3.compose.AsyncImage
import com.nuvio.app.core.ui.nuvioSafeBottomPadding
import com.nuvio.app.features.notifications.EpisodeReleaseNotificationPlatform
import com.nuvio.app.features.player.PlayerSettingsRepository
import com.nuvio.app.features.watchprogress.WatchProgressRepository
import kotlinx.coroutines.launch
import kotlin.math.round
import kotlin.math.roundToInt
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.foundation.layout.width
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import org.jetbrains.compose.resources.stringResource
import nuvio.composeapp.generated.resources.stream_parser_cached_badge_label
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
    downloadMode: Boolean = false,
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
    val coroutineScope = rememberCoroutineScope()
    val streamLinkCopiedText = stringResource(Res.string.streams_link_copied)
    val noDirectStreamLinkText = stringResource(Res.string.streams_no_direct_link)
    val torrentUnsupportedText = stringResource(Res.string.streams_torrent_not_supported)
    var streamActionsTarget by remember(videoId) { mutableStateOf<StreamItem?>(null) }
    var preferredFilterApplied by remember(videoId) { mutableStateOf(false) }
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
            searchQuery = title,
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
    fun enqueueDownload(stream: StreamItem) {
        coroutineScope.launch {
            runCatching { EpisodeReleaseNotificationPlatform.requestAuthorization() }
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
            NuvioToastController.show(result.toastMessage())
        }
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
                displayMode = streamsAppearance.displayMode,
                seasonNumber = seasonNumber,
                episodeNumber = episodeNumber,
                episodeTitle = episodeTitle,
                uiState = uiState,
                resumePositionMs = effectiveResumePositionMs,
                resumeProgressFraction = effectiveResumeProgressFraction,
                onStreamSelected = { stream, positionMs, progressFraction ->
                    if (stream.isTorrentStream) {
                        NuvioToastController.show(torrentUnsupportedText)
                    } else if (downloadMode) {
                        enqueueDownload(stream)
                    } else {
                        onStreamSelected(stream, positionMs, progressFraction)
                    }
                },
                onStreamLongPress = { stream -> streamActionsTarget = stream },
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
                displayMode = streamsAppearance.displayMode,
                uiState = uiState,
                resumePositionMs = effectiveResumePositionMs,
                resumeProgressFraction = effectiveResumeProgressFraction,
                onStreamSelected = { stream, positionMs, progressFraction ->
                    if (stream.isTorrentStream) {
                        NuvioToastController.show(torrentUnsupportedText)
                    } else if (downloadMode) {
                        enqueueDownload(stream)
                    } else {
                        onStreamSelected(stream, positionMs, progressFraction)
                    }
                },
                onStreamLongPress = { stream -> streamActionsTarget = stream },
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
                                searchQuery = title,
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
                    if (!logo.isNullOrBlank()) {
                        AsyncImage(
                            model = logo,
                            contentDescription = null,
                            modifier = Modifier
                                .height(48.dp),
                            contentScale = ContentScale.Fit,
                        )
                    }
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = Color.White,
                        strokeWidth = 2.5.dp,
                    )
                    Text(
                        text = stringResource(Res.string.streams_finding_source),
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
                val directUrl = stream.directPlaybackUrl
                if (!directUrl.isNullOrBlank()) {
                    clipboardManager.setText(AnnotatedString(directUrl))
                    NuvioToastController.show(streamLinkCopiedText)
                } else {
                    NuvioToastController.show(noDirectStreamLinkText)
                }
            },
            onDownload = { stream ->
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
                NuvioToastController.show(result.toastMessage())
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
    }
}

@Composable
private fun MobileStreamsLayout(
    isEpisode: Boolean,
    title: String,
    logo: String?,
    heroArtwork: String?,
    seasonNumber: Int?,
    displayMode: DisplayMode, 
    episodeNumber: Int?,
    episodeTitle: String?,
    uiState: StreamsUiState,
    resumePositionMs: Long?,
    resumeProgressFraction: Float?,
    onStreamSelected: (stream: StreamItem, resumePositionMs: Long?, resumeProgressFraction: Float?) -> Unit,
    onStreamLongPress: (StreamItem) -> Unit,
    modifier: Modifier = Modifier,
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
                        onStreamSelected = onStreamSelected,
                        onStreamLongPress = onStreamLongPress,
                        resumePositionMs = resumePositionMs,
                        displayMode = displayMode,
                        resumeProgressFraction = resumeProgressFraction,
                        modifier = Modifier.weight(1f),
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
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top)),
        contentAlignment = Alignment.Center,
    ) {
        if (logo != null) {
            AsyncImage(
                model = logo,
                contentDescription = null,
                modifier = Modifier
                    .height(80.dp)
                    .fillMaxWidth(0.85f),
                contentScale = ContentScale.Fit,
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
                text = stringResource(Res.string.streams_episode_badge, seasonNumber, episodeNumber),
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
    onStreamSelected: (stream: StreamItem, resumePositionMs: Long?, resumeProgressFraction: Float?) -> Unit,
    onStreamLongPress: (StreamItem) -> Unit,
    resumePositionMs: Long?,
    displayMode: DisplayMode,
    resumeProgressFraction: Float?,
    modifier: Modifier = Modifier,
) {
    val filteredGroups = uiState.filteredGroups
    val hasGroups = filteredGroups.isNotEmpty()
    val hasAnyStreams = filteredGroups.any { it.streams.isNotEmpty() }
    val anyLoading = filteredGroups.any { it.isLoading }

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
                filteredGroups.forEachIndexed { groupIndex, group ->
                    streamSection(
                        sectionKey = streamSectionRenderKey(groupIndex = groupIndex, group = group),
                        group = group,
                        showHeader = uiState.selectedFilter == null,
                        onStreamSelected = onStreamSelected,
                        displayMode = displayMode,
                        onStreamLongPress = onStreamLongPress,
                        resumePositionMs = resumePositionMs,
                        resumeProgressFraction = resumeProgressFraction,
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
            StreamCard(
                stream = stream,
                onClick = {
                    if (stream.directPlaybackUrl != null || stream.isTorrentStream || stream.isDirectDebridStream) {
                        onStreamSelected(stream, resumePositionMs, resumeProgressFraction)
                    }
                },
                onLongClick = {
                    if (stream.directPlaybackUrl != null) {
                        onStreamLongPress(stream)
                    }
                },
                displayMode = displayMode,
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
    displayMode: DisplayMode,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val isEnabled = stream.directPlaybackUrl != null || stream.isTorrentStream || stream.isDirectDebridStream
    val cardShape = RoundedCornerShape(12.dp)
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
                enabled = isEnabled,
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        if (displayMode == DisplayMode.POLISHED) {
            PolishedStreamCardContent(stream = stream, modifier = Modifier.weight(1f))
        } else {
            OriginalStreamCardContent(stream = stream, modifier = Modifier.weight(1f))
        }
    }
}
@Composable
private fun OriginalStreamCardContent(stream: StreamItem, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = stream.streamLabel,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 20.sp,
                letterSpacing = 0.1.sp,
            ),
            color = MaterialTheme.colorScheme.onSurface,
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
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            StreamFileSizeBadge(stream = stream)
        }
    }
}
private data class StreamBadgeData(
    val label: String,
    val color: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    val iconTint: Color? = null,
)

@Composable
private fun PolishedStreamCardContent(stream: StreamItem, modifier: Modifier = Modifier) {
    val badges = rememberStreamBadges(stream)
    val qualityBadge = badges.quality
    val hdrBadge = badges.hdr
    val audioBadge = badges.audio
    val codecBadge = badges.codec
    val sizeBadge = badges.size
    val isCached = badges.isCached
    val sourceName = stream.sourceName?.takeIf { it.isNotBlank() }

    Column(modifier = modifier) {
        // Riga 1: badge qualità (grande) + titolo fonte + cached
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            // Badge qualità video — sfondo colorato pieno, testo grande
            QualityBadge(badge = qualityBadge)

            // Titolo / source name al centro con peso
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

            // Badge "Cached" con icona fulmine
            if (isCached) {
                CachedBadge()
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Riga 2: HDR + Audio + Codec
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            hdrBadge?.let { SmallBadgeChip(badge = it) }
            audioBadge?.let { SmallBadgeChip(badge = it) }
            codecBadge?.let { SmallBadgeChip(badge = it) }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Riga 3: dimensione file + provider (allineato a destra)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            sizeBadge?.let { badge ->
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

            // Lingua (se presente nel subtitle)
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

            // Provider name allineato a destra
            val provider = stream.addonName.takeIf { it.isNotBlank() }
            if (provider != null) {
                Text(
                    text = provider,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Badge composable
// ---------------------------------------------------------------------------

@Composable
private fun QualityBadge(badge: StreamBadgeData) {
    val gradientColors = when (badge.label) {
        "4K"    -> listOf(Color(0xFF6C3FE8), Color(0xFF1565C0))  // viola → blu
        "1080p" -> listOf(Color(0xFF1565C0), Color(0xFF0288D1))  // blu → azzurro
        "720p"  -> listOf(Color(0xFF2E7D32), Color(0xFF00897B))  // verde → teal
        "SD"    -> listOf(Color(0xFF616161), Color(0xFF424242))  // grigio scuro → più scuro
        else    -> listOf(badge.color, badge.color)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                brush = Brush.horizontalGradient(colors = gradientColors),
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
private fun CachedBadge() {
    val infiniteTransition = rememberInfiniteTransition(label = "cached_pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "cached_alpha",
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1A6B2F).copy(alpha = alpha))  // ← alpha qui
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
                tint = Color(0xFF4ADE80),  // ← fisso
                modifier = Modifier.size(13.dp),
            )
            Text(
                text = stringResource(Res.string.stream_parser_cached_badge_label),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = Color(0xFF4ADE80),  // ← fisso
            )
        }
    }
}

@Composable
private fun SmallBadgeChip(badge: StreamBadgeData) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(badge.color)          // ← sfondo pieno, non più alpha 0.15
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
                    tint = Color.White,        // ← icona sempre bianca
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
                color = Color.White,           // ← testo sempre bianco
            )
        }
    }
}

@Composable
private fun StreamBadgeChip(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.2.sp,
            ),
            color = Color.White,
        )
    }
}

// ---------------------------------------------------------------------------
// Badge parsing
// ---------------------------------------------------------------------------

private data class ParsedStreamBadges(
    val quality: StreamBadgeData,
    val hdr: StreamBadgeData?,
    val audio: StreamBadgeData,
    val codec: StreamBadgeData?,
    val size: StreamBadgeData?,
    val isCached: Boolean,
)

@Composable
private fun rememberStreamBadges(stream: StreamItem): ParsedStreamBadges =
    remember(stream.streamLabel, stream.streamSubtitle, stream.behaviorHints.videoSize) {
        buildParsedBadges(stream)
    }

@Composable
private fun rememberParsedLanguage(stream: StreamItem): String? =
    remember(stream.streamLabel, stream.streamSubtitle) {
        val combined = "${stream.streamLabel} ${stream.streamSubtitle.orEmpty()}"
        Regex("\\b(English|Italian|French|Spanish|German|Japanese|Korean|Portuguese|Chinese|Arabic|Hindi|Russian)\\b", RegexOption.IGNORE_CASE)
            .find(combined)?.value
    }

private fun buildParsedBadges(stream: StreamItem): ParsedStreamBadges {
    val combined = "${stream.streamLabel} ${stream.streamSubtitle.orEmpty()}"

    // --- Qualità video: colore diverso per ogni livello
    val quality = when {
        Regex("\\b(4K|2160p|UHD)\\b", RegexOption.IGNORE_CASE).containsMatchIn(combined) ->
            StreamBadgeData("4K", Color(0xFF6C3FE8))           // viola intenso
        Regex("\\b1080p\\b|\\bFHD\\b", RegexOption.IGNORE_CASE).containsMatchIn(combined) ->
            StreamBadgeData("1080p", Color(0xFF1565C0))        // blu
        Regex("\\b720p\\b", RegexOption.IGNORE_CASE).containsMatchIn(combined) ->
            StreamBadgeData("720p", Color(0xFF2E7D32))         // verde
        Regex("\\b480p\\b", RegexOption.IGNORE_CASE).containsMatchIn(combined) ->
            StreamBadgeData("SD", Color(0xFF616161))           // grigio medio
        else ->
            StreamBadgeData("SD", Color(0xFF616161))           // default grigio
    }

    // --- HDR
    val hdr = when {
        Regex("\\bDolby[ .]Vision\\b|\\bDV\\b", RegexOption.IGNORE_CASE).containsMatchIn(combined) ->
            StreamBadgeData(
                label = "DV",
                color = Color(0xFF1565C0),
                icon = Icons.Rounded.AutoAwesome,
                iconTint = Color(0xFF90CAF9),                  // azzurro chiaro
            )
        Regex("\\bHDR10\\+", RegexOption.IGNORE_CASE).containsMatchIn(combined) ->
            StreamBadgeData(
                label = "HDR10+",
                color = Color(0xFFB7950B),
                iconTint = Color(0xFFFFD54F),                  // ambra
            )
        Regex("\\bHDR\\b", RegexOption.IGNORE_CASE).containsMatchIn(combined) ->
            StreamBadgeData(
                label = "HDR",
                color = Color(0xFF9C6A00),
                iconTint = Color(0xFFFFCC80),                  // arancio chiaro
            )
        else -> null
    }

    // --- Audio: ogni formato ha il suo colore distinto
    val audio = when {
        Regex("\\bAtmos\\b", RegexOption.IGNORE_CASE).containsMatchIn(combined) ->
            StreamBadgeData(
                label = "Atmos",
                color = Color(0xFF0277BD),                     // azzurro Dolby
                icon = Icons.Rounded.VolumeUp,
                iconTint = Color(0xFF81D4FA),
            )
        Regex("\\bDTS[-: ]?X\\b", RegexOption.IGNORE_CASE).containsMatchIn(combined) ->
            StreamBadgeData(
                label = "DTS:X",
                color = Color(0xFF6A1B9A),                     // viola DTS
                icon = Icons.Rounded.VolumeUp,
                iconTint = Color(0xFFCE93D8),
            )
        Regex("\\bDTS\\b", RegexOption.IGNORE_CASE).containsMatchIn(combined) ->
            StreamBadgeData(
                label = "DTS",
                color = Color(0xFF4A148C),                     // viola scuro
                icon = Icons.Rounded.VolumeUp,
                iconTint = Color(0xFFB39DDB),
            )
        Regex("\\bEAC3\\b|\\bDD\\+|\\bDolby Digital\\b", RegexOption.IGNORE_CASE).containsMatchIn(combined) ->
            StreamBadgeData(
                label = "DD+",
                color = Color(0xFF1565C0),                     // blu Dolby Digital
                icon = Icons.Rounded.VolumeUp,
                iconTint = Color(0xFF90CAF9),
            )
        Regex("\\bAC3\\b", RegexOption.IGNORE_CASE).containsMatchIn(combined) ->
            StreamBadgeData(
                label = "AC3",
                color = Color(0xFF0D47A1),
                icon = Icons.Rounded.VolumeUp,
                iconTint = Color(0xFF82B1FF),
            )
        Regex("\\bAAC\\b", RegexOption.IGNORE_CASE).containsMatchIn(combined) ->
            StreamBadgeData(
                label = "AAC",
                color = Color(0xFF37474F),                     // grigio-blu scuro
                icon = Icons.Rounded.VolumeUp,
                iconTint = Color(0xFF90A4AE),
            )
        else ->
            StreamBadgeData(                                   // default: Stereo
                label = "Stereo",
                color = Color(0xFF424242),
                icon = Icons.Rounded.VolumeUp,
                iconTint = Color(0xFFBDBDBD),
            )
    }

    // --- Codec
    val codec = when {
        Regex("\\bHEVC\\b|\\bx265\\b|\\bH\\.265\\b", RegexOption.IGNORE_CASE).containsMatchIn(combined) ->
            StreamBadgeData(
                label = "HEVC",
                color = Color(0xFF546E7A),
                iconTint = Color(0xFFB0BEC5),
            )
        Regex("\\bAVC\\b|\\bx264\\b|\\bH\\.264\\b", RegexOption.IGNORE_CASE).containsMatchIn(combined) ->
            StreamBadgeData(
                label = "AVC",
                color = Color(0xFF455A64),
                iconTint = Color(0xFFCFD8DC),
            )
        Regex("\\bAV1\\b", RegexOption.IGNORE_CASE).containsMatchIn(combined) ->
            StreamBadgeData(
                label = "AV1",
                color = Color(0xFF004D40),                     // verde scuro
                iconTint = Color(0xFF80CBC4),
            )
        else -> null
    }

    // --- Dimensione file
    val sizeBytes = stream.behaviorHints.videoSize
    val size = if (sizeBytes != null) {
        val gib = sizeBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
        val label = if (gib >= 1.0) {
            "${kotlin.math.round(gib * 10.0) / 10.0} GB"
        } else {
            "${kotlin.math.round(sizeBytes / (1024.0 * 1024.0)).toInt()} MB"
        }
        StreamBadgeData(label, Color(0xFF424242))
    } else null

    // --- Cached: cerca keyword tipiche debrid/cache
    val isCached = Regex(
        "\\b(cached|instant|RD\\+|AD\\+|debrid)\\b|⚡",
        RegexOption.IGNORE_CASE,
    ).containsMatchIn(combined)

    return ParsedStreamBadges(
        quality = quality,
        hdr = hdr,
        audio = audio,
        codec = codec,
        size = size,
        isCached = isCached,
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

@Composable
private fun StreamFileSizeBadge(stream: StreamItem) {
    val bytes = stream.behaviorHints.videoSize ?: return
    val gib = bytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
    val sizeLabel = if (gib >= 1.0) {
        val roundedGiB = round(gib * 10.0) / 10.0
        "$roundedGiB ${localizedByteUnit("GB")}"
    } else {
        val mib = bytes.toDouble() / (1024.0 * 1024.0)
        "${round(mib).toInt()} ${localizedByteUnit("MB")}"
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0A0C0C))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = stringResource(Res.string.streams_size, sizeLabel),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.2.sp,
            ),
            color = Color.White,
        )
    }
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
