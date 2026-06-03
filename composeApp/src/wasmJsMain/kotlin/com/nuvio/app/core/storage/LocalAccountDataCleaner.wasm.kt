package com.nuvio.app.core.storage

internal actual object PlatformLocalAccountDataCleaner {
    actual fun wipe() {
        com.nuvio.app.WebStorage.run {
            // Clear all nuvio-prefixed keys
            val keysToRemove = mutableListOf<String>()
            for (i in 0 until kotlinx.browser.localStorage.length) {
                val key = kotlinx.browser.localStorage.key(i)
                if (key != null && key.startsWith("nuvio_")) {
                    keysToRemove.add(key)
                }
            }
            keysToRemove.forEach { remove(it) }
        }
    }
}
