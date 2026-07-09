package com.nuvio.app.core.sync

import java.util.prefs.Preferences

actual object SyncClientIdentityStorage {
    private const val clientIdKey = "client_instance_id"
    private val prefs = Preferences.userNodeForPackage(SyncClientIdentityStorage::class.java)

    actual fun loadClientId(): String? =
        prefs.get(clientIdKey, null)

    actual fun saveClientId(clientId: String) {
        prefs.put(clientIdKey, clientId)
        prefs.sync()
    }
}
