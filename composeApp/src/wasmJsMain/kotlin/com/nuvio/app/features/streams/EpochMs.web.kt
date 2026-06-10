package com.nuvio.app.features.streams

import com.nuvio.app.core.platform.webNowEpochMs

internal actual fun epochMs(): Long = webNowEpochMs()
