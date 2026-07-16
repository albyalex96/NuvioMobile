package com.nuvio.app.features.anilist

import com.nuvio.app.core.storage.DesktopStorage

internal actual object AniListStorage {
    private val store = DesktopStorage.store("nuvio_anilist")

    actual fun loadAuthPayload(): String? = store.getString("anilist_auth_payload")

    actual fun saveAuthPayload(payload: String) = store.putString("anilist_auth_payload", payload)

    actual fun loadSettingsPayload(): String? = store.getString("anilist_settings_payload")

    actual fun saveSettingsPayload(payload: String) = store.putString("anilist_settings_payload", payload)

    actual fun loadLibraryPayload(): String? = store.getString("anilist_library_payload")

    actual fun saveLibraryPayload(payload: String) = store.putString("anilist_library_payload", payload)

    actual fun loadMappingCachePayload(): String? = store.getString("anilist_mapping_cache_payload")

    actual fun saveMappingCachePayload(payload: String) = store.putString("anilist_mapping_cache_payload", payload)

    actual fun loadMenuPrefsPayload(): String? = store.getString("anilist_menu_prefs_payload")

    actual fun saveMenuPrefsPayload(payload: String) = store.putString("anilist_menu_prefs_payload", payload)
}
