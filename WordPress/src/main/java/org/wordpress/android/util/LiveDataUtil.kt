package org.wordpress.android.util

import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer

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
