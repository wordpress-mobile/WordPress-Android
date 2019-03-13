package org.wordpress.android.util

import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MediatorLiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.Transformations
import kotlinx.coroutines.CoroutineScope
import org.wordpress.android.viewmodel.SingleMediatorLiveEvent

/**
 * A helper function that merges sources into a single LiveData object
 * @param sources producing an item of the same type
 * @param distinct true if all the emitted items should be distinct
 * @param singleEvent is true when each item should be shown only once
 * @return merged results from all the sources
 */
fun <T> mergeNotNull(
    vararg sources: LiveData<T>,
    distinct: Boolean = true,
    singleEvent: Boolean = false
): MediatorLiveData<T> {
    val mediator = if (singleEvent) SingleMediatorLiveEvent() else MediatorLiveData<T>()
    for (source in sources) {
        mediator.addSource(source) {
            if (mediator.value != it || !distinct) {
                mediator.value = it
            }
        }
    }
    return mediator
}

/**
 * A helper function that merges sources into a single LiveData object
 * @param sources producing an item of the same type
 * @param distinct true if all the emitted items should be distinct
 * @param singleEvent is true when each item should be shown only once
 * @return merged results from all the sources
 */
fun <T> mergeNotNull(
    sources: Iterable<LiveData<T>>,
    distinct: Boolean = true,
    singleEvent: Boolean = false
): MediatorLiveData<T> {
    val mediator = if (singleEvent) SingleMediatorLiveEvent() else MediatorLiveData<T>()
    for (source in sources) {
        mediator.addSource(source) {
            if (mediator.value != it || !distinct) {
                mediator.value = it
            }
        }
    }
    return mediator
}

/**
 * Merges two LiveData sources using a given function. The function returns an object of a new type.
 * @param sourceA first source
 * @param sourceB second source
 * @return new data source
 */
fun <T, U, V> mergeNotNull(sourceA: LiveData<T>, sourceB: LiveData<U>, merger: (T, U) -> V): LiveData<V> {
    val mediator = MediatorLiveData<Pair<T?, U?>>()
    mediator.addSource(sourceA) {
        if (mediator.value?.first != it) {
            mediator.value = it to mediator.value?.second
        }
    }
    mediator.addSource(sourceB) {
        if (mediator.value?.second != it) {
            mediator.value = mediator.value?.first to it
        }
    }
    return mediator.map { (dataA, dataB) ->
        if (dataA != null && dataB != null) {
            merger(dataA, dataB)
        } else {
            null
        }
    }
}

/**
 * Merges two LiveData sources using a given function. The function returns an object of a new type.
 * @param sourceA first source
 * @param sourceB second source
 * @return new data source
 */
fun <T, U, V> merge(sourceA: LiveData<T>, sourceB: LiveData<U>, merger: (T?, U?) -> V?): MediatorLiveData<V> {
    val mediator = MediatorLiveData<Pair<T?, U?>>()
    mediator.addSource(sourceA) {
        mediator.value = Pair(it, mediator.value?.second)
    }
    mediator.addSource(sourceB) {
        mediator.value = Pair(mediator.value?.first, it)
    }
    return mediator.map { (dataA, dataB) -> merger(dataA, dataB) }
}

/**
 * Merges three LiveData sources using a given function. The function returns an object of a new type.
 * @param sourceA first source
 * @param sourceB second source
 * @param sourceC third source
 * @return new data source
 */
fun <S, T, U, V> merge(
    sourceA: LiveData<S>,
    sourceB: LiveData<T>,
    sourceC: LiveData<U>,
    distinct: Boolean = false,
    merger: (S?, T?, U?) -> V
): MediatorLiveData<V> {
    val mediator = MediatorLiveData<Triple<S?, T?, U?>>()
    mediator.addSource(sourceA) {
        if (mediator.value?.first != it || !distinct) {
            mediator.value = Triple(it, mediator.value?.second, mediator.value?.third)
        }
    }
    mediator.addSource(sourceB) {
        if (mediator.value?.second != it || !distinct) {
            mediator.value = Triple(mediator.value?.first, it, mediator.value?.third)
        }
    }
    mediator.addSource(sourceC) {
        if (mediator.value?.third != it || !distinct) {
            mediator.value = Triple(mediator.value?.first, mediator.value?.second, it)
        }
    }
    return mediator.map { (dataA, dataB, dataC) -> merger(dataA, dataB, dataC) }
}

/**
 * Combines all the LiveData values in the given Map into one LiveData with the map of values.
 * @param sources is a map of all the live data sources in a map by a given key
 * @return one livedata instance that combines all the values into one map
 */
fun <Key, Value> combineMap(sources: Map<Key, LiveData<Value>>): MediatorLiveData<Map<Key, Value>> {
    val mediator = MediatorLiveData<MutableMap<Key, Value>>()
    mediator.value = mutableMapOf()
    for (source in sources) {
        mediator.addSource(source.value) { updatedValue ->
            val value = mediator.value ?: mutableMapOf()
            if (value[source.key] != updatedValue) {
                if (updatedValue != null) {
                    value[source.key] = updatedValue
                } else {
                    value.remove(source.key)
                }
                mediator.value = value
            }
        }
    }
    return mediator.map { it.toMap() }
}

/**
 * Simple wrapper of the map utility method that is null safe
 */
fun <T, U> LiveData<T>.map(mapper: (T) -> U?): MediatorLiveData<U> {
    val result = MediatorLiveData<U>()
    result.addSource(this) { x -> result.value = x?.let { mapper(x) } }
    return result
}

/**
 * Calls the specified function [block] with `this` value as its receiver and returns new instance of LiveData.
 */
fun <T> LiveData<T>.perform(block: LiveData<T>.(T) -> Unit): LiveData<T> {
    return Transformations.map(this) {
        block(it)
        return@map it
    }
}

/**
 * Simple wrapper of the map utility method that is null safe
 */
fun <T, U> LiveData<T>.mapNullable(mapper: (T?) -> U?): LiveData<U> {
    return Transformations.map(this) { mapper(it) }
}

/**
 * This method ensures that the LiveData instance doesn't emit the same item twice
 */
fun <T> LiveData<T>.distinct(): MediatorLiveData<T> {
    val mediatorLiveData: MediatorLiveData<T> = MediatorLiveData()
    mediatorLiveData.addSource(this) {
        if (it != mediatorLiveData.value) {
            mediatorLiveData.value = it
        }
    }
    return mediatorLiveData
}

/**
 * Call this method if you want to throttle the LiveData emissions.
 * The default implementation takes only the last emitted result after 100ms.
 */
fun <T> LiveData<T>.throttle(
    coroutineScope: CoroutineScope,
    distinct: Boolean = false,
    offset: Long = 100
): ThrottleLiveData<T> {
    val mediatorLiveData: ThrottleLiveData<T> = ThrottleLiveData(coroutineScope = coroutineScope, offset = offset)
    mediatorLiveData.addSource(this) {
        if (it != mediatorLiveData.value || !distinct) {
            mediatorLiveData.value = it
        }
    }
    return mediatorLiveData
}

/**
 * A helper function that filters data and only emits what fits the predicate
 * @param predicate
 * @return filtered result
 */
fun <T> LiveData<T>.filter(predicate: (T) -> Boolean): LiveData<T> {
    val mediator = MediatorLiveData<T>()
    mediator.addSource(this) {
        if (it != null && predicate(it)) {
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
