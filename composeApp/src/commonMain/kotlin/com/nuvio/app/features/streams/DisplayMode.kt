package com.nuvio.app.features.streams

enum class DisplayMode {
    POLISHED,
    ORIGINAL;

    companion object {
        fun fromString(value: String?): DisplayMode =
            entries.firstOrNull { it.name == value } ?: ORIGINAL
    }
}