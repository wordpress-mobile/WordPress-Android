package org.wordpress.android

import androidx.lifecycle.LiveData
import org.mockito.Mockito
import org.mockito.kotlin.internal.createInstance
import org.wordpress.android.viewmodel.Event

/**
 * This method allows you to match a nullable parameter in mocked methods
 */
inline fun <reified T : Any> anyNullable(): T? {
    return Mockito.any(T::class.java) ?: createInstance()
}

fun <T> LiveData<T>.toList(): MutableList<T> {
    val list = mutableListOf<T>()
    this.observeForever {
        it?.let { list.add(it) }
    }
    return list
}

fun <T> LiveData<Event<T>>.eventToList(): MutableList<T> {
    val list = mutableListOf<T>()
    this.observeForever { event ->
        event?.getContentIfNotHandled()?.let { list.add(it) }
    }
    return list
}
