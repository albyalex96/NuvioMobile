package com.nuvio.app.features.plugins

import co.touchlab.kermit.Logger
import com.dokar.quickjs.binding.define
import com.dokar.quickjs.binding.function
import com.dokar.quickjs.QuickJs
import com.dokar.quickjs.quickJs
import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.nodes.Element
import com.fleeksoft.ksoup.select.Elements
import com.nuvio.app.features.addons.httpRequestRaw
import com.nuvio.app.features.addons.httpGetText
import com.nuvio.app.features.tmdb.buildTmdbUrl
import com.nuvio.app.features.tmdb.TmdbSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlin.random.Random

private const val SORA_TIMEOUT_MS = 60_000L

internal object SoraModuleRuntime {
    private val log = Logger.withTag("SoraModuleRuntime")
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun executeModule(
        module: SoraModuleItem,
        tmdbId: String,
        mediaType: String,
        season: Int?,
        episode: Int?,
    ): List<PluginRuntimeResult> = withContext(Dispatchers.Default) {
        withTimeout(SORA_TIMEOUT_MS) {
            executeModuleInternal(module, tmdbId, mediaType, season, episode)
        }
    }

    private suspend fun executeModuleInternal(
        module: SoraModuleItem,
        tmdbId: String,
        mediaType: String,
        season: Int?,
        episode: Int?,
    ): List<PluginRuntimeResult> {
        val title = resolveTmdbTitle(tmdbId, mediaType) ?: return emptyList()
        val isAsync = module.asyncJS

        val searchInput = if (isAsync) title
            else buildSearchHtml(module, title) ?: return emptyList()

        val searchResults = executeSearch(module, searchInput, isAsync)
        val firstResult = searchResults.firstOrNull() ?: return emptyList()
        val contentUrl = firstResult.href ?: return emptyList()

        val isTv = mediaType.lowercase() in setOf("tv", "series", "show", "tvshow") || season != null
        val targetUrl = if (isTv) {
            resolveEpisodeUrl(module, contentUrl, season ?: 1, episode ?: 1, isAsync) ?: return emptyList()
        } else contentUrl

        val streamUrl = resolveStreamUrl(module, targetUrl, isAsync) ?: return emptyList()

        return listOf(PluginRuntimeResult(
            title = firstResult.title,
            name = module.sourceName,
            url = streamUrl,
            quality = module.quality,
            language = module.language,
            provider = module.sourceName,
        ))
    }

    private suspend fun resolveTmdbTitle(tmdbId: String, mediaType: String): String? {
        val apiKey = TmdbSettingsRepository.snapshot().apiKey.trim().takeIf(String::isNotBlank) ?: return null
        val cleanId = tmdbId.trim().substringBefore(':').substringBefore('/')
        if (!cleanId.all(Char::isDigit)) return null
        val endpoint = when (mediaType.trim().lowercase()) {
            "tv", "series", "show", "tvshow" -> "tv/$cleanId"
            else -> "movie/$cleanId"
        }
        val url = buildTmdbUrl(endpoint, apiKey)
        return runCatching {
            val obj = json.parseToJsonElement(httpGetText(url)) as? JsonObject ?: return@runCatching null
            (obj["title"]?.jsonPrimitive?.contentOrNull ?: obj["name"]?.jsonPrimitive?.contentOrNull)
                ?.trim()?.takeIf(String::isNotBlank)
        }.getOrNull()
    }

    private suspend fun buildSearchHtml(module: SoraModuleItem, query: String): String? {
        val searchUrl = module.searchBaseUrl?.replace("%s", query) ?: module.baseUrl ?: return null
        return runCatching { httpGetText(searchUrl) }.getOrNull()
    }

    private var capturedResult = "[]"

