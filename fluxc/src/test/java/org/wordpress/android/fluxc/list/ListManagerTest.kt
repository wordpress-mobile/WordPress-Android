package org.wordpress.android.fluxc.list

import com.nhaarman.mockito_kotlin.KArgumentCaptor
import com.nhaarman.mockito_kotlin.argumentCaptor
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.ListAction
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.list.ListDescriptor
import org.wordpress.android.fluxc.model.list.ListItemModel
import org.wordpress.android.fluxc.model.list.ListManager
import org.wordpress.android.fluxc.model.list.PostListDescriptor.PostListDescriptorForRestSite
import org.wordpress.android.fluxc.store.ListStore.FetchListPayload
import java.util.Collections
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(MockitoJUnitRunner::class)
class ListManagerTest {
    @Mock private lateinit var dispatcher: Dispatcher
    private lateinit var actionCaptor: KArgumentCaptor<Action<FetchListPayload>>

    // Helpers
    private val testSite by lazy {
        val site = SiteModel()
        site.id = 111
        site.siteId = 222
        site
    }
    private val listDescriptor = PostListDescriptorForRestSite(testSite)
    private val numberOfItems = 30
    private val loadMoreOffset = 10
    private val indexThatShouldLoadMore: Int = numberOfItems - loadMoreOffset + 1

    @Before
    fun setup() {
        actionCaptor = argumentCaptor()
    }

    /**
     * Calling refresh on the [ListManager] should dispatch an action to refresh the list if
     * `isFetchingFirstPage` is false and it's the first call of [ListManager.refresh].
     *
     * The second call of [ListManager.refresh] should be ignored.
     */
    @Test
    fun testRefreshTriggersFetch() {
        val fetchList = { _: ListDescriptor, _: Int ->
            assertFalse(true)
        }
        val listManager = setupListManager(
                isFetchingFirstPage = false,
                isLoadingMore = false,
                canLoadMore = true,
                indexToGet = 11, // doesn't matter
                remoteItemId = 222L, // doesn't matter
                remoteItem = PostModel(),
                fetchList = fetchList
        )
        assertTrue(listManager.refresh())
        assertFalse(listManager.refresh())
        verify(dispatcher, times(1)).dispatch(actionCaptor.capture())
        with(actionCaptor.firstValue) {
            assertEquals(this.type, ListAction.FETCH_LIST)
            assertEquals(this.payload.listDescriptor, listDescriptor)
            assertEquals(this.payload.loadMore, false)
            assertEquals(this.payload.fetchList, fetchList)
        }
    }

    /**
     * Calling refresh on the [ListManager] should NOT dispatch an action to refresh the list if
     * `isFetchingFirstPage` is true.
     */
    @Test
    fun testRefreshDoesNotTriggerFetchIfIsFetchingFirstPageIsTrue() {
        val listManager = setupListManager(
                isFetchingFirstPage = true,
                isLoadingMore = false,
                canLoadMore = true,
                indexToGet = 11, // doesn't matter
                remoteItemId = 222L, // doesn't matter
                remoteItem = PostModel()
        )
        assertFalse(listManager.refresh())
        verify(dispatcher, never()).dispatch(actionCaptor.capture())
    }

    /**
     * Tests [ListManager.getItem] triggering `fetchItem` function if the remote item is `null`.
     *
     * Calling [ListManager.getItem] a second time should not trigger the `fetchItem` again.
     */
    @Test
    fun testGetItemTriggersItemFetch() {
        var fetchedFirstTime = false
        var fetchedSecondTime = false
        val indexToGet = 22 // doesn't matter
        val remoteItemId = 333L // doesn't matter
        val fetchItem = { itemId: Long ->
            if (!fetchedFirstTime) {
                fetchedFirstTime = itemId == remoteItemId
            } else {
                fetchedSecondTime = true
            }
        }
        val listManager = setupListManager(
                isFetchingFirstPage = false,
                isLoadingMore = false,
                canLoadMore = true,
                indexToGet = indexToGet,
                remoteItemId = remoteItemId,
                remoteItem = null,
                fetchItem = fetchItem
        )
        listManager.getItem(indexToGet, shouldFetchIfNull = true)
        assertTrue(fetchedFirstTime)
        listManager.getItem(indexToGet, shouldFetchIfNull = true)
        assertFalse(fetchedSecondTime)
    }

