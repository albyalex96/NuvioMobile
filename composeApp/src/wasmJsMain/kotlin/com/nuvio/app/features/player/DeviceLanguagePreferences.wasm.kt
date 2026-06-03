package com.nuvio.app.features.player

internal actual object DeviceLanguagePreferences {
    actual fun preferredLanguageCodes(): List<String> {
        return kotlinx.browser.window.navigator.languages.toList()
            .filterIsInstance<String>()
            .map { it.substringBefore("-") }
            .distinct()
    }
}
