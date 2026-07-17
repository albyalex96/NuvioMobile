package com.nuvio.app.features.settings

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.res.ResourcesCompat
import com.nuvio.app.R

@Composable
actual fun AppIconPreview(iconId: String, modifier: Modifier) {
    val resId = when (iconId) {
        "default" -> R.mipmap.ic_launcher
        "enhanced" -> R.mipmap.ic_launcher_alt_enhanced
        "monochrome" -> R.mipmap.ic_launcher_alt_monochrome
        "neon" -> R.mipmap.ic_launcher_alt_neon
        "gear" -> R.mipmap.ic_launcher_alt_gear
        "chrome" -> R.mipmap.ic_launcher_alt_chrome
        "aurora" -> R.mipmap.ic_launcher_alt_aurora
        "emerald" -> R.mipmap.ic_launcher_alt_emerald
        else -> return
    }
    val ctx = LocalContext.current
    val imageBitmap = remember(resId, ctx) {
        val drawable = ResourcesCompat.getDrawable(ctx.resources, resId, ctx.theme)
            ?: return@remember null
        val size = drawable.intrinsicWidth.coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        bitmap.asImageBitmap()
    }
    if (imageBitmap != null) {
        Image(
            bitmap = imageBitmap,
            contentDescription = iconId,
            modifier = modifier,
            contentScale = ContentScale.Fit,
        )
    }
}
