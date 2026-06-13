package com.nuvio.app.features.player.cast

data class CastDevice(
    val id: String,
    val name: String,
    val modelName: String? = null,
)

enum class CastConnectionState {
    Unavailable,
    NotConnected,
    Connecting,
    Connected,
}

data class CastMediaRequest(
    val url: String,
    val title: String,
    val subtitle: String? = null,
    val posterUrl: String? = null,
    val contentType: String? = null,
    val headers: Map<String, String> = emptyMap(),
    val startPositionMs: Long = 0L,
)

data class CastPlaybackSnapshot(
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
)

fun guessCastContentType(url: String): String {
    val path = url.substringBefore('?').substringBefore('#').lowercase()
    return when {
        path.endsWith(".m3u8") -> "application/x-mpegURL"
        path.endsWith(".mpd") -> "application/dash+xml"
        path.endsWith(".mkv") -> "video/x-matroska"
        path.endsWith(".webm") -> "video/webm"
        path.endsWith(".mov") -> "video/quicktime"
        path.endsWith(".ts") -> "video/mp2t"
        else -> "video/mp4"
    }
}
