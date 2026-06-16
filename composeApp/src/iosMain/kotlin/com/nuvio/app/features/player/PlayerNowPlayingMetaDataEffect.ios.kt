package com.nuvio.app.features.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.nuvio.app.core.ui.formatEpisodeCode
import com.nuvio.app.core.ui.rememberEpisodeCodeFormat

@Composable
internal actual fun PlayerScreenRuntime.PlayerNowPlayingMetaDataEffect() {
    val episodeCodeFormat = rememberEpisodeCodeFormat()

    val nowPlayingTitle = remember(title) {
        title.trim().takeIf { it.isNotEmpty() } ?: "Nuvio"
    }

    val nowPlayingSubtitle = remember(
        activeSeasonNumber,
        activeEpisodeNumber,
        activeEpisodeTitle,
        episodeCodeFormat,
    ) {
        buildNowPlayingSubtitle(
            seasonNumber = activeSeasonNumber,
            episodeNumber = activeEpisodeNumber,
            episodeTitle = activeEpisodeTitle,
            episodeCodeFormat = episodeCodeFormat,
        )
    }

    LaunchedEffect(
        playerController,
        nowPlayingTitle,
        nowPlayingSubtitle,
        poster,
        activeEpisodeThumbnail,
    ) {
        val controller = playerController ?: return@LaunchedEffect

        val artworkUrl = activeEpisodeThumbnail
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: poster
                ?.trim()
                ?.takeIf { it.isNotEmpty() }

        controller.updateNowPlayingMetadata(
            PlayerNowPlayingInfo(
                title = nowPlayingTitle,
                subtitle = nowPlayingSubtitle,
                artworkUrl = artworkUrl,
            ),
        )
    }
}

private fun buildNowPlayingSubtitle(
    seasonNumber: Int?,
    episodeNumber: Int?,
    episodeTitle: String?,
    episodeCodeFormat: com.nuvio.app.core.ui.EpisodeCodeFormat,
): String? {
    val episodePrefix = when {
        seasonNumber != null && episodeNumber != null ->
            formatEpisodeCode(seasonNumber, episodeNumber, episodeCodeFormat)
        episodeNumber != null ->
            "E${episodeNumber.toString().padStart(2, '0')}"
        else -> null
    }

    val episodeName = episodeTitle
        ?.trim()
        ?.takeIf { it.isNotEmpty() }

    return listOfNotNull(episodePrefix, episodeName)
        .distinct()
        .joinToString(" • ")
        .takeIf { it.isNotEmpty() }
}
