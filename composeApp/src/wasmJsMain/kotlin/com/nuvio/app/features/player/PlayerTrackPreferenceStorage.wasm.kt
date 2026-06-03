package com.nuvio.app.features.player

import com.nuvio.app.WebStorage
import kotlinx.serialization.json.*

internal actual object PlayerTrackPreferenceStorage {
    private const val KEY_PREFIX = "nuvio_player_track_pref_"
    private const val DELAY_PREFIX = "nuvio_sub_delay_"

    actual fun load(contentId: String): PersistedPlayerTrackPreference? {
        val raw = WebStorage.getString("$KEY_PREFIX$contentId") ?: return null
        return try {
            val obj = Json.parseToJsonElement(raw) as? JsonObject ?: return null
            PersistedPlayerTrackPreference(
                subtitleType = obj["subtitleType"]?.let { if (it is JsonPrimitive) it.contentOrNull else null },
                subtitleLanguage = obj["subtitleLanguage"]?.let { if (it is JsonPrimitive) it.contentOrNull else null },
                subtitleName = obj["subtitleName"]?.let { if (it is JsonPrimitive) it.contentOrNull else null },
                subtitleTrackId = obj["subtitleTrackId"]?.let { if (it is JsonPrimitive) it.contentOrNull else null },
                addonSubtitleId = obj["addonSubtitleId"]?.let { if (it is JsonPrimitive) it.contentOrNull else null },
                addonSubtitleUrl = obj["addonSubtitleUrl"]?.let { if (it is JsonPrimitive) it.contentOrNull else null },
                addonSubtitleAddonName = obj["addonSubtitleAddonName"]?.let { if (it is JsonPrimitive) it.contentOrNull else null },
                audioLanguage = obj["audioLanguage"]?.let { if (it is JsonPrimitive) it.contentOrNull else null },
                audioName = obj["audioName"]?.let { if (it is JsonPrimitive) it.contentOrNull else null },
                audioTrackId = obj["audioTrackId"]?.let { if (it is JsonPrimitive) it.contentOrNull else null },
            )
        } catch (_: Exception) { null }
    }

    actual fun save(contentId: String, preference: PersistedPlayerTrackPreference) {
        val obj = buildJsonObject {
            preference.subtitleType?.let { put("subtitleType", it) }
            preference.subtitleLanguage?.let { put("subtitleLanguage", it) }
            preference.subtitleName?.let { put("subtitleName", it) }
            preference.subtitleTrackId?.let { put("subtitleTrackId", it) }
            preference.addonSubtitleId?.let { put("addonSubtitleId", it) }
            preference.addonSubtitleUrl?.let { put("addonSubtitleUrl", it) }
            preference.addonSubtitleAddonName?.let { put("addonSubtitleAddonName", it) }
            preference.audioLanguage?.let { put("audioLanguage", it) }
            preference.audioName?.let { put("audioName", it) }
            preference.audioTrackId?.let { put("audioTrackId", it) }
        }
        val raw = Json.encodeToString(JsonObject.serializer(), obj)
        WebStorage.setString("$KEY_PREFIX$contentId", raw)
    }

    actual fun loadSubtitleDelayMs(videoId: String): Int? {
        val raw = WebStorage.getString("$DELAY_PREFIX$videoId") ?: return null
        return raw.toIntOrNull()
    }

    actual fun saveSubtitleDelayMs(videoId: String, delayMs: Int) {
        WebStorage.setString("$DELAY_PREFIX$videoId", delayMs.toString())
    }
}
