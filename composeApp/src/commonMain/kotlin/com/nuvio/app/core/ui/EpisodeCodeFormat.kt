package com.nuvio.app.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.features.player.PlayerSettingsRepository

enum class EpisodeCodeFormat(val key: String) {
    S01E01("S01E01"),
    S1E1("S1E1"),
    X01x01("01x01"),
    X1x1("1x1"),
}

fun formatEpisodeCode(season: Int, episode: Int, format: EpisodeCodeFormat): String = when (format) {
    EpisodeCodeFormat.S01E01 -> "S${season.toString().padStart(2, '0')}E${episode.toString().padStart(2, '0')}"
    EpisodeCodeFormat.S1E1 -> "S${season}E${episode}"
    EpisodeCodeFormat.X01x01 -> "${season.toString().padStart(2, '0')}x${episode.toString().padStart(2, '0')}"
    EpisodeCodeFormat.X1x1 -> "${season}x${episode}"
}

fun formatEpisodeCodeWithTitle(season: Int, episode: Int, title: String, format: EpisodeCodeFormat): String =
    "${formatEpisodeCode(season, episode, format)} • $title"

@Composable
fun rememberEpisodeCodeFormat(): EpisodeCodeFormat {
    val state by PlayerSettingsRepository.uiState.collectAsStateWithLifecycle()
    return state.episodeCodeFormat
}
