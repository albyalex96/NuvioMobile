package com.nuvio.app.core.storage

import com.nuvio.app.core.platform.WebKeyValueStorage

internal actual object PlatformLocalAccountDataCleaner {
    actual fun wipe() {
        WebKeyValueStorage.wipeAll()
    }
}
