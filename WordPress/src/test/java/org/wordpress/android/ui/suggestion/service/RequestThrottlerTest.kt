package org.wordpress.android.ui.suggestion.service

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RequestThrottlerTest {
    private val currentMs: Long = 1000000
    private val key: Long = 123
    private val longEnoughToRefresh = 60 * 1000.toLong()

    lateinit var timeFunction: () -> Long
    lateinit var requestThrottler: RequestThrottler<Long>

    @Before
    fun setUp() {
        timeFunction = mock()
        requestThrottler = RequestThrottler(longEnoughToRefresh, timeFunction)
    }

    @Test
    fun `results are stale when no responses have been received`() {
        whenever(timeFunction())
                .thenReturn(currentMs)

        assertTrue(requestThrottler.areResultsStale(key))
    }

    @Test
    fun `results are stale even if results were received for a different site recently`() {
        whenever(timeFunction())
                .thenReturn(currentMs - 1)
                .thenReturn(currentMs)

        val differentSiteId = key + 1
        requestThrottler.onResponseReceived(differentSiteId)
        assertTrue(requestThrottler.areResultsStale(key))
    }

    @Test
    fun `results are stale when enough time has passed since last successful response`() {
        whenever(timeFunction())
                .thenReturn(currentMs - longEnoughToRefresh)
                .thenReturn(currentMs)
        requestThrottler.onResponseReceived(key)
        assertTrue(requestThrottler.areResultsStale(key))
    }

    @Test
    fun `results are not stale when not enough time has passed since last successful response`() {
        val notLongEnoughToRefresh = currentMs - longEnoughToRefresh + 1
        whenever(timeFunction())
                .thenReturn(notLongEnoughToRefresh)
                .thenReturn(currentMs)

        requestThrottler.onResponseReceived(key)
        assertFalse(requestThrottler.areResultsStale(key))
    }
}