    private suspend fun executeSearch(
        module: SoraModuleItem, input: String, isAsync: Boolean,
    ): List<SoraSearchResult> {
        try {
            capturedResult = "[]"
            quickJs(Dispatchers.Default) {
                withNativeFunctions()
                evaluate<Any?>(polyfillCode(module.sourceName))
                evaluate<Any?>(wrapJsCode(module.jsCode))

                val escaped = input.replace("\\", "\\\\").replace("\"", "\\\"")
                    .replace("\n", "\\n").replace("\r", "\\r")

                if (isAsync) {
                    evaluate<Any?>("""
                        (async function() {
                            try {
                                var fn = module.exports.searchResults || globalThis.searchResults;
                                if (!fn) { __capture_result("[]"); return; }
                                var r = await fn("$escaped");
                                __capture_result(typeof r === 'string' ? r : JSON.stringify(r || []));
                            } catch(e) { __capture_result("[]"); }
                        })();
                    """.trimIndent())
                } else {
                    evaluate<Any?>("""
                        (function() {
                            try {
                                var fn = module.exports.searchResults || globalThis.searchResults;
                                if (!fn) { __capture_result("[]"); return; }
                                var r = fn("$escaped");
                                __capture_result(typeof r === 'string' ? r : JSON.stringify(r || []));
                            } catch(e) { __capture_result("[]"); }
                        })();
                    """.trimIndent())
                }
            }
            return parseSearchResults(capturedResult)
        } catch (e: Exception) {
            log.e(e) { "Sora search failed for ${module.sourceName}" }
            return emptyList()
        }
    }

    private suspend fun resolveEpisodeUrl(
        module: SoraModuleItem, contentUrl: String, season: Int, episode: Int, isAsync: Boolean,
    ): String? {
        try {
            capturedResult = "[]"
            quickJs(Dispatchers.Default) {
                withNativeFunctions()
                evaluate<Any?>(polyfillCode(module.sourceName))
                evaluate<Any?>(wrapJsCode(module.jsCode))

                val escaped = contentUrl.replace("\\", "\\\\").replace("\"", "\\\"")

                if (isAsync) {
                    evaluate<Any?>("""
                        (async function() {
                            try {
                                var fn = module.exports.extractEpisodes || globalThis.extractEpisodes;
                                if (!fn) { __capture_result("[]"); return; }
                                var r = await fn("$escaped");
                                __capture_result(typeof r === 'string' ? r : JSON.stringify(r || []));
                            } catch(e) { __capture_result("[]"); }
                        })();
                    """.trimIndent())
                } else {
                    val html = httpGetText(contentUrl)
                    val eh = html.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r")
                    evaluate<Any?>("""
                        (function() {
                            try {
                                var fn = module.exports.extractEpisodes || globalThis.extractEpisodes;
                                if (!fn) { __capture_result("[]"); return; }
                                var r = fn("$eh");
                                __capture_result(typeof r === 'string' ? r : JSON.stringify(r || []));
                            } catch(e) { __capture_result("[]"); }
                        })();
                    """.trimIndent())
                }
            }
            val episodes = parseEpisodeResults(capturedResult)
            return (episodes.find { it.number?.toIntOrNull() == episode } ?: episodes.firstOrNull())?.href
        } catch (e: Exception) {
            log.e(e) { "Sora episode resolution failed for ${module.sourceName}" }
            return null
        }
    }

