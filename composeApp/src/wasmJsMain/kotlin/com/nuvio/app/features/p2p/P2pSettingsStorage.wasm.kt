package com.nuvio.app.features.p2p

import com.nuvio.app.WebStorage

internal actual object P2pSettingsStorage {
    private const val P2P_ENABLED_KEY = "nuvio_p2p_enabled"
    private const val ENABLE_UPLOAD_KEY = "nuvio_p2p_enable_upload"
    private const val HIDE_TORRENT_STATS_KEY = "nuvio_p2p_hide_torrent_stats"

    actual fun loadP2pEnabled(): Boolean? = WebStorage.getBoolean(P2P_ENABLED_KEY)
    actual fun saveP2pEnabled(enabled: Boolean) { WebStorage.setBoolean(P2P_ENABLED_KEY, enabled) }
    actual fun loadEnableUpload(): Boolean? = WebStorage.getBoolean(ENABLE_UPLOAD_KEY)
    actual fun saveEnableUpload(enabled: Boolean) { WebStorage.setBoolean(ENABLE_UPLOAD_KEY, enabled) }
    actual fun loadHideTorrentStats(): Boolean? = WebStorage.getBoolean(HIDE_TORRENT_STATS_KEY)
    actual fun saveHideTorrentStats(enabled: Boolean) { WebStorage.setBoolean(HIDE_TORRENT_STATS_KEY, enabled) }
}
