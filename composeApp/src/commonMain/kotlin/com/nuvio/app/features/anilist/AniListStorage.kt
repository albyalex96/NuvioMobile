package com.nuvio.app.features.anilist

internal expect object AniListStorage {
    fun loadAuthPayload(): String?
    fun saveAuthPayload(payload: String)

    fun loadSettingsPayload(): String?
    fun saveSettingsPayload(payload: String)

    fun loadLibraryPayload(): String?
    fun saveLibraryPayload(payload: String)

    fun loadMappingCachePayload(): String?
    fun saveMappingCachePayload(payload: String)

    fun loadMenuPrefsPayload(): String?
    fun saveMenuPrefsPayload(payload: String)
}
