package com.nuvio.app.features.settings

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.app_icon_aurora_preview
import nuvio.composeapp.generated.resources.app_icon_chrome_preview
import nuvio.composeapp.generated.resources.app_icon_default_preview
import nuvio.composeapp.generated.resources.app_icon_emerald_preview
import nuvio.composeapp.generated.resources.app_icon_enhanced_preview
import nuvio.composeapp.generated.resources.app_icon_gear_preview
import nuvio.composeapp.generated.resources.app_icon_monochrome_preview
import nuvio.composeapp.generated.resources.app_icon_neon_preview
import org.jetbrains.compose.resources.painterResource

@Composable
actual fun AppIconPreview(iconId: String, modifier: Modifier) {
    val res = when (iconId) {
        "default" -> Res.drawable.app_icon_default_preview
        "enhanced" -> Res.drawable.app_icon_enhanced_preview
        "monochrome" -> Res.drawable.app_icon_monochrome_preview
        "neon" -> Res.drawable.app_icon_neon_preview
        "gear" -> Res.drawable.app_icon_gear_preview
        "chrome" -> Res.drawable.app_icon_chrome_preview
        "aurora" -> Res.drawable.app_icon_aurora_preview
        "emerald" -> Res.drawable.app_icon_emerald_preview
        else -> return
    }
    Image(
        painter = painterResource(res),
        contentDescription = iconId,
        modifier = modifier,
        contentScale = ContentScale.Fit,
    )
}
