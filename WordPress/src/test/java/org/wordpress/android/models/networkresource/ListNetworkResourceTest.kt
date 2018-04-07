package org.wordpress.android.models.networkresource

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ListNetworkResourceTest {
    @Test
    fun testInitState() {
        val initState = ListNetworkResource.Init<String>()

        // Verify the state

        assertNull(initState.previous)
        assertTrue(initState.data.isEmpty())

        // We are not loading anything
        assertFalse(initState.isFetchingFirstPage())
        assertFalse(initState.isLoadingMore())

        // We shouldn't be able to fetch anything
        assertFalse(initState.shouldFetch(true))
        assertFalse(initState.shouldFetch(false))

        // State transitions

        val readyState = initState.ready(ArrayList())
        assertEquals(initState, readyState.previous)

        val successState = initState.success(ArrayList())
        assertEquals(initState, successState.previous)

        val loadingState = initState.loading(false)
        assertEquals(initState, loadingState.previous)

        val errorState = initState.error("error")
        assertEquals(initState, errorState.previous)
    }

    @Test
    fun testReadyState() {
        val initState = ListNetworkResource.Init<String>()
        val testData = listOf("item1", "item2")
        val readyState = ListNetworkResource.Ready(initState, testData)

        // Verify the state

        assertEquals(testData, readyState.data)
        assertEquals(initState, readyState.previous)

        // We are not loading anything
        assertFalse(readyState.isFetchingFirstPage())
        assertFalse(readyState.isLoadingMore())

        // We can refresh the first page but we can't load more
        assertFalse(readyState.shouldFetch(true))
        assertTrue(readyState.shouldFetch(false))

        // State transitions

        val successState = readyState.success(ArrayList())
        assertEquals(readyState, successState.previous)
        assertEquals(testData, successState.previous?.data)

        val loadingState = readyState.loading(false)
        assertEquals(readyState, loadingState.previous)
        assertEquals(testData, loadingState.previous?.data)

        val errorState = readyState.error("error")
        assertEquals(readyState, errorState.previous)
        assertEquals(testData, errorState.previous?.data)
    }
}
