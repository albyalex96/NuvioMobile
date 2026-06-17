package com.nuvio.app.features.streams

import com.nuvio.app.core.storage.DesktopStorage
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

internal actual object StreamBadgeSettingsStorage {
    private val store = DesktopStorage.store("nuvio_stream_badge_settings")
    private val json = Json { ignoreUnknownKeys = true }

    actual fun loadStreamBadgeRules(): String? =
        store.getString("stream_badge_rules")

    actual fun saveStreamBadgeRules(rules: String) {
        store.putString("stream_badge_rules", rules)
    }

    actual fun loadShowFileSizeBadges(): Boolean? =
        store.getBoolean("show_file_size_badges")

    actual fun saveShowFileSizeBadges(enabled: Boolean) {
        store.putBoolean("show_file_size_badges", enabled)
    }

    actual fun loadShowAddonLogo(): Boolean? =
        store.getBoolean("show_addon_logo")

    actual fun saveShowAddonLogo(enabled: Boolean) {
        store.putBoolean("show_addon_logo", enabled)
    }

    actual fun loadStreamBadgePlacement(): String? =
        store.getString("stream_badge_placement")

    actual fun saveStreamBadgePlacement(placement: String) {
        store.putString("stream_badge_placement", placement)
    }

    actual fun loadLegacyDebridStreamBadgeRules(): String? =
        store.getString("legacy_debrid_stream_badge_rules")

    actual fun clearLegacyDebridStreamBadgeRules() {
        store.remove("legacy_debrid_stream_badge_rules")
    }

    actual fun exportToSyncPayload(): JsonObject {
        val map = mutableMapOf<String, String>()
        loadStreamBadgeRules()?.let { map["stream_badge_rules"] = it }
        loadShowFileSizeBadges()?.let { map["show_file_size_badges"] = it.toString() }
        loadShowAddonLogo()?.let { map["show_addon_logo"] = it.toString() }
        loadStreamBadgePlacement()?.let { map["stream_badge_placement"] = it }
        return json.decodeFromString(json.encodeToString(map))
    }

    actual fun replaceFromSyncPayload(payload: JsonObject) {
        payload["stream_badge_rules"]?.let { saveStreamBadgeRules(it.toString()) }
        payload["show_file_size_badges"]?.let { saveShowFileSizeBadges(it.toString().toBooleanStrictOrNull() ?: return@let) }
        payload["show_addon_logo"]?.let { saveShowAddonLogo(it.toString().toBooleanStrictOrNull() ?: return@let) }
        payload["stream_badge_placement"]?.let { saveStreamBadgePlacement(it.toString()) }
    }
}
