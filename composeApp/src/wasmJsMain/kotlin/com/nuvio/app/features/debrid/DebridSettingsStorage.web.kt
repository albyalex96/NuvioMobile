package com.nuvio.app.features.debrid

import com.nuvio.app.core.platform.WebKeyValueStorage
import com.nuvio.app.core.storage.ProfileScopedKey
import com.nuvio.app.core.sync.decodeSyncBoolean
import com.nuvio.app.core.sync.decodeSyncInt
import com.nuvio.app.core.sync.decodeSyncString
import com.nuvio.app.core.sync.encodeSyncBoolean
import com.nuvio.app.core.sync.encodeSyncInt
import com.nuvio.app.core.sync.encodeSyncString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal actual object DebridSettingsStorage {
    private const val namespace = "nuvio_debrid_settings"
    private const val enabledKey = "debrid_enabled"
    private const val cloudLibraryEnabledKey = "debrid_cloud_library_enabled"
    private const val preferredResolverProviderIdKey = "debrid_preferred_resolver_provider_id"
    private const val torboxApiKeyKey = "debrid_torbox_api_key"
    private const val realDebridApiKeyKey = "debrid_real_debrid_api_key"
    private const val instantPlaybackPreparationLimitKey = "debrid_instant_playback_preparation_limit"
    private const val streamMaxResultsKey = "debrid_stream_max_results"
    private const val streamSortModeKey = "debrid_stream_sort_mode"
    private const val streamMinimumQualityKey = "debrid_stream_minimum_quality"
    private const val streamDolbyVisionFilterKey = "debrid_stream_dolby_vision_filter"
    private const val streamHdrFilterKey = "debrid_stream_hdr_filter"
    private const val streamCodecFilterKey = "debrid_stream_codec_filter"
    private const val streamPreferencesKey = "debrid_stream_preferences"
    private const val streamNameTemplateKey = "debrid_stream_name_template"
    private const val streamDescriptionTemplateKey = "debrid_stream_description_template"

    actual fun loadEnabled(): Boolean? = loadBoolean(enabledKey)

    actual fun saveEnabled(enabled: Boolean) = saveBoolean(enabledKey, enabled)

    actual fun loadCloudLibraryEnabled(): Boolean? = loadBoolean(cloudLibraryEnabledKey)

    actual fun saveCloudLibraryEnabled(enabled: Boolean) = saveBoolean(cloudLibraryEnabledKey, enabled)

    actual fun loadPreferredResolverProviderId(): String? = loadString(preferredResolverProviderIdKey)

    actual fun savePreferredResolverProviderId(providerId: String) = saveString(preferredResolverProviderIdKey, providerId)

    actual fun loadProviderApiKey(providerId: String): String? =
        WebKeyValueStorage.getString(namespace, ProfileScopedKey.of("debrid_provider_apikey_$providerId"))

    actual fun saveProviderApiKey(providerId: String, apiKey: String) =
        WebKeyValueStorage.setString(namespace, ProfileScopedKey.of("debrid_provider_apikey_$providerId"), apiKey)

    actual fun loadTorboxApiKey(): String? = loadString(torboxApiKeyKey)

    actual fun saveTorboxApiKey(apiKey: String) = saveString(torboxApiKeyKey, apiKey)

    actual fun loadRealDebridApiKey(): String? = loadString(realDebridApiKeyKey)

    actual fun saveRealDebridApiKey(apiKey: String) = saveString(realDebridApiKeyKey, apiKey)

    actual fun loadInstantPlaybackPreparationLimit(): Int? = loadInt(instantPlaybackPreparationLimitKey)

    actual fun saveInstantPlaybackPreparationLimit(limit: Int) = saveInt(instantPlaybackPreparationLimitKey, limit)

    actual fun loadStreamMaxResults(): Int? = loadInt(streamMaxResultsKey)

    actual fun saveStreamMaxResults(maxResults: Int) = saveInt(streamMaxResultsKey, maxResults)

    actual fun loadStreamSortMode(): String? = loadString(streamSortModeKey)

    actual fun saveStreamSortMode(mode: String) = saveString(streamSortModeKey, mode)

    actual fun loadStreamMinimumQuality(): String? = loadString(streamMinimumQualityKey)

    actual fun saveStreamMinimumQuality(quality: String) = saveString(streamMinimumQualityKey, quality)

    actual fun loadStreamDolbyVisionFilter(): String? = loadString(streamDolbyVisionFilterKey)

    actual fun saveStreamDolbyVisionFilter(filter: String) = saveString(streamDolbyVisionFilterKey, filter)

    actual fun loadStreamHdrFilter(): String? = loadString(streamHdrFilterKey)

    actual fun saveStreamHdrFilter(filter: String) = saveString(streamHdrFilterKey, filter)

    actual fun loadStreamCodecFilter(): String? = loadString(streamCodecFilterKey)

    actual fun saveStreamCodecFilter(filter: String) = saveString(streamCodecFilterKey, filter)

    actual fun loadStreamPreferences(): String? = loadString(streamPreferencesKey)

    actual fun saveStreamPreferences(preferences: String) = saveString(streamPreferencesKey, preferences)

    actual fun loadStreamNameTemplate(): String? = loadString(streamNameTemplateKey)

    actual fun saveStreamNameTemplate(template: String) = saveString(streamNameTemplateKey, template)

    actual fun loadStreamDescriptionTemplate(): String? = loadString(streamDescriptionTemplateKey)

    actual fun saveStreamDescriptionTemplate(template: String) = saveString(streamDescriptionTemplateKey, template)

    actual fun exportToSyncPayload(): JsonObject = buildJsonObject {
        loadEnabled()?.let { put(enabledKey, encodeSyncBoolean(it)) }
        loadTorboxApiKey()?.let { put(torboxApiKeyKey, encodeSyncString(it)) }
        loadRealDebridApiKey()?.let { put(realDebridApiKeyKey, encodeSyncString(it)) }
        loadInstantPlaybackPreparationLimit()?.let { put(instantPlaybackPreparationLimitKey, encodeSyncInt(it)) }
        loadStreamNameTemplate()?.let { put(streamNameTemplateKey, encodeSyncString(it)) }
        loadStreamDescriptionTemplate()?.let { put(streamDescriptionTemplateKey, encodeSyncString(it)) }
    }

    actual fun replaceFromSyncPayload(payload: JsonObject) {
        listOf(
            enabledKey, torboxApiKeyKey, realDebridApiKeyKey,
            instantPlaybackPreparationLimitKey, streamNameTemplateKey, streamDescriptionTemplateKey,
        ).forEach { WebKeyValueStorage.remove(namespace, ProfileScopedKey.of(it)) }
        payload.decodeSyncBoolean(enabledKey)?.let(::saveEnabled)
        payload.decodeSyncString(torboxApiKeyKey)?.let(::saveTorboxApiKey)
        payload.decodeSyncString(realDebridApiKeyKey)?.let(::saveRealDebridApiKey)
        payload.decodeSyncInt(instantPlaybackPreparationLimitKey)?.let(::saveInstantPlaybackPreparationLimit)
        payload.decodeSyncString(streamNameTemplateKey)?.let(::saveStreamNameTemplate)
        payload.decodeSyncString(streamDescriptionTemplateKey)?.let(::saveStreamDescriptionTemplate)
    }

    private fun loadBoolean(key: String): Boolean? =
        WebKeyValueStorage.getBoolean(namespace, ProfileScopedKey.of(key))

    private fun saveBoolean(key: String, value: Boolean) =
        WebKeyValueStorage.setBoolean(namespace, ProfileScopedKey.of(key), value)

    private fun loadInt(key: String): Int? =
        WebKeyValueStorage.getInt(namespace, ProfileScopedKey.of(key))

    private fun saveInt(key: String, value: Int) =
        WebKeyValueStorage.setInt(namespace, ProfileScopedKey.of(key), value)

    private fun loadString(key: String): String? =
        WebKeyValueStorage.getString(namespace, ProfileScopedKey.of(key))

    private fun saveString(key: String, value: String) =
        WebKeyValueStorage.setString(namespace, ProfileScopedKey.of(key), value)
}
