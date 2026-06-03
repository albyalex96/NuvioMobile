package com.nuvio.app.features.watchprogress

import com.nuvio.app.WebStorage

internal actual object ResumePromptStorage {
    private const val WAS_IN_PLAYER = "nuvio_rp_was_in_player"
    private const val LAST_VIDEO_ID = "nuvio_rp_last_video_id"

    actual fun loadWasInPlayer(): Boolean = WebStorage.getBoolean(WAS_IN_PLAYER) ?: false
    actual fun saveWasInPlayer(value: Boolean) { WebStorage.setBoolean(WAS_IN_PLAYER, value) }
    actual fun loadLastPlayerVideoId(): String? = WebStorage.getString(LAST_VIDEO_ID)
    actual fun saveLastPlayerVideoId(videoId: String?) {
        if (videoId != null) WebStorage.setString(LAST_VIDEO_ID, videoId) else WebStorage.remove(LAST_VIDEO_ID)
    }
}
