package com.nuvio.app.features.plugins.runtime

import com.nuvio.app.features.plugins.PluginRuntimeResult
import com.nuvio.app.features.plugins.PluginStorage
import com.nuvio.app.features.plugins.runtime.crypto.CryptoBridge
import com.nuvio.app.features.plugins.runtime.dom.DomBridge
import com.nuvio.app.features.plugins.runtime.host.HostApiRegistry
import com.nuvio.app.features.plugins.runtime.host.HostFunctions
import com.nuvio.app.features.plugins.runtime.js.JsBindings
import com.nuvio.app.features.plugins.runtime.js.JsRuntime
import com.dokar.quickjs.binding.function
import com.nuvio.app.features.plugins.runtime.network.FetchBridge
import com.nuvio.app.features.plugins.runtime.network.UrlBridge
import com.nuvio.app.features.plugins.runtime.wasm.WasmBridge
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import co.touchlab.kermit.Logger
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

internal object PluginRuntime {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val log = Logger.withTag("PluginRuntime")

    suspend fun executePlugin(
        code: String,
        tmdbId: String,
        mediaType: String,
        season: Int?,
        episode: Int?,
        scraperId: String,
    ): List<PluginRuntimeResult> = withContext(Dispatchers.Default) {
        val scraperSettingsJson = PluginStorage.loadScraperSettings(scraperId) ?: "{}"
        val scraperSettingsMap = runCatching {
            json.decodeFromString<Map<String, JsonElement>>(scraperSettingsJson)
        }.getOrElse { emptyMap() }

        withTimeout(PLUGIN_TIMEOUT_MS) {
            executePluginInternal(
                code = code,
                tmdbId = tmdbId,
                mediaType = mediaType,
                season = season,
                episode = episode,
                scraperId = scraperId,
                scraperSettings = scraperSettingsMap,
            )
        }
    }

