package com.nuvio.app.features.opensubtitles

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenSubtitlesSearchResponse(
    @SerialName("total_count")
    val totalCount: Int? = null,
    val data: List<OpenSubtitlesSubtitleData> = emptyList(),
)

@Serializable
data class OpenSubtitlesSubtitleData(
    val id: String? = null,
    val type: String? = null,
    val attributes: OpenSubtitlesSubtitleAttributes? = null,
)

@Serializable
data class OpenSubtitlesSubtitleAttributes(
    @SerialName("subtitle_id")
    val subtitleId: String? = null,
    val language: String? = null,
    val languageCode: String? = null,
    @SerialName("download_count")
    val downloadCount: Int? = null,
    @SerialName("new_download_count")
    val newDownloadCount: Int? = null,
    @SerialName("hearing_impaired")
    val hearingImpaired: Boolean? = null,
    val hd: Boolean? = null,
    val fps: Double? = null,
    val votes: Int? = null,
    val ratings: Double? = null,
    @SerialName("from_trusted")
    val fromTrusted: Boolean? = null,
    @SerialName("foreign_parts_only")
    val foreignPartsOnly: Boolean? = null,
    @SerialName("ai_translated")
    val aiTranslated: Boolean? = null,
    @SerialName("machine_translated")
    val machineTranslated: Boolean? = null,
    @SerialName("upload_date")
    val uploadDate: String? = null,
    val release: String? = null,
    val comments: String? = null,
    val uploader: OpenSubtitlesUploader? = null,
    val files: List<OpenSubtitlesFile>? = null,
    @SerialName("feature_details")
    val featureDetails: OpenSubtitlesFeatureDetails? = null,
    val url: String? = null,
)

@Serializable
data class OpenSubtitlesUploader(
    @SerialName("uploader_id")
    val uploaderId: Int? = null,
    val name: String? = null,
    val rank: String? = null,
)

@Serializable
data class OpenSubtitlesFile(
    @SerialName("file_id")
    val fileId: Int? = null,
    @SerialName("file_name")
    val fileName: String? = null,
)

@Serializable
data class OpenSubtitlesFeatureDetails(
    @SerialName("feature_id")
    val featureId: Int? = null,
    @SerialName("feature_type")
    val featureType: String? = null,
    val year: Int? = null,
    val title: String? = null,
    @SerialName("imdb_id")
    val imdbId: Int? = null,
    @SerialName("tmdb_id")
    val tmdbId: Int? = null,
    @SerialName("season_number")
    val seasonNumber: Int? = null,
    @SerialName("episode_number")
    val episodeNumber: Int? = null,
    @SerialName("parent_imdb_id")
    val parentImdbId: Int? = null,
    @SerialName("parent_title")
    val parentTitle: String? = null,
    @SerialName("parent_tmdb_id")
    val parentTmdbId: Int? = null,
)

@Serializable
data class OpenSubtitlesDownloadResponse(
    val link: String? = null,
    val fileName: String? = null,
    val requests: Int? = null,
    val remaining: Int? = null,
)

data class OpenSubtitlesSubtitleItem(
    val fileId: Int,
    val language: String,
    val languageCode: String,
    val release: String?,
    val fileName: String?,
    val hearingImpaired: Boolean,
    val fromTrusted: Boolean,
    val downloadCount: Int,
    val url: String? = null,
)
