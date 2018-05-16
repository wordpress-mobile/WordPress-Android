package org.wordpress.android.models.networkresource

import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import kotlin.reflect.KProperty

class ListStateLiveDataDelegate<T: Any>(
    initialState: ListState<T> = ListState.Init(),
    private val liveData: MutableLiveData< ListState<T>> =
            MutableLiveData()) {

    init {
        liveData.value = initialState
    }

    fun observe(owner: LifecycleOwner, observer: Observer<ListState<T>>) =
            liveData.observe(owner, observer)

    operator fun setValue(ref: Any, p: KProperty<*>, value: ListState<T>) {
        liveData.postValue(value)
    }

    operator fun getValue(ref: Any, p: KProperty<*>): ListState<T> =
            liveData.value!!
}
