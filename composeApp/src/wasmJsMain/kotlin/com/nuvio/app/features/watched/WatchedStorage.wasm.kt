package com.nuvio.app.features.watched

import com.nuvio.app.WebStorage

actual object WatchedStorage {
    private const val KEY = "nuvio_watched"

    actual fun loadPayload(profileId: Int): String? = WebStorage.getString("${KEY}_$profileId")
    actual fun savePayload(profileId: Int, payload: String) { WebStorage.setString("${KEY}_$profileId", payload) }
}
