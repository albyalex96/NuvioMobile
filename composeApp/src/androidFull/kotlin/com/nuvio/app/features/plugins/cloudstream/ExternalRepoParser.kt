package com.nuvio.app.features.plugins.cloudstream

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

private const val TAG = "ExternalRepoParser"

data class ExternalPluginEntry(
    val name: String,
    val internalName: String,
    val description: String? = null,
    val version: String? = null,
    val apiVersion: Int = 1,
    val status: Int = 1,
    val authors: List<String>? = null,
    val tvTypes: List<String>? = null,
    val iconUrl: String? = null,
    val url: String,
    val fileSize: Long? = null,
    val repositoryUrl: String? = null,
)

data class ExternalRepoManifest(
    val name: String,
    val description: String? = null,
    val manifestVersion: Int = 1,
    val pluginLists: List<String>,
)

data class ExternalRepoParseResult(
    val name: String,
    val description: String?,
    val plugins: List<ExternalPluginEntry>,
)

object ExternalRepoParser {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun tryParse(url: String): ExternalRepoParseResult? = withContext(Dispatchers.IO) {
        val body = fetchBody(url) ?: return@withContext null
        val trimmed = body.trim()

        if (trimmed.contains("\"pluginLists\"")) {
            try {
                val obj = JSONObject(trimmed)
                val manifest = ExternalRepoManifest(
                    name = obj.getString("name"),
                    description = obj.optString("description", null),
                    manifestVersion = obj.optInt("manifestVersion", 1),
                    pluginLists = obj.getJSONArray("pluginLists").let { arr ->
                        (0 until arr.length()).map { arr.getString(it) }
                    }
                )
                if (manifest.pluginLists.isNotEmpty()) {
                    Log.d(TAG, "Parsed as repo manifest: ${manifest.name}, ${manifest.pluginLists.size} plugin lists")
                    val allPlugins = coroutineScope {
                        manifest.pluginLists.map { listUrl ->
                            async {
                                val resolvedUrl = resolveUrl(url, listUrl)
                                fetchPluginList(resolvedUrl) ?: emptyList()
                            }
                        }.awaitAll().flatten()
                    }
                    return@withContext ExternalRepoParseResult(
                        name = manifest.name,
                        description = manifest.description,
                        plugins = allPlugins
                    )
                }
            } catch (e: Exception) {
                Log.d(TAG, "Not a repo manifest: ${e.message}")
            }
        }

        if (trimmed.startsWith("[")) {
            try {
                val plugins = parsePluginJsonArray(trimmed)
                if (plugins.isNotEmpty() && plugins.first().internalName.isNotBlank()) {
                    Log.d(TAG, "Parsed as direct plugins list: ${plugins.size} plugins")
                    val repoName = inferRepoName(url)
                    return@withContext ExternalRepoParseResult(
                        name = repoName,
                        description = null,
                        plugins = plugins
                    )
                }
            } catch (e: Exception) {
                Log.d(TAG, "Not a direct plugins list: ${e.message}")
            }
        }

        null
    }

    private suspend fun fetchPluginList(url: String): List<ExternalPluginEntry>? = withContext(Dispatchers.IO) {
        val body = fetchBody(url) ?: return@withContext null
        try {
            parsePluginJsonArray(body.trim())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse plugin list from $url: ${e.message}")
            null
        }
    }

    private fun parsePluginJsonArray(json: String): List<ExternalPluginEntry> {
        val arr = JSONArray(json)
        return (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            ExternalPluginEntry(
                name = obj.getString("name"),
                internalName = obj.getString("internalName"),
                description = obj.optString("description", null),
                version = parseVersion(obj),
                apiVersion = obj.optInt("apiVersion", 1),
                status = obj.optInt("status", 1),
                authors = obj.optJSONArray("authors")?.let { ja ->
                    (0 until ja.length()).map { ja.getString(it) }
                },
                tvTypes = obj.optJSONArray("tvTypes")?.let { ja ->
                    (0 until ja.length()).map { ja.getString(it) }
                },
                iconUrl = obj.optString("iconUrl", null),
                url = obj.getString("url"),
                fileSize = if (obj.has("fileSize")) obj.optLong("fileSize") else null,
                repositoryUrl = obj.optString("repositoryUrl", null),
            )
        }
    }

    private fun fetchBody(url: String): String? {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "NuvioMobile/1.0")
                .build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "HTTP ${response.code} for $url")
                    return null
                }
                response.body?.string()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch $url: ${e.message}")
            null
        }
    }

    private fun resolveUrl(baseUrl: String, relativeUrl: String): String {
        if (relativeUrl.startsWith("http://") || relativeUrl.startsWith("https://")) {
            return relativeUrl
        }
        val base = baseUrl.substringBeforeLast("/")
        return "$base/$relativeUrl"
    }

    private fun parseVersion(obj: JSONObject): String? {
        return if (obj.has("version")) {
            val v = obj.get("version")
            when (v) {
                is Int -> "v$v"
                is String -> if (v.startsWith("v")) v else "v$v"
                else -> null
            }
        } else null
    }

    private fun inferRepoName(url: String): String {
        val path = url.substringAfter("://").substringBefore("?")
        val segments = path.split("/").filter { it.isNotBlank() }
        return segments.lastOrNull()?.removeSuffix(".json") ?: "External Repository"
    }
}
