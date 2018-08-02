package org.wordpress.android.viewmodel

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import java.util.concurrent.TimeoutException
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.suspendCoroutine

fun <T> LiveData<T>.test(): TestObserver<T> {
    val mutableLiveData = MutableLiveData<T>()
    this.observeForever { mutableLiveData.postValue(it) }
    mutableLiveData.value = null
    val observer = TestObserver<T>()
    mutableLiveData.observeForever(observer)
    return observer
}

class TestObserver<T> : Observer<T> {
    private val values = mutableListOf<T?>()
    private var requiredItemCount: Int = -1
    private var continuation: Continuation<List<T>>? = null
    private var nullableContinuation: Continuation<List<T?>>? = null
    override fun onChanged(t: T?) {
        values.add(t)
        if (values.size >= requiredItemCount) {
            nullableContinuation?.resume(values.toList())
            nullableContinuation = null
        }
        if (t != null) {
            val nonNullValues = nonNullValues()
            if (nonNullValues.size >= requiredItemCount) {
                continuation?.resume(nonNullValues)
                continuation = null
            }
        }
    }

    suspend fun await(timeout: Int = 1000): T {
        return awaitValues(1, timeout)[0]
    }

    suspend fun awaitValues(count: Int, timeout: Int = 1000) = suspendCoroutine<List<T>> {
        requiredItemCount = count
        continuation = it
        launch(CommonPool) {
            delay(timeout)
            continuation?.resumeWithException(TimeoutException())
            continuation = null
        }
        val nonNullValues = nonNullValues()
        if (nonNullValues.size >= count) {
            it.resume(nonNullValues)
            continuation = null
        }
    }

    suspend fun awaitNullableValues(count: Int, timeout: Int = 1000) = suspendCoroutine<List<T?>> {
        requiredItemCount = count
        continuation = it
        launch(CommonPool) {
            delay(timeout)
            nullableContinuation?.resumeWithException(TimeoutException())
            nullableContinuation = null
        }
        if (values.size >= count) {
            it.resume(values)
            continuation = null
        }
    }

    private fun nonNullValues(): List<T> {
        return values.filter { it != null }.map { it!! }
    }
}
