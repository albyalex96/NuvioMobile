package com.nuvio.app.core.auth

import com.nuvio.app.WebStorage

internal actual object AuthStorage {
    private const val KEY_ANONYMOUS_USER_ID = "anonymous_user_id"

    actual fun loadAnonymousUserId(): String? = WebStorage.getString(KEY_ANONYMOUS_USER_ID)
    actual fun saveAnonymousUserId(userId: String) { WebStorage.setString(KEY_ANONYMOUS_USER_ID, userId) }
    actual fun clearAnonymousUserId() { WebStorage.remove(KEY_ANONYMOUS_USER_ID) }
}
