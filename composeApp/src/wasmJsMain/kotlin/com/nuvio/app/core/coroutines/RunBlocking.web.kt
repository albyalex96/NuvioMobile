package com.nuvio.app.core.coroutines

import kotlinx.coroutines.runBlocking as coroutinesRunBlocking

actual fun <T> runBlocking(block: suspend () -> T): T =
    coroutinesRunBlocking { block() }
