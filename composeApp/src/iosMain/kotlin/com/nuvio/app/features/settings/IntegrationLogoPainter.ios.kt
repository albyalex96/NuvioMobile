package com.nuvio.app.features.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.anilist_logo
import nuvio.composeapp.generated.resources.introdb_favicon
import nuvio.composeapp.generated.resources.kitsu_logo
import nuvio.composeapp.generated.resources.mdblist_logo
import nuvio.composeapp.generated.resources.myanimelist_logo
import nuvio.composeapp.generated.resources.opensubtitles_favicon
import nuvio.composeapp.generated.resources.rating_tmdb
import nuvio.composeapp.generated.resources.simkl_logo
import nuvio.composeapp.generated.resources.subdl_favicon
import nuvio.composeapp.generated.resources.trakt_tv_favicon
import org.jetbrains.compose.resources.painterResource

@Composable
internal actual fun integrationLogoPainter(logo: IntegrationLogo): Painter =
    when (logo) {
        IntegrationLogo.Tmdb -> painterResource(Res.drawable.rating_tmdb)
        IntegrationLogo.Trakt -> painterResource(Res.drawable.trakt_tv_favicon)
        IntegrationLogo.MdbList -> painterResource(Res.drawable.mdblist_logo)
        IntegrationLogo.IntroDb -> painterResource(Res.drawable.introdb_favicon)
        IntegrationLogo.Mal -> painterResource(Res.drawable.myanimelist_logo)
        IntegrationLogo.Kitsu -> painterResource(Res.drawable.kitsu_logo)
        IntegrationLogo.Anilist -> painterResource(Res.drawable.anilist_logo)
        IntegrationLogo.Simkl -> painterResource(Res.drawable.simkl_logo)
        IntegrationLogo.OpenSubtitles -> painterResource(Res.drawable.opensubtitles_favicon)
        IntegrationLogo.Subdl -> painterResource(Res.drawable.subdl_favicon)
    }
