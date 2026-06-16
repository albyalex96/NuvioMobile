package com.nuvio.app.features.downloads

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object DownloadsSettingsRepository {
    private val _downloadLocationUri = MutableStateFlow<String?>(null)
    val downloadLocationUri: StateFlow<String?> = _downloadLocationUri.asStateFlow()

    private var hasLoaded = false

    fun ensureLoaded() {
        if (hasLoaded) return
        hasLoaded = true
        _downloadLocationUri.value = DownloadsStorage.getDownloadLocationUri()
    }

    fun onProfileChanged() {
        hasLoaded = false
        ensureLoaded()
    }

    fun setDownloadLocationUri(uri: String?) {
        _downloadLocationUri.value = uri
        DownloadsStorage.setDownloadLocationUri(uri)
    }
}
