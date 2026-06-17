package com.nuvio.app.features.player

import com.nuvio.app.core.storage.DesktopStorage
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

internal actual object PlayerTrackPreferenceStorage {
    private val store = DesktopStorage.store("nuvio_player_track_preferences")
    private val json = Json { ignoreUnknownKeys = true }

    actual fun load(contentId: String): PersistedPlayerTrackPreference? {
        return store.getString("track_pref_$contentId")?.let { payload ->
            runCatching { json.decodeFromString<PersistedPlayerTrackPreference>(payload) }.getOrNull()
        }
    }

    actual fun save(contentId: String, preference: PersistedPlayerTrackPreference) {
        store.putString("track_pref_$contentId", json.encodeToString(preference))
    }

    actual fun loadSubtitleDelayMs(videoId: String): Int? =
        store.getInt("subtitle_delay_$videoId")

    actual fun saveSubtitleDelayMs(videoId: String, delayMs: Int) {
        store.putInt("subtitle_delay_$videoId", delayMs)
    }
}
