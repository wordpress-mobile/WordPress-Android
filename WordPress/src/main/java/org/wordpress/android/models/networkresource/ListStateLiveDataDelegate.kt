package org.wordpress.android.models.networkresource

import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import kotlin.reflect.KProperty

class ListStateLiveDataDelegate<T : Any>(
    private var listState: ListState<T> = ListState.Init(),
    private val liveData: MutableLiveData<ListState<T>> =  MutableLiveData()
) {
    init {
        liveData.value = listState
    }

    fun observe(owner: LifecycleOwner, observer: Observer<ListState<T>>) =
            liveData.observe(owner, observer)

    operator fun setValue(ref: Any, p: KProperty<*>, value: ListState<T>) {
        listState = value
        liveData.postValue(listState)
    }

    operator fun getValue(ref: Any, p: KProperty<*>): ListState<T> =
            listState
}
