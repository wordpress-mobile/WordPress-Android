package org.wordpress.android.fluxc

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.mockito.Mockito.mock
import org.wordpress.android.fluxc.model.DomainModel
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

fun <T> test(context: CoroutineContext = EmptyCoroutineContext, block: suspend CoroutineScope.() -> T) {
    runBlocking(context, block)
}

val TEST_SCOPE = CoroutineScope(Dispatchers.Unconfined)

val mock: DomainModel = mock()
