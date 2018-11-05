package org.wordpress.android.viewmodel

import android.arch.lifecycle.ViewModel
import kotlinx.coroutines.experimental.CoroutineDispatcher
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Job
import kotlin.coroutines.experimental.CoroutineContext

abstract class ScopedViewModel(private val defaultDispatcher: CoroutineDispatcher) : ViewModel(), CoroutineScope {
    protected var job: Job = Job()

    override val coroutineContext: CoroutineContext
        get() = defaultDispatcher + job

    override fun onCleared() {
        super.onCleared()

        job.cancel()
    }
}
