package com.nuvio.app.features.details

import com.nuvio.app.core.platform.WebKeyValueStorage
import com.nuvio.app.core.storage.ProfileScopedKey

internal actual object SeasonViewModeStorage {
    private const val namespace = "nuvio_season_view_mode"
    private const val modeKey = "season_view_mode"

    actual fun load(): SeasonViewMode? =
        SeasonViewMode.parse(WebKeyValueStorage.getString(namespace, ProfileScopedKey.of(modeKey)))

    actual fun save(mode: SeasonViewMode) {
        WebKeyValueStorage.setString(namespace, ProfileScopedKey.of(modeKey), SeasonViewMode.persist(mode))
    }
}
