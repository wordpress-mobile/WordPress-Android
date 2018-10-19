package org.wordpress.android.viewmodel

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.Main
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext
import kotlin.coroutines.experimental.CoroutineContext

abstract class ScopedViewModel : ViewModel(), CoroutineScope {
    protected var job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onCleared() {
        super.onCleared()
        job.cancel()
    }

    protected suspend fun <T> MutableLiveData<T>.setOnUi(value: T) = withContext(coroutineContext) {
        setValue(value)
    }

    private fun <T> MutableLiveData<T>.postOnUi(value: T) {
        val liveData = this
        launch {
            liveData.value = value
        }
    }
}
