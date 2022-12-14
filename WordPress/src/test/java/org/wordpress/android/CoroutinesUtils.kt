package org.wordpress.android

import kotlinx.coroutines.runBlocking
import org.mockito.Mockito
import org.mockito.kotlin.KStubbing
import org.mockito.stubbing.OngoingStubbing

@Suppress("unused")
fun <T : Any, R> KStubbing<T>.onBlocking(methodCall: suspend T.() -> R): OngoingStubbing<R> {
    return runBlocking { Mockito.`when`(mock.methodCall()) }
}
