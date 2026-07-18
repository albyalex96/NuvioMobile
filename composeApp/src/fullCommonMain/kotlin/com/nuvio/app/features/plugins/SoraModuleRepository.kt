package com.nuvio.app.features.plugins

import co.touchlab.kermit.Logger
import com.nuvio.app.features.addons.httpGetText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.sora_error_enter_url
import nuvio.composeapp.generated.resources.sora_error_fetch_failed
import nuvio.composeapp.generated.resources.sora_error_script_fetch_failed
import nuvio.composeapp.generated.resources.sora_module_already_installed
import org.jetbrains.compose.resources.getString

internal object SoraModuleRepository {
    private const val STORAGE_KEY = "sora_modules_state"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val log = Logger.withTag("SoraModuleRepository")
    private val json = Json { ignoreUnknownKeys = true }

    private val _soraModules = MutableStateFlow<List<SoraModuleItem>>(emptyList())
    val soraModules: StateFlow<List<SoraModuleItem>> = _soraModules.asStateFlow()

    fun initialize() {
        val stored = loadModules()
        _soraModules.value = stored
        log.d { "SoraModuleRepository initialized with ${stored.size} modules" }
    }

    suspend fun addModule(rawUrl: String): AddSoraModuleResult {
        val moduleUrl = rawUrl.trim()
        if (moduleUrl.isBlank()) {
            return AddSoraModuleResult.Error(getString(Res.string.sora_error_enter_url))
        }

        val normalizedUrl = if (!moduleUrl.startsWith("http://") && !moduleUrl.startsWith("https://")) {
            "https://$moduleUrl"
        } else {
            moduleUrl
        }

        if (_soraModules.value.any { it.moduleUrl == normalizedUrl }) {
            return AddSoraModuleResult.Error(getString(Res.string.sora_module_already_installed))
        }

        return try {
            val payload = httpGetText(normalizedUrl)
            val moduleJson = json.decodeFromString<SoraModuleJson>(payload)

            val scriptBaseUrl = normalizedUrl.substringBeforeLast("/")
            val scriptUrl = if (moduleJson.scriptUrl.startsWith("http://") || moduleJson.scriptUrl.startsWith("https://")) {
                moduleJson.scriptUrl
            } else {
                "$scriptBaseUrl/${moduleJson.scriptUrl.trimStart('/')}"
            }

            val jsCode = try {
                httpGetText(scriptUrl)
            } catch (e: Exception) {
                log.e(e) { "Failed to fetch script for ${moduleJson.sourceName}" }
                return AddSoraModuleResult.Error(getString(Res.string.sora_error_script_fetch_failed))
            }

            val module = SoraModuleItem(
                moduleUrl = normalizedUrl,
                sourceName = moduleJson.sourceName,
                iconUrl = moduleJson.iconUrl,
                authorName = moduleJson.author?.name,
                version = moduleJson.version,
                language = moduleJson.language,
                streamType = moduleJson.streamType,
                quality = moduleJson.quality,
                baseUrl = moduleJson.baseUrl,
                searchBaseUrl = moduleJson.searchBaseUrl,
                scriptUrl = scriptUrl,
                asyncJS = moduleJson.asyncJS ?: false,
                streamAsyncJS = moduleJson.streamAsyncJS ?: false,
                type = moduleJson.type,
                downloadSupport = moduleJson.downloadSupport ?: false,
                softsub = moduleJson.softsub ?: false,
                enabled = true,
                jsCode = jsCode,
            )

            _soraModules.update { it + module }
            persistModules()
            AddSoraModuleResult.Success(module)
        } catch (e: Exception) {
            log.e(e) { "Failed to add Sora module from $normalizedUrl" }
            AddSoraModuleResult.Error(e.message ?: getString(Res.string.sora_error_fetch_failed))
        }
    }

    fun removeModule(moduleUrl: String) {
        _soraModules.update { it.filterNot { m -> m.moduleUrl == moduleUrl } }
        persistModules()
    }

    fun toggleModule(moduleUrl: String, enabled: Boolean) {
        _soraModules.update { modules ->
            modules.map { m ->
                if (m.moduleUrl == moduleUrl) m.copy(enabled = enabled) else m
            }
        }
        persistModules()
    }

