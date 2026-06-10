package com.nuvio.app.features.streams

import com.nuvio.app.core.platform.WebKeyValueStorage
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal actual object StreamBadgeSettingsStorage {
    private const val namespace = "nuvio_stream_badges"

    actual fun loadStreamBadgeRules(): String? =
        WebKeyValueStorage.getString(namespace, "badge_rules")

    actual fun saveStreamBadgeRules(rules: String) =
        WebKeyValueStorage.setString(namespace, "badge_rules", rules)

    actual fun loadShowFileSizeBadges(): Boolean? =
        WebKeyValueStorage.getBoolean(namespace, "show_file_size_badges")

    actual fun saveShowFileSizeBadges(enabled: Boolean) =
        WebKeyValueStorage.setBoolean(namespace, "show_file_size_badges", enabled)

    actual fun loadStreamBadgePlacement(): String? =
        WebKeyValueStorage.getString(namespace, "badge_placement")

    actual fun saveStreamBadgePlacement(placement: String) =
        WebKeyValueStorage.setString(namespace, "badge_placement", placement)

    actual fun loadLegacyDebridStreamBadgeRules(): String? =
        WebKeyValueStorage.getString(namespace, "legacy_debrid_badge_rules")

    actual fun clearLegacyDebridStreamBadgeRules() =
        WebKeyValueStorage.remove(namespace, "legacy_debrid_badge_rules")

    actual fun exportToSyncPayload(): JsonObject = buildJsonObject {
        loadStreamBadgeRules()?.let { put("badge_rules", it) }
        loadShowFileSizeBadges()?.let { put("show_file_size_badges", it.toString()) }
        loadStreamBadgePlacement()?.let { put("badge_placement", it) }
    }

    actual fun replaceFromSyncPayload(payload: JsonObject) {
        payload["badge_rules"]?.let { saveStreamBadgeRules(it.toString().trim('"')) }
        payload["show_file_size_badges"]?.let {
            it.toString().trim('"').toBooleanStrictOrNull()?.let { v -> saveShowFileSizeBadges(v) }
        }
        payload["badge_placement"]?.let { saveStreamBadgePlacement(it.toString().trim('"')) }
    }
}
