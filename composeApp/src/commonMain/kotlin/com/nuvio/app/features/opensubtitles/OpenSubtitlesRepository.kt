package com.nuvio.app.features.opensubtitles

import com.nuvio.app.core.logging.InAppLogger
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
        if (!shouldSearch()) {
            println("[OpenSubtitles] searchAndPrepareSubtitles: skipped (not configured)")
            InAppLogger.debug("OpenSubtitles", "searchAndPrepareSubtitles: skipped (not configured)")
            return emptyList()
        }

        val settings = settingsRepository.snapshot()
        val preferredLanguages = settings.languages.toList()

        if (preferredLanguages.isEmpty()) {
            println("[OpenSubtitles] searchAndPrepareSubtitles: no preferred languages")
            InAppLogger.debug("OpenSubtitles", "searchAndPrepareSubtitles: no preferred languages")
            return emptyList()
        }

        println("[OpenSubtitles] searchAndPrepareSubtitles: imdbId=$imdbId type=$type S${seasonNumber}E${episodeNumber} languages=$preferredLanguages")
        InAppLogger.info("OpenSubtitles", "searchAndPrepareSubtitles: imdbId=$imdbId type=$type S${seasonNumber}E${episodeNumber} languages=$preferredLanguages")

        val response = apiClient.searchSubtitles(
            apiKey = settings.apiKey,
            imdbId = imdbId,
            type = type,
            seasonNumber = seasonNumber,
            episodeNumber = episodeNumber,
            languages = preferredLanguages,
        )

        val results = processSearchResponse(response, settings.apiKey, preferredLanguages)
        println("[OpenSubtitles] searchAndPrepareSubtitles: returning ${results.size} subtitles")
        InAppLogger.info("OpenSubtitles", "searchAndPrepareSubtitles: returning ${results.size} subtitles")
        return results
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
        if (response.data.isEmpty()) {
            println("[OpenSubtitles] processSearchResponse: no data in response")
            InAppLogger.debug("OpenSubtitles", "processSearchResponse: no data in response")
            return emptyList()
        }

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

        println("[OpenSubtitles] processSearchResponse: ${sorted.size} candidates after sorting")
        InAppLogger.debug("OpenSubtitles", "processSearchResponse: ${sorted.size} candidates after sorting")

        val results = mutableListOf<StreamSubtitle>()
        val seenLanguages = mutableSetOf<String>()

        for (sub in sorted) {
            val attrs = sub.attributes ?: continue
            val langCode = (attrs.language ?: "").take(2).lowercase()
            if (langCode.isBlank()) continue
            if (langCode in seenLanguages) continue
            seenLanguages.add(langCode)

            val files = attrs.files ?: continue
            val file = files.firstOrNull() ?: continue
            val fileId = file.fileId ?: continue

            try {
                val downloadResponse = apiClient.downloadSubtitle(apiKey, fileId)
                val downloadUrl = downloadResponse.link
                if (downloadUrl == null) {
                    println("[OpenSubtitles] processSearchResponse: download returned no link for lang=$langCode fileId=$fileId")
                    InAppLogger.warn("OpenSubtitles", "download returned no link for lang=$langCode fileId=$fileId")
                    continue
                }
                val label = buildString {
                    append(attrs.language ?: langCode)
                    if (attrs.hearingImpaired == true) append(" [HI]")
                    if (attrs.fromTrusted == true) append(" ★")
                }

                println("[OpenSubtitles] processSearchResponse: added lang=$langCode label='$label' fileId=$fileId")
                InAppLogger.info("OpenSubtitles", "processSearchResponse: added lang=$langCode label='$label' fileId=$fileId")

                results.add(
                    StreamSubtitle(
                        url = downloadUrl,
                        language = langCode,
                        name = label,
                    )
                )
            } catch (e: Exception) {
                println("[OpenSubtitles] processSearchResponse: download failed for lang=$langCode fileId=$fileId: ${e.message}")
                InAppLogger.warn("OpenSubtitles", "download failed for lang=$langCode fileId=$fileId: ${e.message}")
                continue
            }
        }

        println("[OpenSubtitles] processSearchResponse: returning ${results.size} subtitles for languages: ${results.map { it.language }}")
        InAppLogger.info("OpenSubtitles", "processSearchResponse: returning ${results.size} subtitles for languages: ${results.map { it.language }}")
        return results
    }

    suspend fun searchManual(
        imdbId: String?,
        type: String?,
        seasonNumber: Int?,
        episodeNumber: Int?,
    ): List<OpenSubtitlesSubtitleItem> {
        if (!isConfigured()) {
            println("[OpenSubtitles] searchManual: skipped (not configured)")
            InAppLogger.debug("OpenSubtitles", "searchManual: skipped (not configured)")
            return emptyList()
        }

        val settings = settingsRepository.snapshot()
        val preferredLanguages = settings.languages.toList()
        if (preferredLanguages.isEmpty()) {
            println("[OpenSubtitles] searchManual: no preferred languages")
            InAppLogger.debug("OpenSubtitles", "searchManual: no preferred languages")
            return emptyList()
        }

        println("[OpenSubtitles] searchManual: imdbId=$imdbId type=$type S${seasonNumber}E${episodeNumber} languages=$preferredLanguages")
        InAppLogger.info("OpenSubtitles", "searchManual: imdbId=$imdbId type=$type S${seasonNumber}E${episodeNumber} languages=$preferredLanguages")

        val response = apiClient.searchSubtitles(
            apiKey = settings.apiKey,
            imdbId = imdbId,
            type = type,
            seasonNumber = seasonNumber,
            episodeNumber = episodeNumber,
            languages = preferredLanguages,
        )

        val items = response.data
            .filter { it.attributes?.files?.isNotEmpty() == true }
            .sortedWith(
                compareByDescending<OpenSubtitlesSubtitleData> { it.attributes?.fromTrusted == true }
                    .thenByDescending { it.attributes?.downloadCount ?: 0 }
            )
            .mapNotNull { sub ->
                val attrs = sub.attributes ?: return@mapNotNull null
                val file = attrs.files?.firstOrNull() ?: return@mapNotNull null
                val fileId = file.fileId ?: return@mapNotNull null
                val langCode = (attrs.language ?: "").take(2).lowercase()
                OpenSubtitlesSubtitleItem(
                    fileId = fileId,
                    language = attrs.language ?: "",
                    languageCode = langCode,
                    release = attrs.release,
                    fileName = file.fileName,
                    hearingImpaired = attrs.hearingImpaired == true,
                    fromTrusted = attrs.fromTrusted == true,
                    downloadCount = attrs.downloadCount ?: 0,
                )
            }

        println("[OpenSubtitles] searchManual: returning ${items.size} items")
        InAppLogger.info("OpenSubtitles", "searchManual: returning ${items.size} items")
        return items
    }

    suspend fun downloadItem(item: OpenSubtitlesSubtitleItem): String? {
        val settings = settingsRepository.snapshot()
        if (!settings.hasApiKey) {
            println("[OpenSubtitles] downloadItem: no API key")
            InAppLogger.warn("OpenSubtitles", "downloadItem: no API key")
            return null
        }
        println("[OpenSubtitles] downloadItem: fileId=${item.fileId} language=${item.language}")
        InAppLogger.info("OpenSubtitles", "downloadItem: fileId=${item.fileId} language=${item.language}")
        return try {
            val response = apiClient.downloadSubtitle(settings.apiKey, item.fileId)
            val link = response.link
            if (link != null) {
                println("[OpenSubtitles] downloadItem: success, link=${link.take(60)}...")
                InAppLogger.info("OpenSubtitles", "downloadItem: success")
            } else {
                println("[OpenSubtitles] downloadItem: response has no link")
                InAppLogger.warn("OpenSubtitles", "downloadItem: response has no link")
            }
            link
        } catch (e: Exception) {
            println("[OpenSubtitles] downloadItem: error: ${e.message}")
            InAppLogger.error("OpenSubtitles", "downloadItem error: ${e.message}")
            null
        }
    }

    fun isConfigured(): Boolean {
        settingsRepository.ensureLoaded()
        val settings = settingsRepository.snapshot()
        return settings.enabled && settings.hasApiKey && settings.languages.isNotEmpty()
    }

    private fun shouldSearch(): Boolean {
        settingsRepository.ensureLoaded()
        val settings = settingsRepository.snapshot()
        return settings.enabled && settings.hasApiKey && settings.languages.isNotEmpty()
    }
}
