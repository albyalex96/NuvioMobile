package com.nuvio.app.features.details

import com.nuvio.app.core.storage.DesktopStorage

internal actual object SeasonViewModeStorage {
    private val store = DesktopStorage.store("nuvio_season_view_mode")

    actual fun load(): SeasonViewMode? {
        val raw = store.getString("season_view_mode")
        return SeasonViewMode.parse(raw)
    }

    actual fun save(mode: SeasonViewMode) {
        store.putString("season_view_mode", SeasonViewMode.persist(mode))
    }
}
