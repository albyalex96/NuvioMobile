package com.nuvio.app.core.coroutines

import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope

internal actual fun <T> platformRunBlocking(
    context: CoroutineContext = kotlin.coroutines.EmptyCoroutineContext,
    block: suspend CoroutineScope.() -> T,
): T = kotlinx.coroutines.runBlocking(context, block)
