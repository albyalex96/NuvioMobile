package com.nuvio.app.features.telegram

import com.nuvio.app.features.streams.StreamItem

internal actual object TelegramSourceResolver {
    actual fun isEnabled(): Boolean = false

    actual suspend fun resolve(
        title: String,
        year: Int?,
        season: Int?,
        episode: Int?,
        imdbId: String,
        isMovie: Boolean
    ): List<StreamItem> = emptyList()
}
