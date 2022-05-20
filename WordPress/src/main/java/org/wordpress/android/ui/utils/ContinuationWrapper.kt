package org.wordpress.android.ui.utils

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class DefaultContinuationWrapper<T> : ContinuationWrapper<T> {
    private var continuation: CancellableContinuation<T>? = null

    override val isWaiting: Boolean
        get() = continuation != null

    override suspend fun suspendCoroutine(
        block: (CancellableContinuation<T>) -> Unit
    ): T {
        if (continuation != null) {
            cancel()
        }
        return suspendCancellableCoroutine<T> {
            continuation = it
            block.invoke(it)
        }
    }

    override fun continueWith(t: T) {
        if (continuation?.isActive == true) {
            continuation?.resume(t)
            continuation = null
        }
    }

    override fun cancel() {
        if (continuation?.isActive == true) {
            continuation?.cancel()
            continuation = null
        }
    }
}

interface ContinuationWrapper<T> {
    val isWaiting: Boolean
    suspend fun suspendCoroutine(
        block: (CancellableContinuation<T>) -> Unit
    ): T
    fun continueWith(t: T)
    fun cancel()
}
