package com.nuvio.app.features.watchprogress

import com.nuvio.app.core.platform.WebKeyValueStorage
import com.nuvio.app.core.storage.ProfileScopedKey

internal actual object ResumePromptStorage {
    private const val namespace = "nuvio_resume_prompt"
    private const val wasInPlayerKey = "was_in_player"
    private const val lastPlayerVideoIdKey = "last_player_video_id"

    actual fun loadWasInPlayer(): Boolean =
        WebKeyValueStorage.getBoolean(namespace, ProfileScopedKey.of(wasInPlayerKey)) ?: false

    actual fun saveWasInPlayer(value: Boolean) {
        WebKeyValueStorage.setBoolean(namespace, ProfileScopedKey.of(wasInPlayerKey), value)
    }

    actual fun loadLastPlayerVideoId(): String? =
        WebKeyValueStorage.getString(namespace, ProfileScopedKey.of(lastPlayerVideoIdKey))

    actual fun saveLastPlayerVideoId(videoId: String?) {
        val scopedKey = ProfileScopedKey.of(lastPlayerVideoIdKey)
        if (videoId == null) {
            WebKeyValueStorage.remove(namespace, scopedKey)
        } else {
            WebKeyValueStorage.setString(namespace, scopedKey, videoId)
        }
    }
}
