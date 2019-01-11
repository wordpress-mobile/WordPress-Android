package org.wordpress.android.viewmodel

import android.arch.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext

abstract class ScopedViewModel(private val defaultDispatcher: CoroutineDispatcher) : ViewModel(), CoroutineScope {
    protected var job: Job = Job()

    override val coroutineContext: CoroutineContext
        get() = defaultDispatcher + job

    override fun onCleared() {
        super.onCleared()

        job.cancel()
    }
}
