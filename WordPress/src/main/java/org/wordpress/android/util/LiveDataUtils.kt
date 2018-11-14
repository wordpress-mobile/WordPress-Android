package org.wordpress.android.util

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MediatorLiveData
import android.arch.lifecycle.Transformations
import android.util.Log

fun <T> merge(vararg sources: LiveData<T>): LiveData<T> {
    val mediator = MediatorLiveData<T>()
    for (source in sources) {
        mediator.addSource(source) {
            mediator.value = it
        }
    }
    return mediator
}

fun <T, U, V> merge(sourceA: LiveData<T>, sourceB: LiveData<U>, merger: (T, U) -> V): LiveData<V> {
    val mediator = MediatorLiveData<Pair<T?, U?>>()
    mediator.addSource(sourceA) {
        Log.d("vojta", "Source A emits: $it")
        mediator.value = it to mediator.value?.second
    }
    mediator.addSource(sourceB) {
        Log.d("vojta", "Source B emits: $it")
        mediator.value = mediator.value?.first to it
    }
    return mediator.map { (dataA, dataB) ->
        if (dataA != null && dataB != null) {
            merger(dataA, dataB)
        } else {
            null
        }
    }
}

fun <Key, Value> mergeToMap(sources: List<Pair<Key, LiveData<Value>>>): LiveData<Map<Key, Value>> {
    val mediator = MediatorLiveData<MutableMap<Key, Value>>()
    mediator.value = mutableMapOf()
    for (source in sources) {
        mediator.addSource(source.second) {
            val value = mediator.value ?: mutableMapOf()
            if (it != null) {
                Log.d("vojta", "Source added to map emits: $it")
                value[source.first] = it
            } else {
                value.remove(source.first)
            }
            Log.d("vojta", "Map updated: ${value.values.size}")
            mediator.value = value
        }
    }
    return mediator.map { it.toMap() }
}

fun <T, U> LiveData<T>.map(mapper: (T) -> U?): LiveData<U> {
    return Transformations.map(this) {
        it?.let(mapper)
    }
}
