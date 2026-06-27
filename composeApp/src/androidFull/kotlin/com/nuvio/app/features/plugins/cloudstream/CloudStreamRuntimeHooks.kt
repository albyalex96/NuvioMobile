package com.nuvio.app.features.plugins.cloudstream

import android.content.Context
import com.lagradost.cloudstream3.AcraApplication
import com.lagradost.cloudstream3.app
import com.nuvio.app.features.plugins.DexRepoInstallData
import com.nuvio.app.features.plugins.PluginRepositoryItem
import com.nuvio.app.features.plugins.PluginRuntimeResult
import com.nuvio.app.features.plugins.PluginScraper
import com.nuvio.app.features.plugins.dexRepoParser
import com.nuvio.app.features.plugins.dexScraper
import okhttp3.OkHttpClient
import org.conscrypt.Conscrypt
import java.security.Security
import java.util.concurrent.TimeUnit

object CloudStreamRuntimeHooks {

    private var initialized = false

    fun ensureInitialized() {
        if (initialized) return
        initialized = true

        try {
            Security.insertProviderAt(Conscrypt.newProvider(), 1)
        } catch (_: Exception) {
        }

        app.baseClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    fun setApplicationContext(context: Context) {
        ensureInitialized()
        AcraApplication.context = context
        ExternalExtensionLoader.initialize(context)
        registerDexScraper()
        registerDexRepoParser()
    }

    fun setActivity(activity: android.app.Activity) {
        ensureInitialized()
        AcraApplication.setActivity(activity)
    }

    fun onAppForeground(context: Context) {
        ensureInitialized()
        AcraApplication.context = context
    }

    private fun registerDexScraper() {
        if (dexScraper != null) return
        dexScraper = { scraperId, tmdbId, mediaType, season, episode ->
            com.nuvio.app.features.plugins.cloudstream.ExternalExtensionRunner.execute(
                scraperId = scraperId,
                tmdbId = tmdbId,
                mediaType = mediaType,
                season = season,
                episode = episode,
            )
        }
    }

    private fun registerDexRepoParser() {
        dexRepoParser = { url ->
            ExternalRepoParser.tryParse(url)?.let { parseResult ->
                val scrapers = mutableListOf<PluginScraper>()
                for (entry in parseResult.plugins) {
                    val file = ExternalExtensionLoader.downloadExtension(entry.internalName, entry.url) ?: continue
                    val supportedTypes = entry.tvTypes?.map { t ->
                        when (t.lowercase()) {
                            "series", "show", "anime", "animeseries" -> "tv"
                            "movie", "anime movie", "anime_movie" -> "movie"
                            else -> t.lowercase()
                        }
                    } ?: listOf("tv", "movie")
                    val desc = buildString {
                        entry.description?.let { append(it) }
                        val authors = entry.authors
                        if (authors != null && authors.isNotEmpty()) {
                            if (isNotEmpty()) append(" | ")
                            append("by ")
                            append(authors.joinToString(", "))
                        }
                    }
                    scrapers.add(
                        PluginScraper(
                            id = entry.internalName,
                            repositoryUrl = url,
                            name = entry.name,
                            description = desc,
                            version = entry.version ?: "",
                            logo = entry.iconUrl,
                            filename = file.name,
                            supportedTypes = supportedTypes,
                            enabled = true,
                            manifestEnabled = true,
                            code = "",
                            pluginType = "dex",
                        )
                    )
                }
                DexRepoInstallData(
                    repository = PluginRepositoryItem(
                        manifestUrl = url,
                        name = parseResult.name,
                        description = parseResult.description,
                        version = parseResult.manifestVersion?.toString(),
                        scraperCount = scrapers.size,
                    ),
                    scrapers = scrapers,
                )
            }
        }
    }
}
