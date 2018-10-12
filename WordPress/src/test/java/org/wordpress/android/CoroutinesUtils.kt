package org.wordpress.android

import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.runBlocking
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.coroutines.experimental.EmptyCoroutineContext

fun <T> test(context: CoroutineContext = EmptyCoroutineContext, block: suspend CoroutineScope.() -> T) {
    runBlocking(context, block)
}

val TEST_SCOPE = CoroutineScope(Dispatchers.Unconfined)
