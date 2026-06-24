package com.nuvio.app.core.logging

import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDate

internal actual fun currentInAppLogTimestamp(): String {
    val formatter = NSDateFormatter()
    formatter.dateFormat = "HH:mm:ss"
    return formatter.stringFromDate(NSDate())
}
