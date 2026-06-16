package com.nuvio.app.features.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import com.nuvio.app.R
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.anilist_logo
import nuvio.composeapp.generated.resources.introdb_favicon
import nuvio.composeapp.generated.resources.kitsu_logo
import nuvio.composeapp.generated.resources.myanimelist_logo
import nuvio.composeapp.generated.resources.opensubtitles_favicon
import nuvio.composeapp.generated.resources.rating_tmdb
import nuvio.composeapp.generated.resources.simkl_logo
import nuvio.composeapp.generated.resources.subdl_favicon
import org.jetbrains.compose.resources.painterResource as composePainterResource

@Composable
internal actual fun integrationLogoPainter(logo: IntegrationLogo): Painter =
    when (logo) {
        IntegrationLogo.Tmdb -> composePainterResource(Res.drawable.rating_tmdb)
        IntegrationLogo.Trakt -> painterResource(id = R.drawable.trakt_tv_favicon)
        IntegrationLogo.MdbList -> painterResource(id = R.drawable.mdblist_logo)
        IntegrationLogo.IntroDb -> composePainterResource(Res.drawable.introdb_favicon)
        IntegrationLogo.Mal -> composePainterResource(Res.drawable.myanimelist_logo)
        IntegrationLogo.Kitsu -> composePainterResource(Res.drawable.kitsu_logo)
        IntegrationLogo.Anilist -> composePainterResource(Res.drawable.anilist_logo)
        IntegrationLogo.Simkl -> composePainterResource(Res.drawable.simkl_logo)
        IntegrationLogo.OpenSubtitles -> composePainterResource(Res.drawable.opensubtitles_favicon)
        IntegrationLogo.Subdl -> composePainterResource(Res.drawable.subdl_favicon)
    }
