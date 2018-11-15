package org.wordpress.android.util

import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MediatorLiveData
import android.arch.lifecycle.Observer

fun <T> merge(vararg sources: LiveData<T>): LiveData<T> {
    val mediator = MediatorLiveData<T>()
    for (source in sources) {
        mediator.addSource(source) {
            mediator.value = it
        }
    }
    return mediator
}

/**
 * Use this in order to observe an emission only once - for example for displaying a Snackbar message
 */
fun <T : Event> LiveData<T>.observeEvent(owner: LifecycleOwner, observer: (T) -> Boolean) {
    this.observe(owner, Observer {
        if (it != null && !it.hasBeenHandled) {
            if (observer(it)) {
                it.hasBeenHandled = true
            }
        }
    })
}

open class Event(var hasBeenHandled: Boolean = false)
