package com.nuvio.app.features.opensubtitles

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenSubtitlesSearchResponse(
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
    val subtitleId: Int? = null,
    val language: String? = null,
    val languageCode: String? = null,
    val downloadCount: Int? = null,
    val newDownloadCount: Int? = null,
    val hearingImpaired: Boolean? = null,
    val hd: Boolean? = null,
    val fps: Double? = null,
    val votes: Int? = null,
    val ratings: Double? = null,
    val fromTrusted: Boolean? = null,
    val foreignPartsOnly: Boolean? = null,
    val aiTranslated: Boolean? = null,
    val machineTranslated: Boolean? = null,
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
    val uploaderId: Int? = null,
    val name: String? = null,
    val rank: String? = null,
)

@Serializable
data class OpenSubtitlesFile(
    val fileId: Int? = null,
    val fileName: String? = null,
)

@Serializable
data class OpenSubtitlesFeatureDetails(
    val featureId: Int? = null,
    val featureType: String? = null,
    val year: Int? = null,
    val title: String? = null,
    val imdbId: Int? = null,
    val tmdbId: Int? = null,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val parentImdbId: Int? = null,
    val parentTitle: String? = null,
    val parentTmdbId: Int? = null,
)

@Serializable
data class OpenSubtitlesDownloadResponse(
    val link: String? = null,
    val fileName: String? = null,
    val requests: Int? = null,
    val remaining: Int? = null,
)
