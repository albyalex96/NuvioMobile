package com.nuvio.app.core.logging

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity

class InAppLogWriter : LogWriter() {

    override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
        val level = when (severity) {
            Severity.Verbose, Severity.Debug -> InAppLogLevel.Debug
            Severity.Info -> InAppLogLevel.Info
            Severity.Warn -> InAppLogLevel.Warn
            Severity.Error, Severity.Assert -> InAppLogLevel.Error
        }
        val safeTag = tag.ifBlank { "Kermit" }
        val fullMessage = buildString {
            append(message)
            if (throwable != null) {
                val cause = throwable.message ?: throwable.javaClass.simpleName
                if (cause.isNotBlank()) {
                    append(" | ")
                    append(cause)
                }
            }
        }
        InAppLogger.log(level, safeTag, fullMessage)
    }
}
