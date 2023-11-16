package org.wordpress.android

import androidx.lifecycle.LiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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

/**
 * Test helper for capturing emitted Flow values during the execution of a certain [testBody]
 *
 * @param scope should be the test [CoroutineScope]
 * @param testBody is the code that will be executed which result in flow emissions
 * @return the list of values emitted by the Flow during the executing of [testBody]
 */
suspend fun <T> Flow<T>.testCollect(scope: CoroutineScope, testBody: () -> Unit): List<T> {
    val result = mutableListOf<T>()
    val job = onEach { result.add(it) }.launchIn(scope)
    testBody()
    job.cancelAndJoin()
    return result
}
