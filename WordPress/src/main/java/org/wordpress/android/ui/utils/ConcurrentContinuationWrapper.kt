package org.wordpress.android.ui.utils

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine

class ConcurrentContinuationWrapper<T> : ContinuationWrapper<T> {
    private val continuationList = arrayListOf<CancellableContinuation<T>>()

    override val isWaiting: Boolean
        get() = continuationList.isNotEmpty()

    override suspend fun suspendCoroutine(
        block: (CancellableContinuation<T>) -> Unit
    ): T {
        return suspendCancellableCoroutine<T> {
            continuationList.add(it)
            block.invoke(it)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun continueWith(t: T) {
        continuationList.removeFirstOrNull()?.let {
            if (it.isActive) {
                it.resume(t, null)
            }
        }
    }

    override fun cancel() {
        continuationList.removeFirstOrNull()?.let {
            if (it.isActive) {
                it.cancel()
            }
        }
    }
}
