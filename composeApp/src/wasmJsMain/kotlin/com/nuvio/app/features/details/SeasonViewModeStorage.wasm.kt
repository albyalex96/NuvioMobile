package com.nuvio.app.features.details

import com.nuvio.app.WebStorage

internal actual object SeasonViewModeStorage {
    private const val KEY = "nuvio_season_view_mode"

    actual fun load(): SeasonViewMode? {
        val raw = WebStorage.getString(KEY) ?: return null
        return SeasonViewMode.parse(raw)
    }

    actual fun save(mode: SeasonViewMode) {
        WebStorage.setString(KEY, SeasonViewMode.persist(mode))
    }
}
