package com.nuvio.app.features.simkl

import java.util.prefs.Preferences

internal actual object SimklSyncStorage {
    private const val NODE_PATH = "/com/nuvio/app/simkl/sync"
    private const val PAYLOAD_KEY = "snapshot"

    private val prefs: Preferences = Preferences.userRoot().node(NODE_PATH)

    actual fun loadPayload(): String? =
        prefs.get(PAYLOAD_KEY, null)

    actual fun savePayload(payload: String) {
        prefs.put(PAYLOAD_KEY, payload)
        prefs.flush()
    }

    actual fun removeProfile(profileId: Int) {
        prefs.remove("${PAYLOAD_KEY}_${profileId}")
        prefs.flush()
    }
}
