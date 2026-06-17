package com.nuvio.app.core.share

internal actual object SharePlatform {
    actual fun shareFile(filePath: String, title: String, mimeType: String) = Unit
}
