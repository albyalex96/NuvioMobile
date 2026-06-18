package com.nuvio.app.core.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

data class ThemeColorPalette(
    val secondary: Color,
    val secondaryVariant: Color,
    val nativeAccentHex: String,
    val onSecondary: Color = Color.White,
    val onSecondaryVariant: Color = Color.White,
    val focusRing: Color,
    val focusBackground: Color,
    val background: Color = Color(0xFF0D0D0D),
    val backgroundElevated: Color = Color(0xFF1A1A1A),
    val backgroundCard: Color = Color(0xFF242424),
)

fun Color.toRgbHex(): String {
    fun component(value: Float): String =
        (value * 255f).roundToInt().coerceIn(0, 255).toString(16).padStart(2, '0').uppercase()
    return "#${component(red)}${component(green)}${component(blue)}"
}

fun parseHexColor(hex: String): Color? {
    val normalized = hex
        .trim()
        .removePrefix("#")
        .takeIf { it.length == 6 || it.length == 8 }
        ?: return null
    val argb = if (normalized.length == 6) "FF$normalized" else normalized
    val parsed = argb.toLongOrNull(16) ?: return null
    return Color(
        red = ((parsed shr 16) and 0xFF).toFloat() / 255f,
        green = ((parsed shr 8) and 0xFF).toFloat() / 255f,
        blue = (parsed and 0xFF).toFloat() / 255f,
        alpha = ((parsed shr 24) and 0xFF).toFloat() / 255f,
    )
}

fun Color.toHsv(): FloatArray {
    val r = red; val g = green; val b = blue
    val max = maxOf(r, g, b); val min = minOf(r, g, b)
    val delta = max - min
    var hue = 0f
    var saturation = 0f
    val value = max
    if (delta > 0.0001f) {
        saturation = delta / max
        val dr = (((max - r) / 6f) + (delta / 2f)) / delta
        val dg = (((max - g) / 6f) + (delta / 2f)) / delta
        val db = (((max - b) / 6f) + (delta / 2f)) / delta
        hue = when {
            r >= g && r >= b -> db - dg
            g >= r && g >= b -> (1f / 3f) + dr - db
            else -> (2f / 3f) + dg - dr
        }
        if (hue < 0f) hue += 1f
        if (hue > 1f) hue -= 1f
    }
    return floatArrayOf(hue * 360f, saturation * 100f, value * 100f)
}

fun Color.Companion.fromHsv(hueDegrees: Float, saturationPercent: Float, valuePercent: Float): Color {
    val h = (hueDegrees % 360f) / 360f
    val s = (saturationPercent.coerceIn(0f, 100f)) / 100f
    val v = (valuePercent.coerceIn(0f, 100f)) / 100f
    if (s < 0.0001f) return Color(v, v, v)
    val i = (h * 6f).toInt()
    val f = h * 6f - i
    val p = v * (1f - s)
    val q = v * (1f - s * f)
    val t = v * (1f - s * (1f - f))
    val (r, g, b) = when (i % 6) {
        0 -> Triple(v, t, p)
        1 -> Triple(q, v, p)
        2 -> Triple(p, v, t)
        3 -> Triple(p, q, v)
        4 -> Triple(t, p, v)
        else -> Triple(v, p, q)
    }
    return Color(r.coerceIn(0f, 1f), g.coerceIn(0f, 1f), b.coerceIn(0f, 1f))
}

object ThemeColors {

    val Crimson = ThemeColorPalette(
        secondary = Color(0xFFE53935),
        secondaryVariant = Color(0xFFC62828),
        nativeAccentHex = "#E53935",
        focusRing = Color(0xFFFF5252),
        focusBackground = Color(0xFF3D1A1A),
        background = Color(0xFF0D0D0D),
        backgroundElevated = Color(0xFF1A1A1A),
        backgroundCard = Color(0xFF241A1A),
    )

    val Ocean = ThemeColorPalette(
        secondary = Color(0xFF1E88E5),
        secondaryVariant = Color(0xFF1565C0),
        nativeAccentHex = "#1E88E5",
        focusRing = Color(0xFF42A5F5),
        focusBackground = Color(0xFF1A2D3D),
        background = Color(0xFF0D0D0F),
        backgroundElevated = Color(0xFF1A1A1E),
        backgroundCard = Color(0xFF1A1F24),
    )