    /**
     * Tests [ListManager.getItem] NOT triggering `fetchItem` function if the item is not null.
     */
    @Test
    fun testGetItemDoesNotTriggerFetchIfTheItemIsNotNull() {
        var fetched = false
        val indexToGet = 22 // doesn't matter
        val remoteItemId = 333L // doesn't matter
        val fetchItem = { _: Long ->
            fetched = true
        }
        val listManager = setupListManager(
                isFetchingFirstPage = false,
                isLoadingMore = false,
                canLoadMore = true,
                indexToGet = indexToGet,
                remoteItemId = remoteItemId,
                remoteItem = PostModel(),
                fetchItem = fetchItem
        )
        listManager.getItem(indexToGet, shouldFetchIfNull = true)
        assertFalse(fetched)
    }
    /**
     * Tests [ListManager.getItem] NOT triggering `fetchItem` function if `shouldFetchIfNull` flag is `false`.
     */
    @Test
    fun testGetItemDoesNotTriggerItemFetchIfShouldFetchIfNullIsFalse() {
        var fetched = false
        val indexToGet = 22 // doesn't matter
        val remoteItemId = 333L // doesn't matter
        val fetchItem = { _: Long ->
            fetched = true
        }
        val listManager = setupListManager(
                isFetchingFirstPage = false,
                isLoadingMore = false,
                canLoadMore = true,
                indexToGet = indexToGet,
                remoteItemId = remoteItemId,
                remoteItem = null,
                fetchItem = fetchItem
        )
        listManager.getItem(indexToGet, shouldFetchIfNull = false)
        assertFalse(fetched)
    }

    /**
     * Tests [ListManager.getItem] triggering load more when the requested index is closer to the end of the list
     * than the offset.
     *
     * Calling [ListManager.getItem] a second time should not dispatch a second action.
     */
    @Test
    fun testGetItemTriggersLoadMore() {
        val fetchList = { _: ListDescriptor, _: Int ->
            assertFalse(true)
        }
        val listManager = setupListManager(
                isFetchingFirstPage = false,
                isLoadingMore = false,
                canLoadMore = true,
                indexToGet = indexThatShouldLoadMore,
                remoteItemId = 132L,
                remoteItem = null,
                fetchList = fetchList
        )
        listManager.getItem(indexThatShouldLoadMore, shouldLoadMoreIfNecessary = true)
        listManager.getItem(indexThatShouldLoadMore, shouldLoadMoreIfNecessary = true)
        verify(dispatcher, times(1)).dispatch(actionCaptor.capture())
        with(actionCaptor.firstValue) {
            assertEquals(this.type, ListAction.FETCH_LIST)
            assertEquals(this.payload.listDescriptor, listDescriptor)
            assertEquals(this.payload.loadMore, true)
            assertEquals(this.payload.fetchList, fetchList)
        }
    }

    /**
     * [ListManager.getItem] should NOT dispatch an action to load more items if the requested index is NOT closer
     * to the end of list than the offset, `isLoadingMore` flag is false and `shouldLoadMoreIfNecessary` flag is true.
     */
    @Test
    fun testGetItemDoesNotTriggerLoadMoreDueToIndex() {
        val listManager = setupListManager(
                isFetchingFirstPage = false,
                isLoadingMore = false,
                canLoadMore = true,
                indexToGet = 0,
                remoteItemId = 132L,
                remoteItem = null
        )
        listManager.getItem(0, shouldLoadMoreIfNecessary = true)
        verify(dispatcher, never()).dispatch(actionCaptor.capture())
    }

