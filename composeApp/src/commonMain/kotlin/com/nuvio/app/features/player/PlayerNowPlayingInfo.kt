package com.nuvio.app.features.player

data class PlayerNowPlayingInfo(
    val title: String,
    val subtitle: String? = null,
    val artworkUrl: String? = null,
)

internal interface NowPlayingMetadataController {
    fun updateNowPlayingMetadata(info: PlayerNowPlayingInfo)
    fun clearNowPlayingInfo()
}

internal fun PlayerEngineController.updateNowPlayingMetadata(info: PlayerNowPlayingInfo) {
    (this as? NowPlayingMetadataController)?.updateNowPlayingMetadata(info)
}

internal fun PlayerEngineController.clearNowPlayingInfo() {
    (this as? NowPlayingMetadataController)?.clearNowPlayingInfo()
}
