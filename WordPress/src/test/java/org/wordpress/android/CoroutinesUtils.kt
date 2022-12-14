package org.wordpress.android

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Unconfined
import kotlinx.coroutines.runBlocking
import org.mockito.Mockito
import org.mockito.kotlin.KStubbing
import org.mockito.stubbing.OngoingStubbing
import kotlin.coroutines.CoroutineContext

@Suppress("unused")
fun <T : Any, R> KStubbing<T>.onBlocking(methodCall: suspend T.() -> R): OngoingStubbing<R> {
    return runBlocking { Mockito.`when`(mock.methodCall()) }
}

fun testScope() = CoroutineScope(Unconfined)

object TestScope : CoroutineScope {
    override val coroutineContext: CoroutineContext = Unconfined
}
