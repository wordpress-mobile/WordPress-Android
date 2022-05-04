package org.wordpress.android.ui.utils

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged
import kotlin.coroutines.Continuation

class ContinuationWrapperWithConcurrency<T> {

    private val continuationList = arrayListOf<CancellableContinuation<T>>()

    suspend fun suspendCoroutine(
        block: (CancellableContinuation<T>) -> Unit
    ): T {
        return suspendCancellableCoroutine<T> {
            continuationList.add(it)
            block.invoke(it)
        }
    }

    fun continueWith(t: T) {
        continuationList[0]?.let {
            if (it.isActive) {
                it.resume(t,null)
            }
        }

    }

}
