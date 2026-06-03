package com.nuvio.app.core.coroutines

import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope

internal actual fun <T> platformRunBlocking(
    context: CoroutineContext,
    block: suspend CoroutineScope.() -> T,
): T {
    throw UnsupportedOperationException("platformRunBlocking is not available on wasmJs target")
}
