package com.nuvio.app.features.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

internal actual object NuvioAppIconSwitcher {
    var currentIconId: String by mutableStateOf("default")
        internal set

    actual fun apply(iconId: String): Boolean {
        ThemeSettingsStorage.saveSelectedAppIconId(iconId)
        currentIconId = iconId
        return true
    }
    actual fun reapply(iconId: String): Boolean {
        ThemeSettingsStorage.saveSelectedAppIconId(iconId)
        currentIconId = iconId
        return true
    }
    actual fun closeAfterApply() = Unit
}
