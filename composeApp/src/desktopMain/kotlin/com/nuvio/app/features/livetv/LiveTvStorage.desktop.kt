package com.nuvio.app.features.livetv

import com.nuvio.app.core.storage.DesktopStorage

internal actual object LiveTvStorage {
    private val store = DesktopStorage.store("nuvio_live_tv")

    actual fun loadSourceUrl(): String? = store.getString("source_url")

    actual fun saveSourceUrl(url: String) = store.putString("source_url", url.ifBlank { null })

    actual fun loadFavoriteUrls(): Set<String> = store.getStringSet("favorite_urls").orEmpty()

    actual fun saveFavoriteUrls(urls: Set<String>) = store.putStringSet("favorite_urls", urls)

    actual fun loadRecentChannel(): LiveTvRecentChannel? {
        val streamUrl = store.getString("recent_channel_url").orEmpty().trim()
        val name = store.getString("recent_channel_name").orEmpty().trim()
        if (streamUrl.isBlank() || name.isBlank()) return null
        return LiveTvRecentChannel(
            streamUrl = streamUrl,
            name = name,
            logoUrl = store.getString("recent_channel_logo")?.takeIf(String::isNotBlank),
            group = store.getString("recent_channel_group").orEmpty(),
            tvgId = store.getString("recent_channel_tvg_id")?.takeIf(String::isNotBlank),
        )
    }

    actual fun saveRecentChannel(channel: LiveTvRecentChannel?) {
        if (channel == null) {
            store.putString("recent_channel_url", null)
            store.putString("recent_channel_name", null)
            store.putString("recent_channel_logo", null)
            store.putString("recent_channel_group", null)
            store.putString("recent_channel_tvg_id", null)
        } else {
            store.putString("recent_channel_url", channel.streamUrl)
            store.putString("recent_channel_name", channel.name)
            store.putString("recent_channel_logo", channel.logoUrl?.takeIf(String::isNotBlank))
            store.putString("recent_channel_group", channel.group.ifBlank { null })
            store.putString("recent_channel_tvg_id", channel.tvgId?.takeIf(String::isNotBlank))
        }
    }
}
