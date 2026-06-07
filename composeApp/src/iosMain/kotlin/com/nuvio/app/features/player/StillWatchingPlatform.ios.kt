package com.nuvio.app.features.player

import platform.Foundation.NSCalendar
import platform.Foundation.NSDate
import platform.Foundation.NSCalendarUnitHour

internal actual fun currentHour(): Int {
    val calendar = NSCalendar.currentCalendar
    return calendar.component(NSCalendarUnitHour, fromDate = NSDate())
}
