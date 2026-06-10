package com.nuvio.app.core.auth

import com.nuvio.app.core.platform.WebKeyValueStorage

internal actual object AuthStorage {
    private const val namespace = "nuvio_auth"

    actual fun loadAnonymousUserId(): String? =
        WebKeyValueStorage.getString(namespace, "anonymous_user_id")

    actual fun saveAnonymousUserId(userId: String) {
        WebKeyValueStorage.setString(namespace, "anonymous_user_id", userId)
    }

    actual fun clearAnonymousUserId() {
        WebKeyValueStorage.remove(namespace, "anonymous_user_id")
    }
}
