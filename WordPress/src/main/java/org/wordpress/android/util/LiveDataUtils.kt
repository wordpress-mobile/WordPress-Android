package org.wordpress.android.util

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MediatorLiveData

fun <T> merge(vararg sources: LiveData<T>): LiveData<T> {
    val mediator = MediatorLiveData<T>()
    for (source in sources) {
        mediator.addSource(source) {
            mediator.value = it
        }
    }
    return mediator
}
