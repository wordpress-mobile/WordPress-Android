package org.wordpress.android

import kotlinx.coroutines.experimental.delay
import org.mockito.stubbing.OngoingStubbing

suspend fun <T> OngoingStubbing<T>.thenReturnSuspended(value: T) {
    delay(1000)
    thenReturn(value)
}
