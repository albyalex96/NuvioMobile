package com.nuvio.app.features.opensubtitles

import com.nuvio.app.features.streams.StreamSubtitle

object OpenSubtitlesRepository {
    private val settingsRepository get() = OpenSubtitlesSettingsRepository
    private val apiClient get() = OpenSubtitlesApiClient

    fun isEnabled(): Boolean {
        settingsRepository.ensureLoaded()
        val settings = settingsRepository.snapshot()
        return settings.enabled && settings.hasApiKey
    }

    suspend fun searchAndPrepareSubtitles(
        imdbId: String?,
        type: String?,
        seasonNumber: Int?,
        episodeNumber: Int?,
    ): List<StreamSubtitle> {
        if (!shouldSearch()) return emptyList()

        val settings = settingsRepository.snapshot()
        val preferredLanguages = settings.languages.toList()

        if (preferredLanguages.isEmpty()) return emptyList()

        val response = apiClient.searchSubtitles(
            apiKey = settings.apiKey,
            imdbId = imdbId,
            type = type,
            seasonNumber = seasonNumber,
            episodeNumber = episodeNumber,
            languages = preferredLanguages,
        )

        return processSearchResponse(response, settings.apiKey, preferredLanguages)
    }

    suspend fun searchAndPrepareSubtitlesByQuery(
        query: String,
        type: String?,
        seasonNumber: Int?,
        episodeNumber: Int?,
    ): List<StreamSubtitle> {
        if (!shouldSearch()) return emptyList()

        val settings = settingsRepository.snapshot()
        val preferredLanguages = settings.languages.toList()

        if (preferredLanguages.isEmpty()) return emptyList()

        val response = apiClient.searchSubtitlesByQuery(
            apiKey = settings.apiKey,
            query = query,
            type = type,
            seasonNumber = seasonNumber,
            episodeNumber = episodeNumber,
            languages = preferredLanguages,
        )

        return processSearchResponse(response, settings.apiKey, preferredLanguages)
    }

    private suspend fun processSearchResponse(
        response: OpenSubtitlesSearchResponse,
        apiKey: String,
        preferredLanguages: List<String>,
    ): List<StreamSubtitle> {
        if (response.data.isEmpty()) return emptyList()

        val langPriority = preferredLanguages.withIndex().associate { (index, lang) -> lang.lowercase() to index }

        val sorted = response.data
            .filter { it.attributes?.files?.isNotEmpty() == true }
            .sortedWith(
                compareByDescending<OpenSubtitlesSubtitleData> { it.attributes?.fromTrusted == true }
                    .thenByDescending { it.attributes?.downloadCount ?: 0 }
                    .thenBy {
                        val code = it.attributes?.languageCode?.lowercase().orEmpty()
                        langPriority[code] ?: Int.MAX_VALUE
                    }
            )

        val results = mutableListOf<StreamSubtitle>()
        val seenLanguages = mutableSetOf<String>()

        for (sub in sorted) {
            val attrs = sub.attributes ?: continue
            val langCode = attrs.languageCode?.take(2)?.lowercase().orEmpty()
            if (langCode.isBlank()) continue
            if (langCode in seenLanguages) continue
            seenLanguages.add(langCode)

            val files = attrs.files ?: continue
            val file = files.firstOrNull() ?: continue
            val fileId = file.fileId ?: continue

            try {
                val downloadResponse = apiClient.downloadSubtitle(apiKey, fileId)
                val downloadUrl = downloadResponse.link ?: continue
                val label = buildString {
                    append(attrs.language ?: langCode)
                    if (attrs.hearingImpaired == true) append(" [HI]")
                    if (attrs.fromTrusted == true) append(" ★")
                }

                results.add(
                    StreamSubtitle(
                        url = downloadUrl,
                        language = langCode,
                        name = label,
                    )
                )
            } catch (_: Exception) {
                continue
            }
        }

        return results
    }

    private fun shouldSearch(): Boolean {
        settingsRepository.ensureLoaded()
        val settings = settingsRepository.snapshot()
        return settings.enabled && settings.hasApiKey && settings.languages.isNotEmpty()
    }
}
