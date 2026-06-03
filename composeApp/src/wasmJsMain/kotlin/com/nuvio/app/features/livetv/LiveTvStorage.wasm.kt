package com.nuvio.app.features.livetv

import com.nuvio.app.WebStorage

internal actual object LiveTvStorage {
    private const val KEY = "nuvio_livetv_playlist_url"

    actual fun loadPlaylistUrl(): String? = WebStorage.getString(KEY)
    actual fun savePlaylistUrl(url: String) { WebStorage.setString(KEY, url) }
}
