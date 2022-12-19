package org.wordpress.android

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Delay
import kotlinx.coroutines.Dispatchers.Unconfined
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.mockito.Mockito
import org.mockito.kotlin.KStubbing
import org.mockito.stubbing.OngoingStubbing
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.resume

fun <T> test(context: CoroutineContext = EmptyCoroutineContext, block: suspend CoroutineScope.() -> T) {
    runBlocking(context, block)
}

@Suppress("unused")
fun <T : Any, R> KStubbing<T>.onBlocking(methodCall: suspend T.() -> R): OngoingStubbing<R> {
    return runBlocking { Mockito.`when`(mock.methodCall()) }
}

fun testScope() = CoroutineScope(Unconfined)
@InternalCoroutinesApi val TEST_DISPATCHER: CoroutineDispatcher = TestDispatcher()

@InternalCoroutinesApi
private class TestDispatcher : CoroutineDispatcher(), Delay {
    override fun scheduleResumeAfterDelay(timeMillis: Long, continuation: CancellableContinuation<Unit>) {
        continuation.resume(Unit)
    }

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        block.run()
    }
}

object TestScope : CoroutineScope {
    override val coroutineContext: CoroutineContext = Unconfined
}
