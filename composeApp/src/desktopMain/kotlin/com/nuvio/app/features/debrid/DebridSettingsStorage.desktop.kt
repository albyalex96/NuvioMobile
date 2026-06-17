package com.nuvio.app.features.debrid

import com.nuvio.app.core.storage.DesktopStorage
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

internal actual object DebridSettingsStorage {
    private val store = DesktopStorage.store("nuvio_debrid_settings")
    private val json = Json { ignoreUnknownKeys = true }

    actual fun loadEnabled(): Boolean? = store.getBoolean("debrid_enabled")
    actual fun saveEnabled(enabled: Boolean) { store.putBoolean("debrid_enabled", enabled) }
    actual fun loadCloudLibraryEnabled(): Boolean? = store.getBoolean("cloud_library_enabled")
    actual fun saveCloudLibraryEnabled(enabled: Boolean) { store.putBoolean("cloud_library_enabled", enabled) }
    actual fun loadPreferredResolverProviderId(): String? = store.getString("preferred_resolver_provider_id")
    actual fun savePreferredResolverProviderId(providerId: String) { store.putString("preferred_resolver_provider_id", providerId) }
    actual fun loadProviderApiKey(providerId: String): String? = store.getString("provider_api_key_$providerId")
    actual fun saveProviderApiKey(providerId: String, apiKey: String) { store.putString("provider_api_key_$providerId", apiKey) }
    actual fun loadTorboxApiKey(): String? = store.getString("torbox_api_key")
    actual fun saveTorboxApiKey(apiKey: String) { store.putString("torbox_api_key", apiKey) }
    actual fun loadRealDebridApiKey(): String? = store.getString("real_debrid_api_key")
    actual fun saveRealDebridApiKey(apiKey: String) { store.putString("real_debrid_api_key", apiKey) }
    actual fun loadInstantPlaybackPreparationLimit(): Int? = store.getInt("instant_playback_preparation_limit")
    actual fun saveInstantPlaybackPreparationLimit(limit: Int) { store.putInt("instant_playback_preparation_limit", limit) }
    actual fun loadStreamMaxResults(): Int? = store.getInt("stream_max_results")
    actual fun saveStreamMaxResults(maxResults: Int) { store.putInt("stream_max_results", maxResults) }
    actual fun loadStreamSortMode(): String? = store.getString("stream_sort_mode")
    actual fun saveStreamSortMode(mode: String) { store.putString("stream_sort_mode", mode) }
    actual fun loadStreamMinimumQuality(): String? = store.getString("stream_minimum_quality")
    actual fun saveStreamMinimumQuality(quality: String) { store.putString("stream_minimum_quality", quality) }
    actual fun loadStreamDolbyVisionFilter(): String? = store.getString("stream_dolby_vision_filter")
    actual fun saveStreamDolbyVisionFilter(filter: String) { store.putString("stream_dolby_vision_filter", filter) }
    actual fun loadStreamHdrFilter(): String? = store.getString("stream_hdr_filter")
    actual fun saveStreamHdrFilter(filter: String) { store.putString("stream_hdr_filter", filter) }
    actual fun loadStreamCodecFilter(): String? = store.getString("stream_codec_filter")
    actual fun saveStreamCodecFilter(filter: String) { store.putString("stream_codec_filter", filter) }
    actual fun loadStreamPreferences(): String? = store.getString("stream_preferences")
    actual fun saveStreamPreferences(preferences: String) { store.putString("stream_preferences", preferences) }
    actual fun loadStreamNameTemplate(): String? = store.getString("stream_name_template")
    actual fun saveStreamNameTemplate(template: String) { store.putString("stream_name_template", template) }
    actual fun loadStreamDescriptionTemplate(): String? = store.getString("stream_description_template")
    actual fun saveStreamDescriptionTemplate(template: String) { store.putString("stream_description_template", template) }
    actual fun loadPendingDeviceAuthorization(providerId: String): String? = store.getString("pending_device_auth_$providerId")
    actual fun savePendingDeviceAuthorization(providerId: String, payload: String) { store.putString("pending_device_auth_$providerId", payload) }
    actual fun clearPendingDeviceAuthorization(providerId: String) { store.remove("pending_device_auth_$providerId") }

    actual fun exportToSyncPayload(): JsonObject {
        val map = mutableMapOf<String, String>()
        fun putOpt(key: String, value: Any?) { if (value != null) map[key] = value.toString() }
        putOpt("debrid_enabled", loadEnabled())
        putOpt("cloud_library_enabled", loadCloudLibraryEnabled())
        putOpt("preferred_resolver_provider_id", loadPreferredResolverProviderId())
        putOpt("stream_max_results", loadStreamMaxResults())
        putOpt("stream_sort_mode", loadStreamSortMode())
        putOpt("stream_minimum_quality", loadStreamMinimumQuality())
        putOpt("stream_dolby_vision_filter", loadStreamDolbyVisionFilter())
        putOpt("stream_hdr_filter", loadStreamHdrFilter())
        putOpt("stream_codec_filter", loadStreamCodecFilter())
        putOpt("stream_preferences", loadStreamPreferences())
        putOpt("stream_name_template", loadStreamNameTemplate())
        putOpt("stream_description_template", loadStreamDescriptionTemplate())
        return json.decodeFromString(json.encodeToString(map))
    }

    actual fun replaceFromSyncPayload(payload: JsonObject) {
        fun bool(key: String) = payload[key]?.toString()?.toBooleanStrictOrNull()
        fun int(key: String) = payload[key]?.toString()?.toIntOrNull()
        fun str(key: String) = payload[key]?.toString()
        bool("debrid_enabled")?.let { saveEnabled(it) }
        bool("cloud_library_enabled")?.let { saveCloudLibraryEnabled(it) }
        str("preferred_resolver_provider_id")?.let { savePreferredResolverProviderId(it) }
        int("stream_max_results")?.let { saveStreamMaxResults(it) }
        str("stream_sort_mode")?.let { saveStreamSortMode(it) }
        str("stream_minimum_quality")?.let { saveStreamMinimumQuality(it) }
        str("stream_dolby_vision_filter")?.let { saveStreamDolbyVisionFilter(it) }
        str("stream_hdr_filter")?.let { saveStreamHdrFilter(it) }
        str("stream_codec_filter")?.let { saveStreamCodecFilter(it) }
        str("stream_preferences")?.let { saveStreamPreferences(it) }
        str("stream_name_template")?.let { saveStreamNameTemplate(it) }
        str("stream_description_template")?.let { saveStreamDescriptionTemplate(it) }
    }
}
