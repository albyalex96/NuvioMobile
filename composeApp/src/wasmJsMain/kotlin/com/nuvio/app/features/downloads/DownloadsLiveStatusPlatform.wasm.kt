package com.nuvio.app.features.downloads

internal actual object DownloadsLiveStatusPlatform {
    actual fun onItemsChanged(items: List<DownloadItem>) {
        // No-op: no live status notification on web
    }
}
