package org.wordpress.android.ui.utils

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine

class ContinuationWrapperWithConcurrency<T> {

    private val continuationList = arrayListOf<CancellableContinuation<T>>()

    val isWaiting: Boolean
        get() = continuationList.isNotEmpty()

    suspend fun suspendCoroutine(
        block: (CancellableContinuation<T>) -> Unit
    ): T {
        return suspendCancellableCoroutine<T> {
            continuationList.add(it)
            block.invoke(it)
        }
    }

    fun continueWith(t: T) {
        continuationList.removeFirstOrNull()?.let {
            if (it.isActive) {
                it.resume(t, null)
            }
        }

    }
}
