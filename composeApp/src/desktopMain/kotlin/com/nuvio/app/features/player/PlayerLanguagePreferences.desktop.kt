package com.nuvio.app.features.player

import java.util.Locale

internal actual object DeviceLanguagePreferences {
    actual fun preferredLanguageCodes(): List<String> {
        return listOf(Locale.getDefault().language)
    }
}
