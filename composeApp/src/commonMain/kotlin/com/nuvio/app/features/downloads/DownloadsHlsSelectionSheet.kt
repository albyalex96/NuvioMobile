package com.nuvio.app.features.downloads

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.nuvio.app.core.ui.rememberNuvioBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nuvio.app.core.ui.NuvioDropdownChip
import com.nuvio.app.core.ui.NuvioDropdownOption
import com.nuvio.app.core.ui.NuvioModalBottomSheet
import com.nuvio.app.core.ui.NuvioPrimaryButton
import com.nuvio.app.core.ui.dismissNuvioBottomSheet
import com.nuvio.app.core.ui.nuvioSafeBottomPadding
import com.nuvio.app.features.player.getLanguageLabelForCode
import com.nuvio.app.features.player.languageLabelForCode
import com.nuvio.app.features.streams.StreamItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.download_failed
import nuvio.composeapp.generated.resources.downloads_hls_audio
import nuvio.composeapp.generated.resources.downloads_hls_download
import nuvio.composeapp.generated.resources.downloads_hls_fetching
import nuvio.composeapp.generated.resources.downloads_hls_quality
import nuvio.composeapp.generated.resources.downloads_hls_select_variant
import nuvio.composeapp.generated.resources.downloads_hls_subtitles
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsHlsSelectionSheet(
    stream: StreamItem?,
    onDismiss: () -> Unit,
    onDownload: (HlsDownloadSelection) -> Unit,
) {
    if (stream == null) return

    val sheetState = rememberNuvioBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    var hlsMetadata by remember { mutableStateOf<HlsStreamMetadata?>(null) }
    var isFetching by remember { mutableStateOf(true) }
    var fetchError by remember { mutableStateOf(false) }

    LaunchedEffect(stream) {
        isFetching = true
        fetchError = false
        val metadata = withContext(Dispatchers.Default) {
            DownloadsRepository.fetchHlsMasterPlaylist(stream)
        }
        hlsMetadata = metadata
        isFetching = false
        if (metadata == null) {
            fetchError = true
        }
    }

    val playlist = hlsMetadata?.masterPlaylist

    val qualityOptions = remember(playlist) {
        playlist?.variants?.mapIndexed { _, variant ->
            val label = buildString {
                variant.resolution?.let { append(it) }
                if (variant.resolution != null) append(" • ")
                append(formatBandwidth(variant.bandwidth))
                variant.codecs?.let { codecs ->
                    val codecLabel = friendlyCodec(codecs)
                    if (codecLabel.isNotBlank()) {
                        append(" • ")
                        append(codecLabel)
                    }
                }
            }
            NuvioDropdownOption(
                key = variant.url,
                label = label,
            )
        }.orEmpty()
    }

    val audioTracks = remember(playlist) {
        playlist?.audioTracks?.filter { it.uri != null }.orEmpty()
    }
    val subtitleTracks = remember(playlist) {
        playlist?.subtitleTracks?.filter { it.uri != null }.orEmpty()
    }

    var selectedQualityKey by remember(qualityOptions) {
        mutableStateOf(qualityOptions.firstOrNull()?.key)
    }
    var selectedAudioKeys by remember(audioTracks) {
        mutableStateOf(audioTracks.filter { it.isDefault }.mapNotNull { it.uri }.toSet())
    }
    var selectedSubtitleKeys by remember(subtitleTracks) {
        mutableStateOf(setOf<String>())
    }

    val hasAudio = audioTracks.isNotEmpty()
    val hasSubtitles = subtitleTracks.isNotEmpty()
    val canDownload = qualityOptions.isNotEmpty()

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
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(Res.string.downloads_hls_select_variant),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            when {
                fetchError -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(Res.string.download_failed),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                isFetching || qualityOptions.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.5.dp,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(Res.string.downloads_hls_fetching),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = stringResource(Res.string.downloads_hls_quality),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        NuvioDropdownChip(
                            title = stringResource(Res.string.downloads_hls_quality),
                            label = qualityOptions.firstOrNull { it.key == selectedQualityKey }?.label.orEmpty(),
                            selectedKey = selectedQualityKey,
                            options = qualityOptions,
                            onSelected = { option ->
                                selectedQualityKey = option.key
                            },
                        )

                        if (hasAudio) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(Res.string.downloads_hls_audio),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            val audioOptions = mutableListOf<NuvioDropdownOption>()
                            val audioSelectedLabels = mutableListOf<String>()
                            for (track in audioTracks) {
                                val langLabel = languageLabelForCode(track.language)
                                if (track.uri in selectedAudioKeys) {
                                    audioSelectedLabels.add(langLabel)
                                }
                                audioOptions.add(
                                    NuvioDropdownOption(
                                        key = track.uri!!,
                                        label = buildString {
                                            append(langLabel)
                                            if (track.name.isNotBlank() && !track.name.equals(track.language, ignoreCase = true) && !track.name.equals(langLabel, ignoreCase = true)) {
                                                append(" • ")
                                                append(track.name)
                                            }
                                        },
                                    )
                                )
                            }
                            val audioChipLabel = audioSelectedLabels.joinToString(", ")
                            NuvioDropdownChip(
                                title = stringResource(Res.string.downloads_hls_audio),
                                label = audioChipLabel,
                                options = audioOptions,
                                multiple = true,
                                selectedKeys = selectedAudioKeys,
                                onSelectedMultiple = { keys -> selectedAudioKeys = keys },
                            )
                        }

                        if (hasSubtitles) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(Res.string.downloads_hls_subtitles),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            val subtitleOptions = mutableListOf<NuvioDropdownOption>()
                            val subtitleSelectedLabels = mutableListOf<String>()
                            for (track in subtitleTracks) {
                                val langLabel = languageLabelForCode(track.language)
                                if (track.uri in selectedSubtitleKeys) {
                                    subtitleSelectedLabels.add(langLabel)
                                }
                                subtitleOptions.add(
                                    NuvioDropdownOption(
                                        key = track.uri!!,
                                        label = buildString {
                                            append(langLabel)
                                            if (track.name.isNotBlank() && !track.name.equals(track.language, ignoreCase = true) && !track.name.equals(langLabel, ignoreCase = true)) {
                                                append(" • ")
                                                append(track.name)
                                            }
                                        },
                                    )
                                )
                            }
                            val subtitleChipLabel = subtitleSelectedLabels.joinToString(", ")
                            NuvioDropdownChip(
                                title = stringResource(Res.string.downloads_hls_subtitles),
                                label = subtitleChipLabel,
                                options = subtitleOptions,
                                multiple = true,
                                selectedKeys = selectedSubtitleKeys,
                                onSelectedMultiple = { keys -> selectedSubtitleKeys = keys },
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        NuvioPrimaryButton(
                            text = stringResource(Res.string.downloads_hls_download),
                            enabled = canDownload,
                            onClick = {
                                coroutineScope.launch {
                                    val selectedAudioTracks = audioTracks.filter { it.uri in selectedAudioKeys }
                                    val selectedSubtitleTracks = subtitleTracks.filter { it.uri in selectedSubtitleKeys }
                                    val audioLabels = selectedAudioTracks.map { getLanguageLabelForCode(it.language) }
                                    val subtitleLabels = selectedSubtitleTracks.map { getLanguageLabelForCode(it.language) }
                                    val selectedVariant = playlist?.variants?.firstOrNull {
                                        it.url == selectedQualityKey
                                    }
                                    val selection = HlsDownloadSelection(
                                        variantUrl = selectedQualityKey.orEmpty(),
                                        audioUrls = selectedAudioKeys.toList(),
                                        subtitleUrls = selectedSubtitleKeys.toList(),
                                        audioLabels = audioLabels,
                                        subtitleLabels = subtitleLabels,
                                        displayQuality = selectedVariant?.resolution
                                            ?: formatBandwidth(selectedVariant?.bandwidth ?: 0L),
                                        displayAudio = audioLabels.joinToString(", "),
                                        displaySubtitle = subtitleLabels.joinToString(", "),
                                    )
                                    dismissNuvioBottomSheet(sheetState = sheetState, onDismiss = {
                                        onDownload(selection)
                                    })
                                }
                            },
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

private fun formatBandwidth(bandwidth: Long): String {
    if (bandwidth <= 0L) return ""
    val mbps = bandwidth.toDouble() / 1_000_000.0
    return "${"%.1f".format(mbps).trimEnd('0').trimEnd('.')} Mbps"
}

private fun friendlyCodec(codecs: String): String {
    val lower = codecs.lowercase()
    return when {
        lower.contains("av01") || lower.contains("av1") -> "AV1"
        lower.contains("hev1") || lower.contains("hvc1") || lower.contains("hevc") -> "HEVC"
        lower.contains("avc1") || lower.contains("h264") -> "H.264"
        lower.contains("vp9") -> "VP9"
        lower.contains("vp8") -> "VP8"
        else -> ""
    }
}
