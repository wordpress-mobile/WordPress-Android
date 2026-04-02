package org.wordpress.android.viewmodel

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer

/**
 * A lifecycle-aware observable that sends only new updates after subscription, used for events like
 * navigation and SnackBar messages.
 *
 *
 * This avoids a common problem with events: on configuration change (like rotation) an update
 * can be emitted if the observer is active.
 *
 *
 * Note that only one observer can be subscribed.
 */
class SingleEventObservable<T>(private val sourceLiveData: LiveData<T>) {
    var lastEvent: T? = null
        private set

    @Suppress("UseCheckOrError")
    fun observe(owner: LifecycleOwner, observer: Observer<T>) {
        if (sourceLiveData.hasObservers()) {
            throw IllegalStateException("SingleEventObservable can be observed only by a single observer.")
        }
        sourceLiveData.observe(owner, Observer {
            if (it !== lastEvent) {
                lastEvent = it
                observer.onChanged(it)
            }
        })
    }

    @Suppress("UseCheckOrError")
    fun observeForever(observer: Observer<T>) {
        if (sourceLiveData.hasObservers()) {
            throw IllegalStateException("SingleEventObservable can be observed only by a single observer.")
        }
        sourceLiveData.observeForever {
            if (it !== lastEvent) {
                lastEvent = it
                observer.onChanged(it)
            }
        }
    }
}
