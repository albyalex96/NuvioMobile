package com.nuvio.app.features.simkl

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.prefs.Preferences

internal actual object SimklPlatformClock {
    actual fun nowEpochMs(): Long = System.currentTimeMillis()
}

internal actual object SimklPkceCrypto {
    private val secureRandom = SecureRandom()

    actual fun secureRandomBytes(size: Int): ByteArray =
        ByteArray(size).also(secureRandom::nextBytes)

    actual fun sha256(value: ByteArray): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(value)
}

internal actual object SimklAuthStorage {
    private const val NODE_PATH = "/com/nuvio/app/simkl/auth"
    private const val METADATA_KEY = "metadata"
    private const val ACCESS_TOKEN_KEY = "access_token"
    private const val CODE_VERIFIER_KEY = "code_verifier"

    private val prefs: Preferences = Preferences.userRoot().node(NODE_PATH)

    actual fun loadMetadataPayload(): String? =
        prefs.get(METADATA_KEY, null)

    actual fun saveMetadataPayload(payload: String) {
        prefs.put(METADATA_KEY, payload)
        prefs.flush()
    }

    actual fun loadAccessToken(): String? =
        loadEncrypted(ACCESS_TOKEN_KEY)

    actual fun saveAccessToken(value: String?) =
        saveEncrypted(ACCESS_TOKEN_KEY, value)

    actual fun loadCodeVerifier(): String? =
        loadEncrypted(CODE_VERIFIER_KEY)

    actual fun saveCodeVerifier(value: String?) =
        saveEncrypted(CODE_VERIFIER_KEY, value)

    actual fun removeProfile(profileId: Int) {
        listOf(METADATA_KEY, ACCESS_TOKEN_KEY, CODE_VERIFIER_KEY).forEach { key ->
            prefs.remove(scoped(key, profileId))
        }
        prefs.flush()
    }

    private fun loadEncrypted(key: String): String? {
        val stored = prefs.get(scoped(key), null) ?: return null
        return runCatching { String(Base64.getDecoder().decode(stored)) }.getOrNull()
    }

    private fun saveEncrypted(key: String, value: String?) {
        if (value.isNullOrBlank()) {
            prefs.remove(scoped(key))
        } else {
            prefs.put(scoped(key), Base64.getEncoder().encodeToString(value.encodeToByteArray()))
        }
        prefs.flush()
    }

    private fun scoped(key: String, profileId: Int? = null): String =
        if (profileId != null) "${key}_${profileId}" else key
}