    val Violet = ThemeColorPalette(
        secondary = Color(0xFF8E24AA),
        secondaryVariant = Color(0xFF6A1B9A),
        nativeAccentHex = "#8E24AA",
        focusRing = Color(0xFFAB47BC),
        focusBackground = Color(0xFF2D1A3D),
        background = Color(0xFF0D0D0F),
        backgroundElevated = Color(0xFF1A1A1E),
        backgroundCard = Color(0xFF1F1A24),
    )

    val Emerald = ThemeColorPalette(
        secondary = Color(0xFF43A047),
        secondaryVariant = Color(0xFF2E7D32),
        nativeAccentHex = "#43A047",
        focusRing = Color(0xFF66BB6A),
        focusBackground = Color(0xFF1A3D1E),
        background = Color(0xFF0D0D0D),
        backgroundElevated = Color(0xFF1A1A1A),
        backgroundCard = Color(0xFF1A241A),
    )

    val Amber = ThemeColorPalette(
        secondary = Color(0xFFFB8C00),
        secondaryVariant = Color(0xFFEF6C00),
        nativeAccentHex = "#FB8C00",
        focusRing = Color(0xFFFFA726),
        focusBackground = Color(0xFF3D2D1A),
        background = Color(0xFF0F0D0D),
        backgroundElevated = Color(0xFF1E1A1A),
        backgroundCard = Color(0xFF24201A),
    )

    val Rose = ThemeColorPalette(
        secondary = Color(0xFFD81B60),
        secondaryVariant = Color(0xFFC2185B),
        nativeAccentHex = "#D81B60",
        focusRing = Color(0xFFEC407A),
        focusBackground = Color(0xFF3D1A2D),
        background = Color(0xFF0D0D0D),
        backgroundElevated = Color(0xFF1A1A1A),
        backgroundCard = Color(0xFF241A1F),
    )

    val White = ThemeColorPalette(
        secondary = Color(0xFFF5F5F5),
        secondaryVariant = Color(0xFFE0E0E0),
        nativeAccentHex = "#F5F5F5",
        onSecondary = Color(0xFF111111),
        onSecondaryVariant = Color(0xFF111111),
        focusRing = Color(0xFFFFFFFF),
        focusBackground = Color(0xFF303030),
        background = Color(0xFF0D0D0D),
        backgroundElevated = Color(0xFF1A1A1A),
        backgroundCard = Color(0xFF222222),
    )

    fun customColorPalette(accentHex: String): ThemeColorPalette {
        val color = parseHexColor(accentHex) ?: Ocean.secondary
        val hsv = color.toHsv()
        val variantColor = Color.fromHsv(hsv[0], hsv[1], (hsv[2] * 0.8f).coerceAtLeast(5f))
        val focusRingColor = Color.fromHsv(hsv[0], (hsv[1] * 0.7f).coerceAtMost(100f), min(hsv[2] * 1.3f, 100f))
        val onColor = if (color.luminance() > 0.5f) Color(0xFF111111) else Color.White
        val bgR = (0.05f + color.red * 0.15f).coerceAtMost(0.25f)
        val bgG = (0.05f + color.green * 0.15f).coerceAtMost(0.25f)
        val bgB = (0.05f + color.blue * 0.15f).coerceAtMost(0.25f)
        val focusBg = Color(bgR, bgG, bgB)
        return ThemeColorPalette(
            secondary = color,
            secondaryVariant = variantColor,
            nativeAccentHex = accentHex,
            onSecondary = onColor,
            onSecondaryVariant = onColor,
            focusRing = focusRingColor,
            focusBackground = focusBg,
            background = Color(0xFF0D0D0D),
            backgroundElevated = Color(0xFF1A1A1A),
            backgroundCard = Color(bgR * 1.2f, bgG * 1.2f, bgB * 1.2f),
        )
    }

    fun getColorPalette(theme: AppTheme, customAccentHex: String? = null): ThemeColorPalette = when (theme) {
        AppTheme.CRIMSON -> Crimson
        AppTheme.OCEAN -> Ocean
        AppTheme.VIOLET -> Violet
        AppTheme.EMERALD -> Emerald
        AppTheme.AMBER -> Amber
        AppTheme.ROSE -> Rose
        AppTheme.WHITE -> White
        AppTheme.CUSTOM -> customColorPalette(customAccentHex ?: Ocean.nativeAccentHex)
    }
}
