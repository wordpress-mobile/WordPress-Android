package org.wordpress.android.util.coroutines

import kotlinx.coroutines.experimental.suspendCancellableCoroutine
import kotlinx.coroutines.experimental.withTimeoutOrNull
import java.util.concurrent.TimeUnit.MILLISECONDS
import kotlin.coroutines.experimental.Continuation

suspend inline fun <T> suspendCoroutineWithTimeout(
    timeout: Long,
    crossinline block: (Continuation<T>) -> Unit
) = withTimeoutOrNull(timeout, MILLISECONDS) {
    suspendCancellableCoroutine(block = block)
}
