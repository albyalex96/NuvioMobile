package com.nuvio.app.features.p2p

import com.nuvio.app.core.platform.WebKeyValueStorage

internal actual object P2pSettingsStorage {
    private const val namespace = "nuvio_p2p"

    actual fun loadP2pEnabled(): Boolean? =
        WebKeyValueStorage.getBoolean(namespace, "enabled")

    actual fun saveP2pEnabled(enabled: Boolean) =
        WebKeyValueStorage.setBoolean(namespace, "enabled", enabled)

    actual fun loadEnableUpload(): Boolean? =
        WebKeyValueStorage.getBoolean(namespace, "enable_upload")

    actual fun saveEnableUpload(enabled: Boolean) =
        WebKeyValueStorage.setBoolean(namespace, "enable_upload", enabled)

    actual fun loadHideTorrentStats(): Boolean? =
        WebKeyValueStorage.getBoolean(namespace, "hide_torrent_stats")

    actual fun saveHideTorrentStats(enabled: Boolean) =
        WebKeyValueStorage.setBoolean(namespace, "hide_torrent_stats", enabled)
}
