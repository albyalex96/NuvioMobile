package com.nuvio.app.features.settings

import com.nuvio.app.core.storage.DesktopStorage

internal actual object SentrySettingsPlatform {
    actual val crashReportsSupported: Boolean = false
}

internal actual object SentrySettingsStorage {
    private val store = DesktopStorage.store("nuvio_sentry_settings")

    actual fun loadEnabled(): Boolean? =
        store.getBoolean("sentry_enabled")

    actual fun saveEnabled(enabled: Boolean) {
        store.putBoolean("sentry_enabled", enabled)
    }
}
