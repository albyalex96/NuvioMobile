package com.nuvio.app.features.livetv

import com.nuvio.app.core.storage.DesktopStorage

internal actual object LiveTvStorage {
    private val store = DesktopStorage.store("nuvio_live_tv")

    actual fun loadPlaylistUrl(): String? = store.getString("playlist_url")

    actual fun savePlaylistUrl(url: String) = store.putString("playlist_url", url)
}
