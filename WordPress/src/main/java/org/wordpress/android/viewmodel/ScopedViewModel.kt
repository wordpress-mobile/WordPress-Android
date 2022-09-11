package org.wordpress.android.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

abstract class ScopedViewModel(private val defaultDispatcher: CoroutineDispatcher) : ViewModel() {
    fun launch(
        context: CoroutineContext = defaultDispatcher,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: suspend CoroutineScope.() -> Unit
    ): Job {
        return viewModelScope.launch(context, start, block)
    }
}

abstract class ScopedAndroidViewModel(application: Application, private val defaultDispatcher: CoroutineDispatcher)
    : AndroidViewModel(application) {
    fun launch(
        context: CoroutineContext = defaultDispatcher,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: suspend CoroutineScope.() -> Unit
    ): Job {
        return viewModelScope.launch(context, start, block)
    }
}
