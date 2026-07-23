package com.nuvio.app.features.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter

internal enum class IntegrationLogo {
    Tmdb,
    Trakt,
    MdbList,
    IntroDb,
    Mal,
    Kitsu,
    Anilist,
    Simkl,
    OpenSubtitles,
    Subdl,
    Telegram,
}

@Composable
internal expect fun integrationLogoPainter(logo: IntegrationLogo): Painter
