package org.wordpress.android.util

import android.arch.lifecycle.MediatorLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ThrottleLiveData<T> constructor(private val offset: Long = 100, private val coroutineScope: CoroutineScope) :
        MediatorLiveData<T>() {
    private var tempValue: T? = null
    private var currentJob: Job? = null

    override fun postValue(value: T) {
        if (tempValue == null || tempValue != value) {
            currentJob?.cancel()
            currentJob = coroutineScope.launch(Dispatchers.Default) {
                tempValue = value
                delay(offset)
                withContext(Dispatchers.Main) {
                    tempValue = null
                    super.postValue(value)
                }
            }
        }
    }
}
