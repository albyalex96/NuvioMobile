package com.nuvio.app.features.downloads

internal actual object DownloadsClock {
    actual fun nowEpochMs(): Long = com.nuvio.app.nowEpochMs()
}