    /**
     * [ListManager.getItem] should NOT dispatch an action to load more items if the requested index is closer to
     * the end of list than the offset and `isLoadingMore` flag is false, BUT `shouldLoadMoreIfNecessary` flag is false.
     */
    @Test
    fun testGetItemDoesNotTriggerLoadMoreIfShouldLoadMoreIfNecessaryIsFalse() {
        val listManager = setupListManager(
                isFetchingFirstPage = false,
                isLoadingMore = false,
                canLoadMore = true,
                indexToGet = indexThatShouldLoadMore,
                remoteItemId = 132L,
                remoteItem = null
        )
        listManager.getItem(indexThatShouldLoadMore, shouldLoadMoreIfNecessary = false)
        verify(dispatcher, never()).dispatch(actionCaptor.capture())
    }

    /**
     * [ListManager.getItem] should NOT dispatch an action to load more items if the requested index is closer to
     * the end of list than the offset and `shouldLoadMoreIfNecessary` flag is true, BUT the `canLoadMore` flag
     * is false.
     */
    @Test
    fun testGetItemDoesNotTriggerLoadMoreIfCanLoadMoreIsFalse() {
        val listManager = setupListManager(
                isFetchingFirstPage = false,
                isLoadingMore = false,
                canLoadMore = false,
                indexToGet = indexThatShouldLoadMore,
                remoteItemId = 132L,
                remoteItem = null
        )
        listManager.getItem(indexThatShouldLoadMore, shouldLoadMoreIfNecessary = true)
        verify(dispatcher, never()).dispatch(actionCaptor.capture())
    }

    /**
     * Simple test to check the local items are returned in the initial indexes of the [ListManager] when available.
     */
    @Test
    fun testGetLocalItem() {
        val localItems = listOf("localItem1", "localItem2")
        val fetchList = { _: ListDescriptor, _: Int -> }
        val listManager = ListManager(
                dispatcher,
                listDescriptor,
                localItems,
                emptyList(),
                emptyMap(),
                10,
                false,
                false,
                false,
                fetchItem = {},
                fetchList = fetchList
        )
        localItems.forEachIndexed { index, item ->
            assertEquals(listManager.getItem(index), item)
        }
    }

    /**
     * Sets up a ListManager with given parameters.
     *
     * It'll also assert various common properties for [ListManager] before returning it.
     */
    private fun setupListManager(
        isFetchingFirstPage: Boolean,
        isLoadingMore: Boolean,
        canLoadMore: Boolean,
        indexToGet: Int,
        remoteItemId: Long,
        remoteItem: PostModel?,
        fetchItem: ((Long) -> Unit)? = null,
        fetchList: ((ListDescriptor, Int) -> Unit)? = null
    ): ListManager<PostModel> {
        val listItems: List<ListItemModel> = mock()
        val listItemModel = ListItemModel()
        listItemModel.remoteItemId = remoteItemId
        whenever(listItems.size).thenReturn(numberOfItems)
        whenever(listItems[indexToGet]).thenReturn(listItemModel)
        val listData = if (remoteItem != null) mapOf(Pair(remoteItemId, remoteItem)) else Collections.emptyMap()
        val fetchItemFunction = fetchItem ?: {}
        val fetchListFunction = fetchList ?: { _: ListDescriptor, _: Int -> }
        val listManager = ListManager(dispatcher, listDescriptor, null, listItems, listData, loadMoreOffset,
                isFetchingFirstPage, isLoadingMore, canLoadMore, fetchItemFunction, fetchListFunction)
        assertEquals(isFetchingFirstPage, listManager.isFetchingFirstPage)
        assertEquals(isLoadingMore, listManager.isLoadingMore)
        assertEquals(numberOfItems, listManager.size)
        return listManager
    }
}
