package com.nuvio.app.features.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nuvio.app.features.details.MetaDetailsUiState
import com.nuvio.app.features.player.cast.CastController
import com.nuvio.app.features.player.cast.CastDevicePicker
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.still_watching_cancel
import nuvio.composeapp.generated.resources.still_watching_proceed
import nuvio.composeapp.generated.resources.still_watching_timeout
import nuvio.composeapp.generated.resources.still_watching_title
import org.jetbrains.compose.resources.stringResource
import com.nuvio.app.features.details.MetaVideo
import com.nuvio.app.features.downloads.DownloadsRepository
import com.nuvio.app.features.p2p.P2pConsentDialog
import com.nuvio.app.features.p2p.P2pSettingsRepository
import com.nuvio.app.features.streams.StreamItem
import com.nuvio.app.features.streams.StreamsUiState
import com.nuvio.app.features.watchprogress.WatchProgressEntry

@Composable
internal fun PlayerScreenModalHosts(
    pendingP2pSwitch: PendingPlayerP2pSwitch?,
    onPendingP2pSwitchChanged: (PendingPlayerP2pSwitch?) -> Unit,
    onP2pEpisodeStreamSelected: (StreamItem, MetaVideo, Boolean) -> Unit,
    onP2pSourceStreamSelected: (StreamItem) -> Unit,
    onNextEpisodeAutoPlaySearchingChanged: (Boolean) -> Unit,
    onNextEpisodeAutoPlayCountdownChanged: (Int?) -> Unit,
    onNextEpisodeAutoPlaySourceNameChanged: (String?) -> Unit,
    showAudioModal: Boolean,
    audioTracks: List<AudioTrack>,
    selectedAudioIndex: Int,
    onAudioTrackSelected: (Int) -> Unit,
    onAudioModalDismissed: () -> Unit,
    showSubtitleModal: Boolean,
    activeSubtitleTab: SubtitleTab,
    subtitleTracks: List<SubtitleTrack>,
    selectedSubtitleIndex: Int,
    addonSubtitles: List<AddonSubtitle>,
    selectedAddonSubtitleId: String?,
    isLoadingAddonSubtitles: Boolean,
    subtitleStyle: SubtitleStyleState,
    subtitleDelayMs: Int,
    selectedAddonSubtitle: AddonSubtitle?,
    subtitleAutoSyncState: SubtitleAutoSyncUiState,
    onSubtitleTabSelected: (SubtitleTab) -> Unit,
    onBuiltInSubtitleTrackSelected: (Int) -> Unit,
    onAddonSubtitleSelected: (AddonSubtitle) -> Unit,
    onFetchAddonSubtitles: () -> Unit,
    onSubtitleStyleChanged: (SubtitleStyleState) -> Unit,
    onSubtitleDelayChanged: (Int) -> Unit,
    onSubtitleDelayReset: () -> Unit,
    onAutoSyncCapture: () -> Unit,
    onAutoSyncCueSelected: (SubtitleSyncCue) -> Unit,
    onAutoSyncReload: () -> Unit,
    onSubtitleModalDismissed: () -> Unit,
    showVideoSettingsModal: Boolean,
    playerSettings: PlayerSettingsUiState,
    onVideoSettingsChanged: () -> Unit,
    onVideoSettingsModalDismissed: () -> Unit,
    showSourcesPanel: Boolean,
    sourceStreamsState: StreamsUiState,
    activeSourceUrl: String,
    activeStreamTitle: String,
    onSourceFilterSelected: (String?) -> Unit,
    onSourceStreamSelected: (StreamItem) -> Unit,
    onReloadSources: () -> Unit,
    onSourcesPanelDismissed: () -> Unit,
    isSeries: Boolean,
    showEpisodesPanel: Boolean,
    allEpisodes: List<MetaVideo>,
    parentMetaType: String,
    parentMetaId: String,
    activeSeasonNumber: Int?,
    activeEpisodeNumber: Int?,
    watchProgressByVideoId: Map<String, WatchProgressEntry>,
    watchedKeys: Set<String>,
    blurUnwatchedEpisodes: Boolean,
    episodeStreamsPanelState: EpisodeStreamsPanelState,
    episodeStreamsRepoState: StreamsUiState,
    onEpisodeSelectedForDownload: (MetaVideo) -> Boolean,
    onEpisodeStreamsRequested: (MetaVideo) -> Unit,
    onEpisodeStreamFilterSelected: (String?) -> Unit,
    onEpisodeStreamSelected: (StreamItem, MetaVideo) -> Unit,
    onBackToEpisodes: () -> Unit,
    onReloadEpisodeStreams: () -> Unit,
    onEpisodesPanelDismissed: () -> Unit,
    showVolumeBoostModal: Boolean,
    volumeBoostDb: Int,
    onVolumeBoostChanged: (Int) -> Unit,
    onVolumeBoostModalDismissed: () -> Unit,
    stillWatchingShowDialog: Boolean,
    stillWatchingTimeoutRemaining: Int,
    onStillWatchingProceed: () -> Unit,
    onStillWatchingCancel: () -> Unit,
    showCastPicker: Boolean,
    castController: CastController?,
    onCastPickerDismissed: () -> Unit,
    showSubmitIntroModal: Boolean,
    activeVideoId: String?,
    metaUiState: MetaDetailsUiState,
    displayedPositionMs: Long,
    submitIntroSegmentType: String,
    onSubmitIntroSegmentTypeChanged: (String) -> Unit,
    submitIntroStartTimeStr: String,
    onSubmitIntroStartTimeChanged: (String) -> Unit,
    submitIntroEndTimeStr: String,
    onSubmitIntroEndTimeChanged: (String) -> Unit,
    onSubmitIntroDismissed: () -> Unit,
    onSubmitIntroSuccess: () -> Unit,
) {
    if (pendingP2pSwitch != null) {
        P2pConsentDialog(
            onEnableP2p = {
                val pending = pendingP2pSwitch
                onPendingP2pSwitchChanged(null)
                P2pSettingsRepository.setP2pEnabled(true)
                val episode = pending.episode
                if (episode != null) {
                    onP2pEpisodeStreamSelected(pending.stream, episode, pending.isAutoPlay)
                } else {
                    onP2pSourceStreamSelected(pending.stream)
                }
            },
            onDismiss = {
                if (pendingP2pSwitch.isAutoPlay) {
                    onNextEpisodeAutoPlaySearchingChanged(false)
                    onNextEpisodeAutoPlayCountdownChanged(null)
                    onNextEpisodeAutoPlaySourceNameChanged(null)
                }
                onPendingP2pSwitchChanged(null)
            },
        )
    }

    if (stillWatchingShowDialog) {
        StillWatchingDialog(
            timeoutRemaining = stillWatchingTimeoutRemaining,
            onProceed = onStillWatchingProceed,
            onCancel = onStillWatchingCancel,
        )
    }

    VolumeBoostModal(
        visible = showVolumeBoostModal,
        currentBoostDb = volumeBoostDb,
        onBoostChanged = onVolumeBoostChanged,
        onDismiss = onVolumeBoostModalDismissed,
    )

    AudioTrackModal(
        visible = showAudioModal,
        audioTracks = audioTracks,
        selectedIndex = selectedAudioIndex,
        onTrackSelected = onAudioTrackSelected,
        onDismiss = onAudioModalDismissed,
    )

    SubtitleModal(
        visible = showSubtitleModal,
        activeTab = activeSubtitleTab,
        subtitleTracks = subtitleTracks,
        selectedSubtitleIndex = selectedSubtitleIndex,
        addonSubtitles = addonSubtitles,
        selectedAddonSubtitleId = selectedAddonSubtitleId,
        isLoadingAddonSubtitles = isLoadingAddonSubtitles,
        subtitleStyle = subtitleStyle,
        subtitleDelayMs = subtitleDelayMs,
        selectedAddonSubtitle = selectedAddonSubtitle,
        subtitleAutoSyncState = subtitleAutoSyncState,
        onTabSelected = onSubtitleTabSelected,
        onBuiltInTrackSelected = onBuiltInSubtitleTrackSelected,
        onAddonSubtitleSelected = onAddonSubtitleSelected,
        onFetchAddonSubtitles = onFetchAddonSubtitles,
        onStyleChanged = onSubtitleStyleChanged,
        onSubtitleDelayChanged = onSubtitleDelayChanged,
        onSubtitleDelayReset = onSubtitleDelayReset,
        onAutoSyncCapture = onAutoSyncCapture,
        onAutoSyncCueSelected = onAutoSyncCueSelected,
        onAutoSyncReload = onAutoSyncReload,
        onDismiss = onSubtitleModalDismissed,
    )

    IosVideoSettingsModal(
        visible = showVideoSettingsModal,
        settings = playerSettings,
        onSettingsChanged = onVideoSettingsChanged,
        onDismiss = onVideoSettingsModalDismissed,
    )

    PlayerSourcesPanel(
        visible = showSourcesPanel,
        streamsUiState = sourceStreamsState,
        currentStreamUrl = activeSourceUrl,
        currentStreamName = activeStreamTitle,
        onFilterSelected = onSourceFilterSelected,
        onStreamSelected = onSourceStreamSelected,
        onReload = onReloadSources,
        onDismiss = onSourcesPanelDismissed,
    )

    if (isSeries) {
        PlayerEpisodesPanel(
            visible = showEpisodesPanel,
            episodes = allEpisodes,
            parentMetaType = parentMetaType,
            parentMetaId = parentMetaId,
            currentSeason = activeSeasonNumber,
            currentEpisode = activeEpisodeNumber,
            progressByVideoId = watchProgressByVideoId,
            watchedKeys = watchedKeys,
            blurUnwatchedEpisodes = blurUnwatchedEpisodes,
            episodeStreamsState = episodeStreamsPanelState.copy(
                streamsUiState = episodeStreamsRepoState,
            ),
            onSeasonSelected = { },
            onEpisodeSelected = { episode ->
                if (!onEpisodeSelectedForDownload(episode)) {
                    onEpisodeStreamsRequested(episode)
                }
            },
            onEpisodeStreamFilterSelected = onEpisodeStreamFilterSelected,
            onEpisodeStreamSelected = onEpisodeStreamSelected,
            onBackToEpisodes = onBackToEpisodes,
            onReloadEpisodeStreams = onReloadEpisodeStreams,
            onDismiss = onEpisodesPanelDismissed,
        )
    }

    if (showCastPicker && castController != null) {
        CastDevicePicker(
            controller = castController,
            onDismiss = onCastPickerDismissed,
        )
    }

    val season = activeSeasonNumber
    val episode = activeEpisodeNumber
    val imdbId = activeVideoId?.split(":")?.firstOrNull()?.takeIf { it.startsWith("tt") }
        ?: parentMetaId.takeIf { it.startsWith("tt") }
        ?: metaUiState.meta?.id?.takeIf { it.startsWith("tt") }

    if (showSubmitIntroModal && season != null && episode != null && !imdbId.isNullOrBlank()) {
        com.nuvio.app.features.player.skip.SubmitIntroDialog(
            imdbId = imdbId,
            season = season,
            episode = episode,
            currentTimeSec = displayedPositionMs / 1000.0,
            segmentType = submitIntroSegmentType,
            onSegmentTypeChange = onSubmitIntroSegmentTypeChanged,
            startTimeStr = submitIntroStartTimeStr,
            onStartTimeChange = onSubmitIntroStartTimeChanged,
            endTimeStr = submitIntroEndTimeStr,
            onEndTimeChange = onSubmitIntroEndTimeChanged,
            onDismiss = onSubmitIntroDismissed,
            onSuccess = onSubmitIntroSuccess,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StillWatchingDialog(
    timeoutRemaining: Int,
    onProceed: () -> Unit,
    onCancel: () -> Unit,
) {
    BasicAlertDialog(
        onDismissRequest = onCancel,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(Res.string.still_watching_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )

                Text(
                    text = stringResource(Res.string.still_watching_timeout, timeoutRemaining),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                ) {
                    TextButton(onClick = onCancel) {
                        Text(stringResource(Res.string.still_watching_cancel))
                    }
                    TextButton(onClick = onProceed) {
                        Text(
                            stringResource(Res.string.still_watching_proceed),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

internal fun selectDownloadedEpisodeForPlayback(
    parentMetaId: String,
    episode: MetaVideo,
    onDownloadedEpisodeSelected: (com.nuvio.app.features.downloads.DownloadItem, MetaVideo) -> Unit,
): Boolean {
    val downloadedEpisode = DownloadsRepository.findPlayableDownload(
        parentMetaId = parentMetaId,
        seasonNumber = episode.season,
        episodeNumber = episode.episode,
        videoId = episode.id,
    )
    if (downloadedEpisode != null) {
        onDownloadedEpisodeSelected(downloadedEpisode, episode)
        return true
    }
    return false
}
