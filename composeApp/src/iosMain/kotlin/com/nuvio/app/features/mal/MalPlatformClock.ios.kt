package com.nuvio.app.features.mal

import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

internal actual object MalPlatformClock {
    actual fun nowEpochMs(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()
}
