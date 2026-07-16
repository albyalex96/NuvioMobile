package com.nuvio.app.core.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.theme_color_blue
import nuvio.composeapp.generated.resources.theme_color_cyan
import nuvio.composeapp.generated.resources.theme_color_green
import nuvio.composeapp.generated.resources.theme_color_pink
import nuvio.composeapp.generated.resources.theme_color_purple
import nuvio.composeapp.generated.resources.theme_color_red
import nuvio.composeapp.generated.resources.theme_color_white
import nuvio.composeapp.generated.resources.theme_color_yellow
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import org.jetbrains.compose.resources.StringResource

enum class ThemeAccentColor(
    val color: Color,
    val labelRes: StringResource,
) {
    PINK(Color(0xFFFF5F9E), Res.string.theme_color_pink),
    PURPLE(Color(0xFF9B5CFF), Res.string.theme_color_purple),
    BLUE(Color(0xFF397CFF), Res.string.theme_color_blue),
    CYAN(Color(0xFF35D6E8), Res.string.theme_color_cyan),
    GREEN(Color(0xFF57D67A), Res.string.theme_color_green),
    YELLOW(Color(0xFFFFD447), Res.string.theme_color_yellow),
    RED(Color(0xFFFF5263), Res.string.theme_color_red),
    WHITE(Color(0xFFF4F7FF), Res.string.theme_color_white),
}

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

    val Messenger = ThemeColorPalette(
        secondary = Color(0xFF168AFF),
        secondaryVariant = Color(0xFF0072FF),
        nativeAccentHex = "#168AFF",
        focusRing = Color(0xFF38BDF8),
        focusBackground = Color(0xFF132B45),
        background = Color(0xFF090E15),
        backgroundElevated = Color(0xFF131A23),
        backgroundCard = Color(0xFF192432),
    )

    val Amethyst = ThemeColorPalette(
        secondary = Color(0xFF8B5CF6),
        secondaryVariant = Color(0xFF7C3AED),
        nativeAccentHex = "#8B5CF6",
        focusRing = Color(0xFFA855F7),
        focusBackground = Color(0xFF281A3D),
        background = Color(0xFF0F0B14),
        backgroundElevated = Color(0xFF1B1522),
        backgroundCard = Color(0xFF251B2F),
    )

    val Blossom = ThemeColorPalette(
        secondary = Color(0xFFEC4899),
        secondaryVariant = Color(0xFFDB2777),
        nativeAccentHex = "#EC4899",
        focusRing = Color(0xFFF472B6),
        focusBackground = Color(0xFF3D1830),
        background = Color(0xFF120A10),
        backgroundElevated = Color(0xFF21141D),
        backgroundCard = Color(0xFF2D1926),
    )

    val Lagoon = ThemeColorPalette(
        secondary = Color(0xFF14B8A6),
        secondaryVariant = Color(0xFF0891B2),
        nativeAccentHex = "#14B8A6",
        focusRing = Color(0xFF2DD4BF),
        focusBackground = Color(0xFF123A3A),
        background = Color(0xFF081110),
        backgroundElevated = Color(0xFF121E1D),
        backgroundCard = Color(0xFF182A28),
    )

    val Sunset = ThemeColorPalette(
        secondary = Color(0xFFF97316),
        secondaryVariant = Color(0xFFF43F5E),
        nativeAccentHex = "#F97316",
        focusRing = Color(0xFFFB7185),
        focusBackground = Color(0xFF402019),
        background = Color(0xFF120C09),
        backgroundElevated = Color(0xFF211713),
        backgroundCard = Color(0xFF2D1E18),
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

    fun getColorPalette(
        theme: AppTheme,
        customFirst: Color = ThemeAccentColor.PINK.color,
        customSecond: Color = ThemeAccentColor.CYAN.color,
    ): ThemeColorPalette {
        val accessibleFirst = customFirst.toAccessibleAccent()
        val accessibleSecond = customSecond.toAccessibleAccent()
        return when (theme) {
            AppTheme.CRIMSON -> Crimson
            AppTheme.OCEAN -> Ocean
            AppTheme.VIOLET -> Violet
            AppTheme.EMERALD -> Emerald
            AppTheme.AMBER -> Amber
            AppTheme.ROSE -> Rose
            AppTheme.MESSENGER -> Messenger
            AppTheme.AMETHYST -> Amethyst
            AppTheme.BLOSSOM -> Blossom
            AppTheme.LAGOON -> Lagoon
            AppTheme.SUNSET -> Sunset
            AppTheme.CUSTOM -> ThemeColorPalette(
                secondary = accessibleFirst,
                secondaryVariant = accessibleSecond,
                nativeAccentHex = accessibleFirst.toThemeHex(),
                onSecondary = Color.White,
                onSecondaryVariant = Color.White,
                focusRing = accessibleSecond,
                focusBackground = Color(0xFF1A1540),
                background = Color(0xFF0E0C12),
                backgroundElevated = Color(0xFF1A1720),
                backgroundCard = Color(0xFF231D2A),
            )
            AppTheme.WHITE -> White
        }
    }

    fun animatedColors(
        theme: AppTheme,
        customFirst: Color = ThemeAccentColor.PINK.color,
        customSecond: Color = ThemeAccentColor.CYAN.color,
    ): List<Color> = when (theme) {
        AppTheme.MESSENGER -> listOf(
            Color(0xFF006DFF),
            Color(0xFF168AFF),
            Color(0xFF00A8FF),
            Color(0xFF38BDF8),
            Color(0xFF5CC8FF),
            Color(0xFF8AD8FF),
        )
        AppTheme.AMETHYST -> listOf(
            Color(0xFF6D28D9),
            Color(0xFF7C3AED),
            Color(0xFF9333EA),
            Color(0xFFA855F7),
            Color(0xFFC084FC),
            Color(0xFFD8B4FE),
        )
        AppTheme.BLOSSOM -> listOf(
            Color(0xFFDB2777),
            Color(0xFFEC4899),
            Color(0xFFF472B6),
            Color(0xFFFDA4AF),
            Color(0xFFF43F7D),
            Color(0xFFFB7185),
        )
        AppTheme.LAGOON -> listOf(
            Color(0xFF0891B2),
            Color(0xFF06B6D4),
            Color(0xFF22D3EE),
            Color(0xFF14B8A6),
            Color(0xFF2DD4BF),
            Color(0xFF5EEAD4),
        )
        AppTheme.SUNSET -> listOf(
            Color(0xFFF43F5E),
            Color(0xFFFB7185),
            Color(0xFFFDA4AF),
            Color(0xFFF97316),
            Color(0xFFFB923C),
            Color(0xFFFDBA74),
        )
        AppTheme.CUSTOM -> listOf(
            customFirst,
            lerp(customFirst, Color.Black, 0.14f),
            lerp(customFirst, customSecond, 0.30f),
            customSecond,
            lerp(customSecond, Color.White, 0.12f),
            lerp(customSecond, customFirst, 0.28f),
        )
        else -> emptyList()
    }
}

