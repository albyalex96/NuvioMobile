package com.nuvio.app.features.plugins.runtime

import com.nuvio.app.features.plugins.PluginRuntimeResult
import com.nuvio.app.features.plugins.PluginStorage
import com.nuvio.app.features.plugins.PluginSubtitleResult
import com.nuvio.app.features.plugins.currentEpochMillis
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.generic_unknown
import org.jetbrains.compose.resources.getString

private const val PLUGIN_TIMEOUT_MS = 60_000L
private const val POLL_INTERVAL_MS = 15L

@JsFun("""
    return function(code, tmdbId, mediaType, season, episode) {
        try {
            window.__plugin_result = undefined;
            window.__plugin_done = false;
            var module = { exports: {} };
            var exports = module.exports;
            (function() {
                (0, eval)(code);
            })();
            var getStreams = module.exports.getStreams || window.getStreams;
            if (typeof getStreams !== 'function') {
                window.__plugin_result = '[]';
                window.__plugin_done = true;
                return;
            }
            var result = getStreams(tmdbId, mediaType,
                (season !== undefined && season !== -1) ? season : undefined,
                (episode !== undefined && episode !== -1) ? episode : undefined);
            if (result && typeof result.then === 'function') {
                result.then(function(r) {
                    window.__plugin_result = JSON.stringify(r || []);
                    window.__plugin_done = true;
                }).catch(function(e) {
                    console.error('Plugin async error:', e && e.message ? e.message : e);
                    window.__plugin_result = '[]';
                    window.__plugin_done = true;
                });
            } else {
                window.__plugin_result = JSON.stringify(result || []);
                window.__plugin_done = true;
            }
        } catch(e) {
            console.error('Plugin eval error:', e && e.message ? e.message : e);
            window.__plugin_result = '[]';
            window.__plugin_done = true;
        }
    }
""")
private external fun jsStartEval(code: String, tmdbId: String, mediaType: String, season: Int, episode: Int)

@JsFun("() => window.__plugin_done === true")
private external fun jsIsDone(): Boolean

@JsFun("() => window.__plugin_result || '[]'")
private external fun jsGetResult(): String

internal object PluginRuntime {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun executePlugin(
        code: String,
        tmdbId: String,
        mediaType: String,
        season: Int?,
        episode: Int?,
        scraperId: String,
    ): List<PluginRuntimeResult> {
        PluginStorage.loadScraperSettings(scraperId)

        return withTimeout(PLUGIN_TIMEOUT_MS) {
            jsStartEval(code, tmdbId, mediaType, season ?: -1, episode ?: -1)

            val start = currentEpochMillis()
            while (currentEpochMillis() - start < PLUGIN_TIMEOUT_MS) {
                if (jsIsDone()) {
                    val rawJson = jsGetResult()
                    return@withTimeout parseJsonResults(rawJson)
                }
                delay(POLL_INTERVAL_MS)
            }
            emptyList()
        }
    }

    suspend fun getPluginSettingsLayout(
        code: String,
        scraperId: String,
    ): String? = null

    private suspend fun parseJsonResults(rawJson: String): List<PluginRuntimeResult> {
        return runCatching {
            val array = json.parseToJsonElement(rawJson) as? JsonArray ?: return emptyList()
            array.mapNotNull { element ->
                val item = element as? JsonObject ?: return@mapNotNull null
                val url = when (val urlValue = item["url"]) {
                    is JsonPrimitive -> urlValue.contentOrNull?.takeIf { it.isNotBlank() }
                    is JsonObject -> urlValue["url"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
                    else -> null
                } ?: return@mapNotNull null

                val headers = (item["headers"] as? JsonObject)
                    ?.mapNotNull { (key, value) ->
                        value.jsonPrimitive.contentOrNull?.let { key to it }
                    }
                    ?.toMap()
                    ?.takeIf { it.isNotEmpty() }

                val subtitles = (item["subtitles"] as? JsonArray)?.mapNotNull { subElement ->
                    val subObj = subElement as? JsonObject ?: return@mapNotNull null
                    val subUrl = subObj["url"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                    val subLang = subObj["language"]?.jsonPrimitive?.contentOrNull ?: "Unknown"
                    val subName = subObj["name"]?.jsonPrimitive?.contentOrNull
                    val subHeaders = (subObj["headers"] as? JsonObject)
                        ?.mapNotNull { (key, value) ->
                            value.jsonPrimitive.contentOrNull?.let { key to it }
                        }
                        ?.toMap()
                        ?.takeIf { it.isNotEmpty() }
                    PluginSubtitleResult(
                        url = subUrl,
                        language = subLang,
                        name = subName,
                        headers = subHeaders,
                    )
                }?.takeIf { it.isNotEmpty() }

                PluginRuntimeResult(
                    title = item.stringOrNull("title") ?: item.stringOrNull("name") ?: getString(Res.string.generic_unknown),
                    name = item.stringOrNull("name"),
                    url = url,
                    quality = item.stringOrNull("quality"),
                    size = item.stringOrNull("size"),
                    language = item.stringOrNull("language"),
                    provider = item.stringOrNull("provider"),
                    type = item.stringOrNull("type"),
                    seeders = item["seeders"]?.jsonPrimitive?.intOrNull,
                    peers = item["peers"]?.jsonPrimitive?.intOrNull,
                    infoHash = item.stringOrNull("infoHash"),
                    headers = headers,
                    subtitles = subtitles,
                )
            }.filter { it.url.isNotBlank() }
        }.getOrElse { emptyList() }
    }

    private fun JsonObject.stringOrNull(key: String): String? =
        this[key]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() && !it.contains("[object") }
}
