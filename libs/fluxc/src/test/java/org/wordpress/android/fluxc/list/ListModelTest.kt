package org.wordpress.android.fluxc.list

import org.junit.Test
import org.wordpress.android.fluxc.model.list.ListOrder
import org.wordpress.android.fluxc.model.list.ListState
import org.wordpress.android.fluxc.model.list.ListState.ERROR
import org.wordpress.android.fluxc.model.list.ListState.FETCHED
import org.wordpress.android.fluxc.model.list.ListState.NEEDS_REFRESH
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ListModelTest {
    /**
     * Tests [ListOrder.fromValue] for [ListOrder].
     */
    @Test
    fun testBasicListOrder() {
        assertEquals(ListOrder.ASC, ListOrder.fromValue("asc"))
        assertEquals(ListOrder.DESC, ListOrder.fromValue("desc"))
    }

    /**
     * Basic tests for [ListState.FETCHING_FIRST_PAGE].
     */
    @Test
    fun testFetchingFirstPage() {
        val listState = ListState.FETCHING_FIRST_PAGE
        assertTrue(listState.isFetchingFirstPage())
        assertFalse(listState.isLoadingMore())
        assertFalse(listState.canLoadMore())
    }

    /**
     * Basic tests for [ListState.LOADING_MORE].
     */
    @Test
    fun testLoadingMore() {
        val listState = ListState.LOADING_MORE
        assertFalse(listState.isFetchingFirstPage())
        assertTrue(listState.isLoadingMore())
        assertFalse(listState.canLoadMore())
    }

    /**
     * Basic tests for [ListState.NEEDS_REFRESH], [ListState.FETCHED], [ListState.ERROR]. Currently we don't have
     * custom logic for these states, these tests should be expanded if/when we add custom implementation for them.
     */
    @Test
    fun testNonSpecialStates() {
        listOf(NEEDS_REFRESH, FETCHED, ERROR).forEach { listState ->
            assertFalse(listState.isFetchingFirstPage())
            assertFalse(listState.isLoadingMore())
            assertFalse(listState.canLoadMore())
        }
    }
}
