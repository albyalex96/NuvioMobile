package com.nuvio.app.features.player

import com.nuvio.app.core.platform.WebKeyValueStorage
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

private val storageJson = Json { ignoreUnknownKeys = true }

internal actual object PlayerTrackPreferenceStorage {
    private const val namespace = "nuvio_player_tracks"

    actual fun load(contentId: String): PersistedPlayerTrackPreference? {
        val raw = WebKeyValueStorage.getString(namespace, "pref_$contentId") ?: return null
        return runCatching {
            val obj = storageJson.parseToJsonElement(raw).jsonObject
            obj.toTrackPreference()
        }.getOrNull()
    }

    actual fun save(contentId: String, preference: PersistedPlayerTrackPreference) {
        val raw = runCatching {
            buildJsonObject {
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
            }.toString()
        }.getOrNull() ?: return
        WebKeyValueStorage.setString(namespace, "pref_$contentId", raw)
    }

    actual fun loadSubtitleDelayMs(videoId: String): Int? =
        WebKeyValueStorage.getInt(namespace, "sub_delay_$videoId")

    actual fun saveSubtitleDelayMs(videoId: String, delayMs: Int) =
        WebKeyValueStorage.setInt(namespace, "sub_delay_$videoId", delayMs)

    private fun JsonObject.toTrackPreference(): PersistedPlayerTrackPreference = PersistedPlayerTrackPreference(
        subtitleType = get("subtitleType")?.jsonPrimitive?.contentOrNull,
        subtitleLanguage = get("subtitleLanguage")?.jsonPrimitive?.contentOrNull,
        subtitleName = get("subtitleName")?.jsonPrimitive?.contentOrNull,
        subtitleTrackId = get("subtitleTrackId")?.jsonPrimitive?.contentOrNull,
        addonSubtitleId = get("addonSubtitleId")?.jsonPrimitive?.contentOrNull,
        addonSubtitleUrl = get("addonSubtitleUrl")?.jsonPrimitive?.contentOrNull,
        addonSubtitleAddonName = get("addonSubtitleAddonName")?.jsonPrimitive?.contentOrNull,
        audioLanguage = get("audioLanguage")?.jsonPrimitive?.contentOrNull,
        audioName = get("audioName")?.jsonPrimitive?.contentOrNull,
        audioTrackId = get("audioTrackId")?.jsonPrimitive?.contentOrNull,
    )
}
