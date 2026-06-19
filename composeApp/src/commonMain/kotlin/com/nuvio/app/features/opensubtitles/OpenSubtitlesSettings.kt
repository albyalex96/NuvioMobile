package com.nuvio.app.features.opensubtitles

data class OpenSubtitlesSettings(
    val enabled: Boolean = false,
    val apiKey: String = "",
    val languages: Set<String> = emptySet(),
) {
    val hasApiKey: Boolean
        get() = apiKey.isNotBlank()
}
