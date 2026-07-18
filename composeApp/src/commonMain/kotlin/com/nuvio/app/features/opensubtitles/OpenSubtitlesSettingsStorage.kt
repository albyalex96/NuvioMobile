package com.nuvio.app.features.opensubtitles

import kotlinx.serialization.json.JsonObject

internal expect object OpenSubtitlesSettingsStorage {
    fun loadEnabled(): Boolean?
    fun saveEnabled(enabled: Boolean)
    fun loadApiKey(): String?
    fun saveApiKey(apiKey: String)
    fun loadLanguages(): Set<String>?
    fun saveLanguages(languages: Set<String>)
    fun exportToSyncPayload(): JsonObject
    fun replaceFromSyncPayload(payload: JsonObject)
}
