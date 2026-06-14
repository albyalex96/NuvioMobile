package com.nuvio.app.features.player.sponsorblock

import androidx.compose.runtime.Composable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.sponsorblock_category_filler
import nuvio.composeapp.generated.resources.sponsorblock_category_interaction
import nuvio.composeapp.generated.resources.sponsorblock_category_intro
import nuvio.composeapp.generated.resources.sponsorblock_category_music_offtopic
import nuvio.composeapp.generated.resources.sponsorblock_category_outro
import nuvio.composeapp.generated.resources.sponsorblock_category_preview
import nuvio.composeapp.generated.resources.sponsorblock_category_selfpromo
import nuvio.composeapp.generated.resources.sponsorblock_category_sponsor
import org.jetbrains.compose.resources.stringResource

/**
 * Represents the categories of segments that SponsorBlock can identify.
 */
enum class SponsorBlockCategory(val apiValue: String) {
    SPONSOR("sponsor"),
    SELFPROMO("selfpromo"),
    INTERACTION("interaction"),
    INTRO("intro"),
    OUTRO("outro"),
    PREVIEW("preview"),
    MUSIC_OFFTOPIC("music_offtopic"),
    FILLER("filler"),
    ;

    @Composable
    fun displayLabel(): String = when (this) {
        SPONSOR -> stringResource(Res.string.sponsorblock_category_sponsor)
        SELFPROMO -> stringResource(Res.string.sponsorblock_category_selfpromo)
        INTERACTION -> stringResource(Res.string.sponsorblock_category_interaction)
        INTRO -> stringResource(Res.string.sponsorblock_category_intro)
        OUTRO -> stringResource(Res.string.sponsorblock_category_outro)
        PREVIEW -> stringResource(Res.string.sponsorblock_category_preview)
        MUSIC_OFFTOPIC -> stringResource(Res.string.sponsorblock_category_music_offtopic)
        FILLER -> stringResource(Res.string.sponsorblock_category_filler)
    }

    companion object {
        /** Default categories that most users want to skip. */
        val DEFAULT_CATEGORIES = listOf(SPONSOR, SELFPROMO, INTERACTION, INTRO, OUTRO, PREVIEW)

        /** All available categories. */
        val ALL_CATEGORIES = entries.toList()

        fun fromApiValue(value: String): SponsorBlockCategory? =
            entries.firstOrNull { it.apiValue == value }
    }
}

/**
 * The action to take when a segment is reached.
 */
enum class SponsorBlockAction(val apiValue: String) {
    SKIP("skip"),
    MUTE("mute"),
    FULL("full"),
    POI("poi"),
    CHAPTER("chapter"),
    ;

    companion object {
        fun fromApiValue(value: String): SponsorBlockAction? =
            entries.firstOrNull { it.apiValue == value }
    }
}

/**
 * A single SponsorBlock segment as returned by the API.
 */
@Serializable
data class SponsorBlockSegment(
    @SerialName("segment") val segment: List<Double>,
    @SerialName("UUID") val uuid: String,
    @SerialName("category") val category: String,
    @SerialName("actionType") val actionType: String = "skip",
    @SerialName("locked") val locked: Int = 0,
    @SerialName("votes") val votes: Int = 0,
    @SerialName("videoDuration") val videoDuration: Double = 0.0,
    @SerialName("description") val description: String = "",
) {
    val startTime: Double get() = segment.getOrElse(0) { 0.0 }
    val endTime: Double get() = segment.getOrElse(1) { 0.0 }

    val categoryEnum: SponsorBlockCategory?
        get() = SponsorBlockCategory.fromApiValue(category)

    val actionEnum: SponsorBlockAction?
        get() = SponsorBlockAction.fromApiValue(actionType)
}

/**
 * Result from the hash-based privacy API endpoint.
 */
@Serializable
data class SponsorBlockHashResult(
    @SerialName("videoID") val videoId: String,
    @SerialName("segments") val segments: List<SponsorBlockSegment>,
)

/**
 * User-facing settings for SponsorBlock behavior.
 */
data class SponsorBlockSettings(
    val enabled: Boolean = false,
    val categories: Set<SponsorBlockCategory> = SponsorBlockCategory.DEFAULT_CATEGORIES.toSet(),
    val autoSkip: Boolean = true,
    val showSkipButton: Boolean = true,
    val showNotification: Boolean = true,
    val usePrivacyApi: Boolean = false,
)