    private suspend fun resolveStreamUrl(
        module: SoraModuleItem, url: String, isAsync: Boolean,
    ): String? {
        try {
            capturedResult = "\"\""
            quickJs(Dispatchers.Default) {
                withNativeFunctions()
                evaluate<Any?>(polyfillCode(module.sourceName))
                evaluate<Any?>(wrapJsCode(module.jsCode))

                val escaped = url.replace("\\", "\\\\").replace("\"", "\\\"")

                if (isAsync || module.streamAsyncJS) {
                    evaluate<Any?>("""
                        (async function() {
                            try {
                                var fn = module.exports.extractStreamUrl || globalThis.extractStreamUrl;
                                if (!fn) { __capture_result('""'); return; }
                                var r = await fn("$escaped");
                                __capture_result(JSON.stringify(typeof r === 'string' ? r : (r ? String(r) : "")));
                            } catch(e) { __capture_result('""'); }
                        })();
                    """.trimIndent())
                } else {
                    val html = httpGetText(url)
                    val eh = html.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r")
                    evaluate<Any?>("""
                        (function() {
                            try {
                                var fn = module.exports.extractStreamUrl || globalThis.extractStreamUrl;
                                if (!fn) { __capture_result('""'); return; }
                                var r = fn("$eh");
                                __capture_result(JSON.stringify(typeof r === 'string' ? r : (r ? String(r) : "")));
                            } catch(e) { __capture_result('""'); }
                        })();
                    """.trimIndent())
                }
            }
            val streamUrl = json.parseToJsonElement(capturedResult).jsonPrimitive.contentOrNull
            return if (streamUrl.isNullOrBlank() || streamUrl == "null") null else streamUrl
        } catch (e: Exception) {
            log.e(e) { "Sora stream resolution failed for ${module.sourceName}" }
            return null
        }
    }

