package com.nuvio.app.core.share

internal expect object SharePlatform {
    fun shareFile(filePath: String, title: String, mimeType: String)
}
