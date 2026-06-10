package com.nuvio.app.core.coroutines

import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

actual fun <T> runBlocking(block: suspend () -> T): T {
    var completed = false
    var value: T? = null
    var failure: Throwable? = null

    block.startCoroutine(
        completion = object : Continuation<T> {
            override val context: CoroutineContext = EmptyCoroutineContext

            override fun resumeWith(result: Result<T>) {
                completed = true
                result.fold(
                    onSuccess = { value = it },
                    onFailure = { failure = it },
                )
            }
        },
    )

    if (completed) {
        failure?.let { throw it }
        @Suppress("UNCHECKED_CAST")
        return value as T
    }

    @Suppress("UNCHECKED_CAST")
    return (null as? T) ?: (Unit as? T) ?: ("" as T)
}
