package com.nuvio.app.features.player

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
