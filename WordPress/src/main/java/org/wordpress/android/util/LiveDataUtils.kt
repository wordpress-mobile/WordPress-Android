package org.wordpress.android.util

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.Transformations
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
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
 * @param bgDispatcher context to run the merger function in
 * @param sourceA first source
 * @param sourceB second source
 * @return new data source
 */
fun <T, U, V> mergeAsyncNotNull(
    scope: CoroutineScope,
    sourceA: LiveData<T>,
    sourceB: LiveData<U>,
    merger: suspend (T, U) -> V
): LiveData<V> {
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
    return mediator.mapAsync(scope) { (dataA, dataB) ->
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
fun <T, U, V> merge(sourceA: LiveData<T>, sourceB: LiveData<U>, merger: (T?, U?) -> V?): LiveData<V> {
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
 * Merges two LiveData sources using a given function. The function returns an object of a new type.
 * @param sourceA first source
 * @param sourceB second source
 * @return new data source
 */
fun <T> merge(sourceA: LiveData<T>?, sourceB: LiveData<T>?): MediatorLiveData<T> {
    val mediator = MediatorLiveData<T>()
    if (sourceA != null) {
        mediator.addSource(sourceA) {
            mediator.value = it
        }
    }
    if (sourceB != null) {
        mediator.addSource(sourceB) {
            mediator.value = it
        }
    }
    return mediator
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
    data class TripleContainer(val first: S? = null, val second: T? = null, val third: U? = null)

    val mediator = MediatorLiveData<TripleContainer>()
    mediator.value = TripleContainer()
    mediator.addSource(sourceA) {
        val container = mediator.value
        if (container?.first != it || !distinct) {
            mediator.value = container?.copy(first = it)
        }
    }
    mediator.addSource(sourceB) {
        val container = mediator.value
        if (container?.second != it || !distinct) {
            mediator.value = container?.copy(second = it)
        }
    }
    mediator.addSource(sourceC) {
        val container = mediator.value
        if (container?.third != it || !distinct) {
            mediator.value = container?.copy(third = it)
        }
    }
    return mediator.map { (first, second, third) -> merger(first, second, third) }
}

/**
 * Merges four LiveData sources using a given function. The function returns an object of a new type.
 * @param sourceA first source
 * @param sourceB second source
 * @param sourceC third source
 * @param sourceD fourth source
 * @return new data source
 */
fun <S, T, U, V, W> merge(
    sourceA: LiveData<S>,
    sourceB: LiveData<T>,
    sourceC: LiveData<U>,
    sourceD: LiveData<V>,
    distinct: Boolean = false,
    merger: (S?, T?, U?, V?) -> W?
): LiveData<W> {
    data class FourItemContainer(
        val first: S? = null,
        val second: T? = null,
        val third: U? = null,
        val fourth: V? = null
    )

    val mediator = MediatorLiveData<FourItemContainer>()
    mediator.value = FourItemContainer()
    mediator.addSource(sourceA) {
        val container = mediator.value
        if (container?.first != it || !distinct) {
            mediator.value = container?.copy(first = it)
        }
    }
    mediator.addSource(sourceB) {
        val container = mediator.value
        if (container?.second != it || !distinct) {
            mediator.value = container?.copy(second = it)
        }
    }
    mediator.addSource(sourceC) {
        val container = mediator.value
        if (container?.third != it || !distinct) {
            mediator.value = container?.copy(third = it)
        }
    }
    mediator.addSource(sourceD) {
        val container = mediator.value
        if (container?.fourth != it || !distinct) {
            mediator.value = container?.copy(fourth = it)
        }
    }
    return mediator.map { (first, second, third, fourth) -> merger(first, second, third, fourth) }
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
 * A wrapper of the map utility method that is null safe and runs the mapping on a background thread
 * @param scope defines the scope to run mapping in
 */
fun <T, U> LiveData<T>.mapAsync(scope: CoroutineScope, mapper: suspend (T) -> U?): MediatorLiveData<U> {
    val result = MediatorLiveData<U>()
    result.addSource(this) { x ->
        scope.launch {
            val mappedValue = x?.let { mapper(x) }
            result.postValue(mappedValue)
        }
    }
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
        if ((it != mediatorLiveData.value || !distinct) && it != null) {
            mediatorLiveData.postValue(it)
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
 * Suppresses the first n items by this [LiveData].
 *
 * Consider this for example:
 *
 * ```
 * val connectionStatusLiveData = getConnectionStatusLiveData()
 * connectionStatusLiveData.skip(1).observe(this, Observer {
 *     refresh()
 * })
 * ```
 *
 * The first value emitted by `connectionStatusLiveData` would be ignored and [Observer] will not be called.
 */
fun <T> LiveData<T>.skip(times: Int): LiveData<T> {
    check(times > 0) { "The number of times to skip must be greater than 0" }

    var skipped = 0
    val mediator = MediatorLiveData<T>()
    mediator.addSource(this) { value ->
        skipped += 1

        if (skipped > times) {
            mediator.value = value
        }
    }

    return mediator
}
