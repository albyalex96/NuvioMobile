package com.nuvio.app.core.logging

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

private const val DEFAULT_MAX_LOG_LINES = 3_000

internal expect fun currentInAppLogTimestamp(): String

enum class InAppLogLevel(val label: String) {
    Debug("Debug"),
    Info("Info"),
    Warn("Warn"),
    Error("Error"),
}

data class InAppLogEntry(
    val timestamp: String,
    val level: InAppLogLevel,
    val tag: String,
    val category: String,
    val message: String,
    val line: String,
)

object InAppLogger {
    val maxRetainedEntries: Int = DEFAULT_MAX_LOG_LINES

    private val maxLines = DEFAULT_MAX_LOG_LINES
    private val _entries = MutableStateFlow<List<InAppLogEntry>>(emptyList())
    private val _lines = MutableStateFlow<List<String>>(emptyList())

    val entries: StateFlow<List<InAppLogEntry>> = _entries.asStateFlow()

    val lines: StateFlow<List<String>> = _lines.asStateFlow()

    fun debug(tag: String, message: String) = log(InAppLogLevel.Debug, tag, message)
    fun info(tag: String, message: String) = log(InAppLogLevel.Info, tag, message)
    fun warn(tag: String, message: String) = log(InAppLogLevel.Warn, tag, message)
    fun error(tag: String, message: String) = log(InAppLogLevel.Error, tag, message)

    fun log(level: InAppLogLevel, tag: String, message: String) {
        val trimmedMessage = message.trim()
        if (trimmedMessage.isEmpty()) return

        val timestamp = currentInAppLogTimestamp()
        val safeTag = tag.trim().ifEmpty { "App" }
        val line = buildString {
            append(timestamp)
            append(" [")
            append(level.label)
            append("] [")
            append(safeTag)
            append("] ")
            append(trimmedMessage)
        }
        appendEntry(
            InAppLogEntry(
                timestamp = timestamp,
                level = level,
                tag = safeTag,
                category = deriveCategory(safeTag),
                message = trimmedMessage,
                line = line,
            ),
        )
    }

    fun logMpv(platform: String, prefix: String, level: String, message: String) {
        val normalizedLevel = when (level.trim().lowercase()) {
            "fatal", "error" -> InAppLogLevel.Error
            "warn", "warning" -> InAppLogLevel.Warn
            "info", "status" -> InAppLogLevel.Info
            else -> InAppLogLevel.Debug
        }
        val tag = buildString {
            append("MPV")
            val safePlatform = platform.trim()
            if (safePlatform.isNotEmpty()) {
                append('/')
                append(safePlatform)
            }
            val safePrefix = prefix.trim()
            if (safePrefix.isNotEmpty()) {
                append('/')
                append(safePrefix)
            }
        }
        log(normalizedLevel, tag, message)
    }

    fun redactUrl(url: String?, maxLength: Int = 180): String {
        val raw = url?.trim().orEmpty()
        if (raw.isEmpty()) return "<empty>"
        val withoutQuery = raw.substringBefore('?')
        return if (withoutQuery.length <= maxLength) {
            withoutQuery
        } else {
            withoutQuery.take(maxLength) + "…"
        }
    }

    fun redactHeaders(headers: Map<String, String>): String {
        if (headers.isEmpty()) return "<empty>"
        return headers.keys.sorted().joinToString(",")
    }

    fun redactConfig(platform: String, type: String, message: String = ""): String {
        return if (message.isBlank()) type else "$type: $message"
    }

    fun dump(): String = entries.value.joinToString(separator = "\n") { it.line }

    fun clear() {
        _entries.value = emptyList()
        syncLines(emptyList())
    }

    private fun appendEntry(entry: InAppLogEntry) {
        _entries.update { current ->
            val updated = if (current.size < maxLines) {
                current + entry
            } else {
                current.drop(current.size - maxLines + 1) + entry
            }
            syncLines(updated)
            updated
        }
    }

    private fun syncLines(entries: List<InAppLogEntry>) {
        _lines.value = entries.map { it.line }
    }

    private fun deriveCategory(tag: String): String {
        val root = tag.substringBefore('/').substringBefore(':').trim()
        val normalized = root.lowercase()
        return when (normalized) {
            "mpv", "exoplayer", "player" -> "Player"
            "streams", "stream" -> "Streams"
            "addons", "addon" -> "Addon"
            "plugins", "plugin" -> "Plugin"
            "metadata", "tmdb", "mdblist", "trakt" -> "Metadata"
            "network", "http" -> "Network"
            "app", "application" -> "App"
            else -> root.toTitleCaseOrDefault("Other")
        }
    }

    private fun String.toTitleCaseOrDefault(default: String): String {
        val value = trim()
        if (value.isEmpty()) return default
        return value.take(1).uppercase() + value.drop(1).lowercase()
    }
}

object InAppLogBridge {
    fun debug(tag: String, message: String) = InAppLogger.debug(tag, message)
    fun info(tag: String, message: String) = InAppLogger.info(tag, message)
    fun warn(tag: String, message: String) = InAppLogger.warn(tag, message)
    fun error(tag: String, message: String) = InAppLogger.error(tag, message)

    fun mpv(platform: String, prefix: String, level: String, message: String) {
        InAppLogger.logMpv(platform = platform, prefix = prefix, level = level, message = message)
    }
}
