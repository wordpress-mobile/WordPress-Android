package org.wordpress.android.viewmodel

import android.arch.lifecycle.ViewModel
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.HandlerDispatcher
import kotlinx.coroutines.experimental.android.Main
import kotlin.coroutines.experimental.CoroutineContext

abstract class ScopedViewModel(private val defaultDispatcher: HandlerDispatcher) : ViewModel(), CoroutineScope {
    protected var job: Job = Job()

    override val coroutineContext: CoroutineContext
        get() = defaultDispatcher + job

    override fun onCleared() {
        super.onCleared()

        job.cancel()
    }
}
