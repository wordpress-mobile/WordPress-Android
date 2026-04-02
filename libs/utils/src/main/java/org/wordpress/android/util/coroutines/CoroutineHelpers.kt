package org.wordpress.android.util.coroutines

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.Continuation

suspend inline fun <T> suspendCoroutineWithTimeout(
    timeout: Long,
    crossinline block: (Continuation<T>) -> Unit
) = coroutineScope {
    withTimeoutOrNull(timeout) {
        suspendCancellableCoroutine(block = block)
    }
}
