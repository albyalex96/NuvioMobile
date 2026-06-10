package com.nuvio.app.features.player

import com.nuvio.app.core.platform.webPreferredLanguageCodes

internal actual object DeviceLanguagePreferences {
    actual fun preferredLanguageCodes(): List<String> =
        webPreferredLanguageCodes().ifEmpty { listOf("en") }
}
