package com.nuvio.app.features.watchprogress

actual object CurrentDateProvider {
    actual fun todayIsoDate(): String = com.nuvio.app.todayIsoDate()
}
