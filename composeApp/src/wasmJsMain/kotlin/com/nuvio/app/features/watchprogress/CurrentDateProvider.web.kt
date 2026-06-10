package com.nuvio.app.features.watchprogress

import com.nuvio.app.core.platform.webTodayIsoDate

actual object CurrentDateProvider {
    actual fun todayIsoDate(): String = webTodayIsoDate()
}
