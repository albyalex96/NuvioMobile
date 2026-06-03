package com.nuvio.app.features.watchprogress

import com.nuvio.app.WebStorage

internal actual object WatchProgressStorage {
    private const val KEY = "nuvio_watch_progress"

    actual fun loadPayload(profileId: Int): String? = WebStorage.getString("${KEY}_$profileId")
    actual fun savePayload(profileId: Int, payload: String) { WebStorage.setString("${KEY}_$profileId", payload) }
}
