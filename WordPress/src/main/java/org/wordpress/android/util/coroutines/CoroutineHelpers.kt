package org.wordpress.android.util.coroutines

import kotlinx.coroutines.experimental.coroutineScope
import kotlinx.coroutines.experimental.suspendCancellableCoroutine
import kotlinx.coroutines.experimental.withTimeoutOrNull
import kotlin.coroutines.experimental.Continuation

suspend inline fun <T> suspendCoroutineWithTimeout(
    timeout: Long,
    crossinline block: (Continuation<T>) -> Unit
) = coroutineScope {
    withTimeoutOrNull(timeout) {
        suspendCancellableCoroutine(block = block)
    }
}