    suspend fun getPluginSettingsLayout(
        code: String,
        scraperId: String,
    ): String? = withContext(Dispatchers.Default) {
        withTimeout(PLUGIN_TIMEOUT_MS) {
            val jsRuntime = JsRuntime()
            val deferred = CompletableDeferred<String?>()

            try {
                jsRuntime.use {
                    val polyfillCode = JsBindings.buildPolyfillCode(
                        scraperIdJson = JsonPrimitive(scraperId).toString(),
                        settingsJson = "{}"
                    )
                    evaluate<Any?>(polyfillCode)

                    val wrappedCode = """
                        var module = { exports: {} };
                        var exports = module.exports;
                        (function() {
                            $code
                        })();
                    """.trimIndent()
                    evaluate<Any?>(wrappedCode)

                    val callCode = """
                        (async function() {
                            try {
                                var onSettings = (typeof module !== 'undefined' && module.exports && module.exports.onSettings) || globalThis.onSettings;
                                if (typeof onSettings === 'function') {
                                    var layout = await onSettings();
                                    __capture_settings_result(JSON.stringify(layout || []));
                                } else {
                                    __capture_settings_result("[]");
                                }
                            } catch (e) {
                                console.error("onSettings error:", e);
                                __capture_settings_result("[]");
                            }
                        })();
                    """.trimIndent()
                    
                    function("__capture_settings_result") { args: Array<Any?> ->
                        deferred.complete(args.getOrNull(0)?.toString())
                        null
                    }
                    
                    evaluate<Any?>(callCode)
                    deferred.await()
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    private suspend fun executePluginInternal(
        code: String,
        tmdbId: String,
        mediaType: String,
        season: Int?,
        episode: Int?,
        scraperId: String,
        scraperSettings: Map<String, JsonElement>,
    ): List<PluginRuntimeResult> {
        val jsRuntime = JsRuntime()
        val deferred = CompletableDeferred<Any?>()

        val domBridge = DomBridge()
        val hostRegistry = HostApiRegistry().apply {
            addModule(HostFunctions(scraperId) { deferred.complete(it) })
            addModule(FetchBridge())
            addModule(UrlBridge())
            addModule(CryptoBridge())
            addModule(WasmBridge())
            addModule(domBridge)
        }

        try {
            jsRuntime.use {
                hostRegistry.registerAll(this)

                val settingsJson = JsonObject(scraperSettings).toString()
                val polyfillCode = JsBindings.buildPolyfillCode(
                    scraperIdJson = JsonPrimitive(scraperId).toString(),
                    settingsJson = settingsJson,
                )
                evaluate<Any?>(polyfillCode)

                val wrappedCode = """
                    var module = { exports: {} };
                    var exports = module.exports;
                    (function() {
                        $code
                    })();
                """.trimIndent()
                evaluate<Any?>(wrappedCode)

                val tmdbIdArg = JsonPrimitive(tmdbId).toString()
                val mediaTypeArg = JsonPrimitive(mediaType).toString()
                val seasonArg = season?.toString() ?: "undefined"
                val episodeArg = episode?.toString() ?: "undefined"
                val callCode = """
                    (async function() {
                        try {
                            var getStreams = module.exports.getStreams || globalThis.getStreams;
                            if (!getStreams) {
                                console.error("getStreams function not found on module.exports or globalThis");
                                __capture_result([]);
                                return;
                            }
                            var result = await getStreams($tmdbIdArg, $mediaTypeArg, $seasonArg, $episodeArg);
                            __capture_result(result || []);
                        } catch (e) {
                            console.error("getStreams error:", e && e.message ? e.message : e, e && e.stack ? e.stack : "");
                            __capture_result([]);
                        }
                    })();
                """.trimIndent()
                evaluate<Any?>(callCode)
                
                deferred.await()
            }
            
            val rawResult = deferred.await()
            val jsonString = when (rawResult) {
                is String -> rawResult
                else -> {
                    val element = toJsonElement(rawResult)
                    when (element) {
                        is JsonArray -> json.encodeToString(element)
                        is JsonObject -> json.encodeToString(JsonArray(listOf(element)))
                        else -> "[]"
                    }
                }
            }
            return parseJsonResults(jsonString)
        } finally {
            domBridge.clear()
        }
    }

    private fun parseJsonResults(rawJson: String): List<PluginRuntimeResult> {
        log.i { "parseJsonResults raw length=${rawJson.length}" }
        if (rawJson.length <= 5000) log.i { "parseJsonResults raw=$rawJson" }
        if (rawJson.length < 10) return emptyList()
        val array = try {
            json.parseToJsonElement(rawJson) as? JsonArray
        } catch (e: Exception) {
            log.w { "parseJsonResults standard parse FAILED: ${e.message?.take(100)}" }
            try {
                json.parseToJsonElement(rawJson.replace(Regex("[\\x00-\\x1F]"), "")) as? JsonArray
            } catch (e2: Exception) {
                log.w { "parseJsonResults lenient parse FAILED: ${e2.message?.take(100)}, trying regex" }
                return extractWithRegex(rawJson)
            }
        }
        if (array == null) return emptyList()
        return array.mapNotNull { element ->
            val item = element as? JsonObject ?: return@mapNotNull null
            val url = when (val urlValue = item["url"]) {
                is JsonPrimitive -> urlValue.contentOrNull?.takeIf { it.isNotBlank() }
                is JsonObject -> urlValue["url"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
                else -> null
            } ?: return@mapNotNull null
            PluginRuntimeResult(
                title = item.stringOrNull("title") ?: item.stringOrNull("name") ?: runBlocking { getString(Res.string.generic_unknown) },
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
                headers = (item["headers"] as? JsonObject)?.mapNotNull { (k, v) ->
                    v.jsonPrimitive.contentOrNull?.let { k to it }
                }?.toMap()?.takeIf { it.isNotEmpty() },
                subtitles = (item["subtitles"] as? JsonArray)?.mapNotNull { sub ->
                    val s = sub as? JsonObject ?: return@mapNotNull null
                    com.nuvio.app.features.plugins.PluginSubtitleResult(
                        url = s["url"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null,
                        language = s["language"]?.jsonPrimitive?.contentOrNull ?: "Unknown",
                        name = s["name"]?.jsonPrimitive?.contentOrNull,
                        headers = (s["headers"] as? JsonObject)?.mapNotNull { (k, v) -> v.jsonPrimitive.contentOrNull?.let { k to it } }?.toMap()?.takeIf { it.isNotEmpty() },
                    )
                }?.takeIf { it.isNotEmpty() },
            )
        }.filter { it.url.isNotBlank() }
    }

    private fun extractWithRegex(rawJson: String): List<PluginRuntimeResult> {
        val results = mutableListOf<PluginRuntimeResult>()
        val urlRegex = Regex(""""url"\s*:\s*"((?:[^"\\]|\\.)*)"""")
        val nameRegex = Regex(""""name"\s*:\s*"((?:[^"\\]|\\.)*)"""")
        val titleRegex = Regex(""""title"\s*:\s*"((?:[^"\\]|\\.)*)"""")
        val qualRegex = Regex(""""quality"\s*:\s*"((?:[^"\\]|\\.)*)"""")

        val urlMatches = urlRegex.findAll(rawJson).toList()
        if (urlMatches.isEmpty()) {
            log.w { "extractWithRegex: no url matches found" }
            return emptyList()
        }
        val nameMatches = nameRegex.findAll(rawJson).toList()
        val titleMatches = titleRegex.findAll(rawJson).toList()
        val qualMatches = qualRegex.findAll(rawJson).toList()

        for (urlMatch in urlMatches) {
            val url = urlMatch.groupValues[1]
            val urlStart = urlMatch.range.first
            val name = nameMatches.lastOrNull { it.range.last < urlStart }?.groupValues?.getOrNull(1)
            val title = titleMatches.lastOrNull { it.range.last < urlStart }?.groupValues?.getOrNull(1)
            val qual = qualMatches.lastOrNull { it.range.last < urlStart }?.groupValues?.getOrNull(1)
            results.add(PluginRuntimeResult(
                title = title ?: name ?: runBlocking { getString(Res.string.generic_unknown) },
                name = name,
                url = url,
                quality = qual,
            ))
        }
        log.i { "extractWithRegex: found ${results.size} results" }
        return results
    }

    private fun JsonObject.stringOrNull(key: String): String? =
        this[key]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() && !it.contains("[object") }

    private fun toJsonElement(value: Any?): JsonElement = when (value) {
        null -> JsonNull
        is JsonElement -> value
        is String -> JsonPrimitive(value)
        is Boolean -> JsonPrimitive(value)
        is Int -> JsonPrimitive(value)
        is Long -> JsonPrimitive(value)
        is Float -> JsonPrimitive(value)
        is Double -> JsonPrimitive(value)
        is Number -> JsonPrimitive(value.toDouble())
        is Map<*, *> -> JsonObject(
            value.entries
                .filter { it.key is String }
                .associate { (it.key as String) to toJsonElement(it.value) },
        )
        is Iterable<*> -> JsonArray(value.map(::toJsonElement))
        else -> JsonPrimitive(value.toString())
    }
}
