package com.nuvio.app.features.player

import com.nuvio.app.core.platform.openWebUrl

internal actual object ExternalPlayerPlatform {
    actual fun defaultPlayerId(): String? = null

    actual fun availablePlayers(): List<ExternalPlayerApp> = emptyList()

    actual fun open(
        request: ExternalPlayerPlaybackRequest,
        playerId: String?,
    ): ExternalPlayerOpenResult =
        if (openWebUrl(request.sourceUrl)) {
            ExternalPlayerOpenResult.Opened
        } else {
            ExternalPlayerOpenResult.Failed
        }

    actual fun buildIntent(
        request: ExternalPlayerPlaybackRequest,
        playerId: String?,
    ): ExternalPlayerIntentResult = ExternalPlayerIntentResult.Failed
}
