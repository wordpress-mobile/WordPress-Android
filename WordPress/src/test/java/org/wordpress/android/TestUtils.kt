package org.wordpress.android

import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.mockito.Mockito
import org.mockito.kotlin.internal.createInstance
import org.wordpress.android.viewmodel.Event
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

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

/**
 * Test helper for getting the current value from a LiveData object using an Observer without needing a lifecycle owner.
 * This is useful when testing LiveData objects that only emit when being observed (such as Mediators or when
 * Transformations are applied).
 *
 * Based on Google codelabs:
 * https://developer.android.com/codelabs/advanced-android-kotlin-training-testing-basics#8
 *
 * @param time The maximum time to wait for the LiveData to emit. Default value is 2 seconds.
 * @param timeUnit The time unit for [time]. Default value is [TimeUnit.SECONDS].
 * @return The value that was emitted by the LiveData.
 *
 * @throws TimeoutException If the LiveData doesn't emit within [time] and [timeUnit].
 */
fun <T> LiveData<T>.getOrAwaitValue(
    time: Long = 2,
    timeUnit: TimeUnit = TimeUnit.SECONDS
): T {
    var data: T? = null
    val latch = CountDownLatch(1)
    val observer = object : Observer<T> {
        override fun onChanged(value: T) {
            data = value
            latch.countDown()
            this@getOrAwaitValue.removeObserver(this)
        }
    }

    this.observeForever(observer)

    // Don't wait indefinitely if the LiveData is not set.
    if (!latch.await(time, timeUnit)) {
        throw TimeoutException("LiveData value was never set.")
    }

    @Suppress("UNCHECKED_CAST")
    return data as T
}