    fun refreshModule(moduleUrl: String) {
        scope.launch {
            try {
                val payload = httpGetText(moduleUrl)
                val moduleJson = json.decodeFromString<SoraModuleJson>(payload)

                val scriptBaseUrl = moduleUrl.substringBeforeLast("/")
                val scriptUrl = if (moduleJson.scriptUrl.startsWith("http://") || moduleJson.scriptUrl.startsWith("https://")) {
                    moduleJson.scriptUrl
                } else {
                    "$scriptBaseUrl/${moduleJson.scriptUrl.trimStart('/')}"
                }

                val jsCode = httpGetText(scriptUrl)

                _soraModules.update { modules ->
                    modules.map { m ->
                        if (m.moduleUrl == moduleUrl) {
                            SoraModuleItem(
                                moduleUrl = moduleUrl,
                                sourceName = moduleJson.sourceName,
                                iconUrl = moduleJson.iconUrl,
                                authorName = moduleJson.author?.name,
                                version = moduleJson.version,
                                language = moduleJson.language,
                                streamType = moduleJson.streamType,
                                quality = moduleJson.quality,
                                baseUrl = moduleJson.baseUrl,
                                searchBaseUrl = moduleJson.searchBaseUrl,
                                scriptUrl = scriptUrl,
                asyncJS = moduleJson.asyncJS ?: false,
                streamAsyncJS = moduleJson.streamAsyncJS ?: false,
                type = moduleJson.type,
                                downloadSupport = moduleJson.downloadSupport ?: false,
                                softsub = moduleJson.softsub ?: false,
                                enabled = m.enabled,
                                jsCode = jsCode,
                            )
                        } else {
                            m
                        }
                    }
                }
                persistModules()
            } catch (e: Exception) {
                log.e(e) { "Failed to refresh Sora module $moduleUrl" }
            }
        }
    }

    private fun persistModules() {
        val stored = _soraModules.value.map { m ->
            StoredSoraModule(
                moduleUrl = m.moduleUrl,
                sourceName = m.sourceName,
                iconUrl = m.iconUrl,
                authorName = m.authorName,
                version = m.version,
                language = m.language,
                streamType = m.streamType,
                quality = m.quality,
                baseUrl = m.baseUrl,
                searchBaseUrl = m.searchBaseUrl,
                scriptUrl = m.scriptUrl,
                asyncJS = m.asyncJS,
                streamAsyncJS = m.streamAsyncJS,
                type = m.type,
                downloadSupport = m.downloadSupport,
                softsub = m.softsub,
                enabled = m.enabled,
                jsCode = m.jsCode,
            )
        }
        PluginStorage.saveRaw(STORAGE_KEY, json.encodeToString(stored))
    }

    fun getEnabledModules(): List<SoraModuleItem> {
        return _soraModules.value.filter { it.enabled }
    }

    fun getEnabledModulesForType(type: String): List<SoraModuleItem> {
        val normalizedType = normalizeSoraType(type)
        return _soraModules.value.filter { module ->
            module.enabled && (module.type == null || module.type.lowercase() == normalizedType || normalizedType == "tv" && module.type?.lowercase() in setOf("anime", "shows"))
        }
    }

    private fun normalizeSoraType(type: String): String = when (type.trim().lowercase()) {
        "movie", "film" -> "movies"
        "tv", "series", "show", "tvshow" -> "shows"
        else -> type.trim().lowercase()
    }

    suspend fun executeModule(
        module: SoraModuleItem,
        tmdbId: String,
        mediaType: String,
        season: Int?,
        episode: Int?,
    ): Result<List<PluginRuntimeResult>> {
        return runCatching {
            SoraModuleRuntime.executeModule(
                module = module,
                tmdbId = tmdbId,
                mediaType = mediaType,
                season = season,
                episode = episode,
            )
        }
    }

    private fun loadModules(): List<SoraModuleItem> {
        val raw = PluginStorage.loadRaw(STORAGE_KEY)?.trim().orEmpty()
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val stored = json.decodeFromString<List<StoredSoraModule>>(raw)
            stored.map { s ->
                SoraModuleItem(
                    moduleUrl = s.moduleUrl,
                    sourceName = s.sourceName,
                    iconUrl = s.iconUrl,
                    authorName = s.authorName,
                    version = s.version,
                    language = s.language,
                    streamType = s.streamType,
                    quality = s.quality,
                    baseUrl = s.baseUrl,
                    searchBaseUrl = s.searchBaseUrl,
                    scriptUrl = s.scriptUrl,
                    asyncJS = s.asyncJS,
                    streamAsyncJS = s.streamAsyncJS,
                    type = s.type,
                    downloadSupport = s.downloadSupport,
                    softsub = s.softsub,
                    enabled = s.enabled,
                    jsCode = s.jsCode,
                )
            }
        }.getOrDefault(emptyList())
    }
}
