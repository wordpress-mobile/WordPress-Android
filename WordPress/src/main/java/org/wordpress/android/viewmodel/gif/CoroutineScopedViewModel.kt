package org.wordpress.android.viewmodel.gif

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext

/**
 * A base class for implementing Android [ViewModel] classes that also act as a [CoroutineScope].
 *
 * One advantage of using this is that coroutine builders like [async] or [launch] will become children of the parent
 * [coroutineJob] declared in this [ViewModel]. If this [ViewModel] is cleared from memory, all the active child
 * coroutines will be automatically cancelled.
 *
 * This strategy is called _Structured Concurrency_. Learn more about it here:
 *
 * - [Structured concurrency, lifecycle and coroutine parent-child hierarchy](https://goo.gl/rH3rmt)
 * - [KotlinConf 2018 - Kotlin Coroutines in Practice by Roman Elizarov](https://www.youtube.com/watch?v=a3agLJQ6vt8)
 */
abstract class CoroutineScopedViewModel : ViewModel(), CoroutineScope {
    /**
     * The default parent [CoroutineContext] of all coroutine builders under this [ViewModel] ([CoroutineScope]).
     */
    protected val coroutineJob = Job()

    /**
     * Sets it up so that all coroutine builders like [async] and [launch] will become children of [coroutineJob]
     * and use the background thread dispatcher defined by [Dispatchers.Default].
     */
    override val coroutineContext: CoroutineContext get() = coroutineJob + Dispatchers.Default

    /**
     * Cancel all active child coroutines.
     */
    override fun onCleared() {
        super.onCleared()
        coroutineJob.cancel()
    }
}
