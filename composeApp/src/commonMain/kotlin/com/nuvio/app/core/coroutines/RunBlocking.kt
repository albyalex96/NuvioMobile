package com.nuvio.app.core.coroutines

expect fun <T> runBlocking(block: suspend () -> T): T