internal fun Color.toThemeHex(): String {
    fun channel(value: Float): String =
        (value.coerceIn(0f, 1f) * 255f).toInt().toString(16).padStart(2, '0').uppercase()
    return "#${channel(red)}${channel(green)}${channel(blue)}"
}

internal fun Color.toAccessibleAccent(): Color {
    var adjusted = copy(alpha = 1f)
    repeat(12) {
        adjusted = when {
            adjusted.luminance() < AccessibleAccentMinLuminance -> lerp(adjusted, Color.White, 0.12f)
            adjusted.luminance() > AccessibleAccentMaxLuminance -> lerp(adjusted, Color.Black, 0.12f)
            else -> return adjusted
        }
    }
    return adjusted
}

internal fun String?.toThemeColor(fallback: Color): Color {
    val value = this?.trim().orEmpty()
    ThemeAccentColor.entries.firstOrNull { it.name == value }?.let { return it.color }
    val hex = value.removePrefix("#")
    if (hex.length != 6 || hex.any { !it.isDigit() && it.lowercaseChar() !in 'a'..'f' }) return fallback
    return runCatching {
        Color(
            red = hex.substring(0, 2).toInt(16) / 255f,
            green = hex.substring(2, 4).toInt(16) / 255f,
            blue = hex.substring(4, 6).toInt(16) / 255f,
            alpha = 1f,
        )
    }.getOrDefault(fallback)
}

private const val AccessibleAccentMinLuminance = 0.12f
private const val AccessibleAccentMaxLuminance = 0.24f
