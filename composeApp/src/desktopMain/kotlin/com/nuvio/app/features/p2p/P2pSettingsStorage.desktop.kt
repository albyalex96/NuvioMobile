package com.nuvio.app.features.p2p

import com.nuvio.app.core.storage.DesktopStorage

internal actual object P2pSettingsStorage {
    private val store = DesktopStorage.store("nuvio_p2p_settings")

    actual fun loadP2pEnabled(): Boolean? =
        store.getBoolean("p2p_enabled")

    actual fun saveP2pEnabled(enabled: Boolean) {
        store.putBoolean("p2p_enabled", enabled)
    }

    actual fun loadEnableUpload(): Boolean? =
        store.getBoolean("enable_upload")

    actual fun saveEnableUpload(enabled: Boolean) {
        store.putBoolean("enable_upload", enabled)
    }

    actual fun loadHideTorrentStats(): Boolean? =
        store.getBoolean("hide_torrent_stats")

    actual fun saveHideTorrentStats(enabled: Boolean) {
        store.putBoolean("hide_torrent_stats", enabled)
    }
}
