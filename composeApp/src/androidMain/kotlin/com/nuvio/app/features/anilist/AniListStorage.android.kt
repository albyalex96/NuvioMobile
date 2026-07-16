package com.nuvio.app.features.anilist

import android.content.Context
import android.content.SharedPreferences
import com.nuvio.app.core.storage.ProfileScopedKey

internal actual object AniListStorage {
    private const val preferencesName = "nuvio_anilist"
    private const val authPayloadKey = "anilist_auth_payload"
    private const val settingsPayloadKey = "anilist_settings_payload"
    private const val libraryPayloadKey = "anilist_library_payload"

    private var preferences: SharedPreferences? = null

    fun initialize(context: Context) {
        preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
    }

    actual fun loadAuthPayload(): String? =
        preferences?.getString(ProfileScopedKey.of(authPayloadKey), null)

    actual fun saveAuthPayload(payload: String) {
        preferences?.edit()?.putString(ProfileScopedKey.of(authPayloadKey), payload)?.apply()
    }

    actual fun loadSettingsPayload(): String? =
        preferences?.getString(ProfileScopedKey.of(settingsPayloadKey), null)

    actual fun saveSettingsPayload(payload: String) {
        preferences?.edit()?.putString(ProfileScopedKey.of(settingsPayloadKey), payload)?.apply()
    }

    actual fun loadLibraryPayload(): String? =
        preferences?.getString(ProfileScopedKey.of(libraryPayloadKey), null)

    actual fun saveLibraryPayload(payload: String) {
        preferences?.edit()?.putString(ProfileScopedKey.of(libraryPayloadKey), payload)?.apply()
    }

    private const val mappingCachePayloadKey = "anilist_mapping_cache_payload"

    actual fun loadMappingCachePayload(): String? =
        preferences?.getString(ProfileScopedKey.of(mappingCachePayloadKey), null)

    actual fun saveMappingCachePayload(payload: String) {
        preferences?.edit()?.putString(ProfileScopedKey.of(mappingCachePayloadKey), payload)?.apply()
    }

    private const val menuPrefsPayloadKey = "anilist_menu_prefs_payload"

    actual fun loadMenuPrefsPayload(): String? =
        preferences?.getString(ProfileScopedKey.of(menuPrefsPayloadKey), null)

    actual fun saveMenuPrefsPayload(payload: String) {
        preferences?.edit()?.putString(ProfileScopedKey.of(menuPrefsPayloadKey), payload)?.apply()
    }
}
