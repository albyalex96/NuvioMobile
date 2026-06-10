package com.nuvio.app.features.livetv

import com.nuvio.app.core.platform.WebKeyValueStorage

internal actual object LiveTvStorage {
    private const val namespace = "nuvio_livetv"

    actual fun loadPlaylistUrl(): String? =
        WebKeyValueStorage.getString(namespace, "playlist_url")

    actual fun savePlaylistUrl(url: String) =
        WebKeyValueStorage.setString(namespace, "playlist_url", url)
}
