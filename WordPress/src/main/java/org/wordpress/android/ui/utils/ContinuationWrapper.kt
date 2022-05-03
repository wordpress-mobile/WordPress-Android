package org.wordpress.android.ui.utils

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class ContinuationWrapper<T> {

    private var continuation: CancellableContinuation<T>? = null

    suspend fun suspendCoroutine(
        block: (CancellableContinuation<T>) -> Unit
    ): T {
        if (continuation != null) {
            throw IllegalStateException("The coroutine task is already in progress.")
        }
        return suspendCancellableCoroutine<T> {
            continuation = it
            block.invoke(it)
        }
    }

    fun continueWith(t: T) {
        if (continuation?.isActive == true) {
            continuation?.resume(t)
            continuation = null
        }
    }

    fun cancel() {
        if (continuation?.isActive == true) {
            continuation?.cancel()
            continuation = null
        }
    }
}
