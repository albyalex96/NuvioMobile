package com.nuvio.app.features.plugins.cloudstream

import com.nuvio.app.core.logging.InAppLogger
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.extractorApis
import com.lagradost.cloudstream3.utils.loadExtractor

private const val TAG = "ExtExtractorRegistry"

object ExternalExtractorRegistry {

    private val missingExtractorDomains = mutableSetOf<String>()
    private var installed = false

    fun registerExtractor(extractor: ExtractorApi) {
        if (extractorApis.any { it.mainUrl == extractor.mainUrl }) return
        extractorApis.add(extractor)
        InAppLogger.debug(TAG, "Registered extractor: ${extractor.name} (${extractor.mainUrl})")
    }

    fun registerAll(extractorList: List<ExtractorApi>) {
        extractorList.forEach { registerExtractor(it) }
    }

    fun clear() {
        missingExtractorDomains.clear()
    }

    suspend fun resolveExtractor(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        return try {
            val result = loadExtractor(url, referer, subtitleCallback, callback)
            if (!result) {
                val domain = try {
                    java.net.URI(url).host ?: url
                } catch (_: Exception) {
                    url
                }
                if (missingExtractorDomains.add(domain)) {
                    InAppLogger.warn(TAG, "No extractor registered for domain: $domain (url: $url)")
                }
            }
            result
        } catch (e: Exception) {
            InAppLogger.error(TAG, "loadExtractor error for ${url.take(80)}: ${e.message}")
            false
        } catch (e: Error) {
            InAppLogger.error(TAG, "loadExtractor linkage error for ${url.take(80)}: ${e.message}")
            false
        }
    }

    fun installGlobal() {
        if (installed) return
        installed = true
        InAppLogger.debug(TAG, "installGlobal: library extractorApis has ${extractorApis.size} built-in extractors")
    }

    fun getMissingExtractorDomains(): Set<String> = missingExtractorDomains.toSet()
}
