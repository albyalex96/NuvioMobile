package com.nuvio.app.core.ui

import com.nuvio.app.WebStorage

internal actual object PosterCardStyleStorage {
    private const val KEY = "nuvio_poster_card_style"

    actual fun loadPayload(): String? = WebStorage.getString(KEY)
    actual fun savePayload(payload: String) { WebStorage.setString(KEY, payload) }
}
