package com.nuvio.app.features.livetv

internal expect object LiveTvStorage {
    fun loadSourceUrl(): String?
    fun saveSourceUrl(url: String)
    fun loadFavoriteUrls(): Set<String>
    fun saveFavoriteUrls(urls: Set<String>)
    fun loadRecentChannel(): LiveTvRecentChannel?
    fun saveRecentChannel(channel: LiveTvRecentChannel?)
}

internal expect object LiveTvClock {
    fun nowEpochMs(): Long
    fun parseXmlTvTimestamp(value: String): Long?
}