    private fun QuickJs.withNativeFunctions() {
        define("console") {
            function("log") { a -> log.d { "SoraJS ${a.joinToString(" ") { it?.toString() ?: "null" }}" }; null }
            function("error") { a -> log.e { "SoraJS ${a.joinToString(" ") { it?.toString() ?: "null" }}" }; null }
            function("warn") { a -> log.w { "SoraJS ${a.joinToString(" ") { it?.toString() ?: "null" }}" }; null }
            function("info") { a -> log.i { "SoraJS ${a.joinToString(" ") { it?.toString() ?: "null" }}" }; null }
            function("debug") { a -> log.d { "SoraJS ${a.joinToString(" ") { it?.toString() ?: "null" }}" }; null }
        }

        function("__native_fetch") { args ->
            val url = args.getOrNull(0)?.toString() ?: ""
            val method = args.getOrNull(1)?.toString() ?: "GET"
            val hJson = args.getOrNull(2)?.toString() ?: "{}"
            val body = args.getOrNull(3)?.toString() ?: ""
            val follow = args.getOrNull(4) as? Boolean ?: true
            try { nativeFetch(url, method, hJson, body, follow) }
            catch (t: Throwable) {
                JsonObject(mapOf("ok" to JsonPrimitive(false), "status" to JsonPrimitive(0),
                    "statusText" to JsonPrimitive(t.message ?: "Fetch failed"), "url" to JsonPrimitive(url),
                    "body" to JsonPrimitive(""), "headers" to JsonObject(emptyMap()))).toString()
            }
        }

        function("__crypto_digest_hex") { a ->
            runCatching { pluginDigestHex(a.getOrNull(0)?.toString() ?: "SHA256", a.getOrNull(1)?.toString() ?: "") }.getOrDefault("")
        }
        function("__crypto_hmac_hex") { a ->
            runCatching { pluginHmacHex(a.getOrNull(0)?.toString() ?: "SHA256", a.getOrNull(1)?.toString() ?: "", a.getOrNull(2)?.toString() ?: "") }.getOrDefault("")
        }
        function("__crypto_base64_encode") { a -> runCatching { pluginBase64Encode(a.getOrNull(0)?.toString() ?: "") }.getOrDefault("") }
        function("__crypto_base64_decode") { a -> runCatching { pluginBase64Decode(a.getOrNull(0)?.toString() ?: "") }.getOrDefault("") }
        function("__crypto_utf8_to_hex") { a -> runCatching { pluginUtf8ToHex(a.getOrNull(0)?.toString() ?: "") }.getOrDefault("") }
        function("__crypto_hex_to_utf8") { a -> runCatching { pluginHexToUtf8(a.getOrNull(0)?.toString() ?: "") }.getOrDefault("") }

        function("__parse_url") { a -> parseUrl(a.getOrNull(0)?.toString() ?: "") }

        val docCache = mutableMapOf<String, Document>()
        val elemCache = mutableMapOf<String, Element>()
        var idCounter = 0
        val containsRegex = Regex(""":contains\([\"']([^\"']+)[\"']\)""")

        function("__cheerio_load") { a ->
            val html = a.getOrNull(0)?.toString() ?: ""
            val docId = "doc_${idCounter++}"
            docCache[docId] = Ksoup.parse(html)
            docId
        }

        function("__cheerio_select") { a ->
            val docId = a.getOrNull(0)?.toString() ?: ""
            var sel = a.getOrNull(1)?.toString() ?: ""
            val doc = docCache[docId] ?: return@function "[]"
            try {
                sel = sel.replace(containsRegex, ":contains($1)")
                val els = if (sel.isEmpty()) Elements() else doc.select(sel)
                val ids = els.mapIndexed { i, el ->
                    val id = "$docId:$i:${el.hashCode()}"
                    elemCache[id] = el; id
                }
                "[" + ids.joinToString(",") { "\"${it.replace("\"", "\\\"")}\"" } + "]"
            } catch (_: Exception) { "[]" }
        }

        function("__cheerio_find") { a ->
            val docId = a.getOrNull(0)?.toString() ?: ""
            val elId = a.getOrNull(1)?.toString() ?: ""
            var sel = a.getOrNull(2)?.toString() ?: ""
            val el = elemCache[elId] ?: return@function "[]"
            try {
                sel = sel.replace(containsRegex, ":contains($1)")
                val ids = el.select(sel).mapIndexed { i, e ->
                    val id = "$docId:find:$i:${e.hashCode()}"
                    elemCache[id] = e; id
                }
                "[" + ids.joinToString(",") { "\"${it.replace("\"", "\\\"")}\"" } + "]"
            } catch (_: Exception) { "[]" }
        }

        function("__cheerio_text") { a ->
            val ids = a.getOrNull(1)?.toString() ?: ""
            ids.split(",").filter(String::isNotEmpty).mapNotNull { elemCache[it]?.text() }.joinToString(" ")
        }

        function("__cheerio_html") { a ->
            val docId = a.getOrNull(0)?.toString() ?: ""
            val elId = a.getOrNull(1)?.toString() ?: ""
            (if (elId.isEmpty()) docCache[docId]?.html() else elemCache[elId]?.html()) ?: ""
        }

        function("__cheerio_attr") { a ->
            val elId = a.getOrNull(1)?.toString() ?: ""
            val name = a.getOrNull(2)?.toString() ?: ""
            val v = elemCache[elId]?.attr(name)
            if (v.isNullOrEmpty()) "__UNDEFINED__" else v
        }

        function("__cheerio_next") { a ->
            val docId = a.getOrNull(0)?.toString() ?: ""
            val elId = a.getOrNull(1)?.toString() ?: ""
            val el = elemCache[elId] ?: return@function "__NONE__"
            val next = el.nextElementSibling() ?: return@function "__NONE__"
            val id = "$docId:next:${next.hashCode()}"
            elemCache[id] = next; id
        }

        function("__cheerio_prev") { a ->
            val docId = a.getOrNull(0)?.toString() ?: ""
            val elId = a.getOrNull(1)?.toString() ?: ""
            val el = elemCache[elId] ?: return@function "__NONE__"
            val prev = el.previousElementSibling() ?: return@function "__NONE__"
            val id = "$docId:prev:${prev.hashCode()}"
            elemCache[id] = prev; id
        }

        function("__capture_result") { args ->
            capturedResult = args.getOrNull(0)?.toString() ?: "[]"
            null
        }
    }

    private fun nativeFetch(url: String, method: String, hJson: String, body: String, follow: Boolean): String {
        return try {
            val headers = runCatching {
                (json.parseToJsonElement(hJson) as? JsonObject ?: JsonObject(emptyMap()))
                    .entries.mapNotNull { (k, v) -> v.jsonPrimitive.contentOrNull?.let { k to it } }.toMap().toMutableMap()
            }.getOrDefault(mutableMapOf())
            if (!headers.containsKey("User-Agent"))
                headers["User-Agent"] = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
            val response = runBlocking { httpRequestRaw(method, url, headers, body, follow) }
            JsonObject(mapOf(
                "ok" to JsonPrimitive(response.status in 200..299),
                "status" to JsonPrimitive(response.status),
                "statusText" to JsonPrimitive(response.statusText),
                "url" to JsonPrimitive(response.url),
                "body" to JsonPrimitive(response.body),
                "headers" to JsonObject(response.headers.mapValues { JsonPrimitive(it.value) }),
            )).toString()
        } catch (error: Throwable) {
            log.e(error) { "Fetch error for $method $url" }
            JsonObject(mapOf("ok" to JsonPrimitive(false), "status" to JsonPrimitive(0),
                "statusText" to JsonPrimitive(error.message ?: "Fetch failed"), "url" to JsonPrimitive(url),
                "body" to JsonPrimitive(""), "headers" to JsonObject(emptyMap()))).toString()
        }
    }

    private fun parseUrl(urlString: String): String = try {
        val p = io.ktor.http.Url(urlString)
        JsonObject(mapOf(
            "protocol" to JsonPrimitive("${p.protocol.name}:"),
            "host" to JsonPrimitive(if (p.port != p.protocol.defaultPort) "${p.host}:${p.port}" else p.host),
            "hostname" to JsonPrimitive(p.host),
            "port" to JsonPrimitive(if (p.port != p.protocol.defaultPort) p.port.toString() else ""),
            "pathname" to JsonPrimitive(p.encodedPath.ifBlank { "/" }),
            "search" to JsonPrimitive(p.encodedQuery?.let { "?$it" } ?: ""),
            "hash" to JsonPrimitive(p.encodedFragment?.let { "#$it" } ?: ""),
        )).toString()
    } catch (_: Exception) {
        JsonObject(mapOf("protocol" to JsonPrimitive(""), "host" to JsonPrimitive(""),
            "hostname" to JsonPrimitive(""), "port" to JsonPrimitive(""), "pathname" to JsonPrimitive("/"),
            "search" to JsonPrimitive(""), "hash" to JsonPrimitive(""))).toString()
    }

    data class SoraSearchResult(val title: String, val href: String?)

    private fun parseSearchResults(raw: String): List<SoraSearchResult> = runCatching {
        (json.parseToJsonElement(raw) as? JsonArray ?: return emptyList()).mapNotNull { el ->
            val obj = el as? JsonObject ?: return@mapNotNull null
            val title = obj["title"]?.jsonPrimitive?.contentOrNull?.takeIf(String::isNotBlank) ?: return@mapNotNull null
            SoraSearchResult(title, obj["href"]?.jsonPrimitive?.contentOrNull?.takeIf(String::isNotBlank))
        }
    }.getOrDefault(emptyList())

    data class SoraEpisodeResult(val href: String?, val number: String?)

    private fun parseEpisodeResults(raw: String): List<SoraEpisodeResult> = runCatching {
        (json.parseToJsonElement(raw) as? JsonArray ?: return emptyList()).mapNotNull { el ->
            val obj = el as? JsonObject ?: return@mapNotNull null
            SoraEpisodeResult(
                obj["href"]?.jsonPrimitive?.contentOrNull?.takeIf(String::isNotBlank),
                obj["number"]?.jsonPrimitive?.contentOrNull?.takeIf(String::isNotBlank),
            )
        }
    }.getOrDefault(emptyList())

    private fun wrapJsCode(code: String): String = """
        var module = { exports: {} };
        var exports = module.exports;
        (function() { $code })();
    """.trimIndent()

    private fun polyfillCode(sourceName: String): String = """
        globalThis.SOURCE_NAME = "$sourceName";
        if (typeof globalThis.global === 'undefined') globalThis.global = globalThis;
        if (typeof globalThis.window === 'undefined') globalThis.window = globalThis;
        if (typeof globalThis.self === 'undefined') globalThis.self = globalThis;

        var fetch = async function(url, headers) {
            var h = headers || {};
            var result = __native_fetch(url, 'GET', JSON.stringify(h), '', true);
            var parsed = JSON.parse(result);
            if (!parsed.ok) throw new Error('HTTP ' + parsed.status);
            return parsed.body;
        };

        var fetchv2 = async function(url, headers, method, body) {
            var m = (method || 'GET').toUpperCase();
            var h = headers || {};
            var b = typeof body === 'object' && body !== null ? JSON.stringify(body) : (body || '');
            var result = __native_fetch(url, m, JSON.stringify(h), b, true);
            var parsed = JSON.parse(result);
            return {
                ok: parsed.ok, status: parsed.status, statusText: parsed.statusText, url: parsed.url,
                headers: { get: function(n) { return parsed.headers[n.toLowerCase()] || null; } },
                text: async function() { return parsed.body; },
                json: async function() {
                    try { return parsed.body ? JSON.parse(parsed.body) : null; } catch(e) { return null; }
                }
            };
        };

        if (typeof atob === 'undefined') {
            globalThis.atob = function(input) {
                var chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=';
                var str = String(input).replace(/=+$/, '');
                if (str.length % 4 === 1) throw new Error('InvalidCharacterError');
                var output = '', bs, bc = 0, buffer, idx = 0;
                while ((buffer = str.charAt(idx++))) {
                    buffer = chars.indexOf(buffer);
                    if (buffer === -1) continue;
                    bs = bc % 4 ? bs * 64 + buffer : buffer;
                    if (bc++ % 4) output += String.fromCharCode(255 & (bs >> ((-2 * bc) & 6)));
                }
                return output;
            };
        }
        if (typeof btoa === 'undefined') {
            globalThis.btoa = function(input) {
                var chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=';
                var str = String(input), output = '', block, charCode, idx = 0, map = chars;
                while (str.charAt(idx | 0) || (map = '=', idx % 1)) {
                    charCode = str.charCodeAt(idx += 3/4);
                    if (charCode > 0xFF) throw new Error('InvalidCharacterError');
                    block = (block << 8) | charCode;
                    output += map.charAt(63 & (block >> (8 - (idx % 1) * 8)));
                }
                return output;
            };
        }

        var URL = function(urlString, base) {
            var fullUrl = urlString;
            if (base && !/^https?:\/\//i.test(urlString)) {
                var b = typeof base === 'string' ? base : base.href;
                fullUrl = urlString.charAt(0) === '/'
                    ? (b.match(/^(https?:\/\/[^\/]+)/) || ['', ''])[1] + urlString
                    : b.replace(/\/[^\/]*$/, '/') + urlString;
            }
            var d = JSON.parse(__parse_url(fullUrl));
            this.href = fullUrl; this.protocol = d.protocol; this.host = d.host;
            this.hostname = d.hostname; this.port = d.port; this.pathname = d.pathname;
            this.search = d.search; this.hash = d.hash;
            this.origin = d.protocol + '//' + d.host;
        };
        URL.prototype.toString = function() { return this.href; };

        var cheerio = {
            load: function(html) {
                var docId = __cheerio_load(html);
                var fn = function(selector, context) {
                    if (selector && selector._elementIds) return selector;
                    if (context && context._elementIds) {
                        var all = [];
                        for (var i = 0; i < context._elementIds.length; i++)
                            all = all.concat(JSON.parse(__cheerio_find(docId, context._elementIds[i], selector)));
                        return { _elementIds: all, _docId: docId, length: all.length };
                    }
                    return { _elementIds: JSON.parse(__cheerio_select(docId, selector)), _docId: docId, length: 0 };
                };
                return fn;
            }
        };
        globalThis.cheerio = cheerio;
        globalThis.require = function(n) { return n === 'cheerio' ? cheerio : {}; };
    """.trimIndent()
}
