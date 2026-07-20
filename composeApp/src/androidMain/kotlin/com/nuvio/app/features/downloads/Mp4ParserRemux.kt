package com.nuvio.app.features.downloads

import com.nuvio.app.core.logging.InAppLogger

internal object Mp4ParserRemux {
    fun remux(videoPath: String, audioPath: String?, outputPath: String): Boolean {
        InAppLogger.warn("Mp4ParserRemux", "skipped — streaming remux not yet implemented, falling back to MediaMuxer")
        return false
    }
}
