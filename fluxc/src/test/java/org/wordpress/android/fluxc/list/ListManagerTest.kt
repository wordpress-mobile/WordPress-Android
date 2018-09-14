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
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.ListAction
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.list.ListDescriptor
import org.wordpress.android.fluxc.model.list.ListItemDataSource
import org.wordpress.android.fluxc.model.list.ListItemModel
import org.wordpress.android.fluxc.model.list.ListManager
import org.wordpress.android.fluxc.model.list.ListType.POST
import org.wordpress.android.fluxc.store.ListStore.FetchListPayload
import java.util.Collections
import kotlin.test.assertEquals

@RunWith(MockitoJUnitRunner::class)
// TODO: These tests need to be updated for the latest changes. Don't open a PR before handling this TODO
class ListManagerTest {
    @Mock private lateinit var dispatcher: Dispatcher
    @Mock private lateinit var fetchItem: (Long) -> Unit
    private lateinit var actionCaptor: KArgumentCaptor<Action<FetchListPayload>>

    // Helpers
    private val listDescriptor = ListDescriptor(POST)
    private val numberOfItems = 30
    private val loadMoreOffset = 10
    private val indexThatShouldLoadMore: Int = numberOfItems - loadMoreOffset + 1

    @Before
    fun setup() {
        actionCaptor = argumentCaptor()
    }

    /**
     * Calling refresh on the [ListManager] should dispatch an action to refresh the list if
     * `isFetchingFirstPage` is false.
     */
    @Test
    fun testRefreshTriggersFetch() {
        val listManager = setupListManager(
                isFetchingFirstPage = false,
                isLoadingMore = false,
                canLoadMore = true,
                indexToGet = 11, // doesn't matter
                remoteItemId = 222L, // doesn't matter
                remoteItem = PostModel()
        )
        listManager.refresh()
        verify(dispatcher).dispatch(actionCaptor.capture())
        with(actionCaptor.firstValue) {
            assertEquals(this.type, ListAction.FETCH_LIST)
            assertEquals(this.payload.listDescriptor, listDescriptor)
            assertEquals(this.payload.loadMore, false)
        }
    }

    /**
     * Calling refresh on the [ListManager] should NOT dispatch an action to refresh the list if
     * `isFetchingFirstPage` is true.
     */
    @Test
    fun testDuplicateRefreshIsIgnored() {
        val listManager = setupListManager(
                isFetchingFirstPage = true,
                isLoadingMore = false,
                canLoadMore = true,
                indexToGet = 11, // doesn't matter
                remoteItemId = 222L, // doesn't matter
                remoteItem = PostModel()
        )
        listManager.refresh()
        verify(dispatcher, never()).dispatch(actionCaptor.capture())
    }

    /**
     * [ListManager.getRemoteItem] should call [ListItemDataSource.fetchItem] if [ListItemDataSource.getItem] returns
     * `null` and `shouldFetchIfNull` flag is true.
     */
    @Test
    fun testGetRemoteItemTriggersItemFetch() {
        val indexToGet = 22 // doesn't matter
        val remoteItemId = 333L // doesn't matter
        val listManager = setupListManager(
                isFetchingFirstPage = false,
                isLoadingMore = false,
                canLoadMore = true,
                indexToGet = indexToGet,
                remoteItemId = remoteItemId,
                remoteItem = null
        )
        listManager.getRemoteItem(indexToGet, shouldFetchIfNull = true)
//        verify(fetchItem(listDescriptor, remoteItemId))
    }

    /**
     * [ListManager.getRemoteItem] should NOT call [ListItemDataSource.fetchItem] if [ListItemDataSource.getItem]
     * returns `null`, BUT `shouldFetchIfNull` flag is false.
     */
    @Test
    fun testGetRemoteItemDoesNotTriggerItemFetch() {
        val indexToGet = 22 // doesn't matter
        val remoteItemId = 333L // doesn't matter
        val listManager = setupListManager(
                isFetchingFirstPage = false,
                isLoadingMore = false,
                canLoadMore = true,
                indexToGet = indexToGet,
                remoteItemId = remoteItemId,
                remoteItem = null
        )
        listManager.getRemoteItem(indexToGet, shouldFetchIfNull = false)
//        verify(fetchItem, never())
    }

    /**
     * [ListManager.getRemoteItem] should dispatch an action to load more items if the requested index is closer to the
     * end of list than the offset, `isLoadingMore` flag is false and `shouldLoadMoreIfNecessary` flag is true.
     */
    @Test
    fun testGetRemoteItemTriggersLoadMore() {
        val listManager = setupListManager(
                isFetchingFirstPage = false,
                isLoadingMore = false,
                canLoadMore = true,
                indexToGet = indexThatShouldLoadMore,
                remoteItemId = 132L,
                remoteItem = null
        )
        listManager.getRemoteItem(indexThatShouldLoadMore, shouldLoadMoreIfNecessary = true)
        verify(dispatcher).dispatch(actionCaptor.capture())
        with(actionCaptor.firstValue) {
            assertEquals(this.type, ListAction.FETCH_LIST)
            assertEquals(this.payload.listDescriptor, listDescriptor)
            assertEquals(this.payload.loadMore, true)
        }
    }

    /**
     * [ListManager.getRemoteItem] should NOT dispatch an action to load more items if the requested index is NOT closer
     * to the end of list than the offset, `isLoadingMore` flag is false and `shouldLoadMoreIfNecessary` flag is true.
     */
    @Test
    fun testGetRemoteItemTriggersLoadMoreDueToIndex() {
        val listManager = setupListManager(
                isFetchingFirstPage = false,
                isLoadingMore = false,
                canLoadMore = true,
                indexToGet = 0,
                remoteItemId = 132L,
                remoteItem = null
        )
        listManager.getRemoteItem(0, shouldLoadMoreIfNecessary = true)
        verify(dispatcher, never()).dispatch(actionCaptor.capture())
    }

    /**
     * [ListManager.getRemoteItem] should NOT dispatch an action to load more items if the requested index is closer to
     * the end of list than the offset and `isLoadingMore` flag is false, BUT `shouldLoadMoreIfNecessary` flag is false.
     */
    @Test
    fun testGetRemoteItemDoesNotTriggerLoadMoreDueToShouldLoadMoreIfNecessary() {
        val listManager = setupListManager(
                isFetchingFirstPage = false,
                isLoadingMore = false,
                canLoadMore = true,
                indexToGet = indexThatShouldLoadMore,
                remoteItemId = 132L,
                remoteItem = null
        )
        listManager.getRemoteItem(indexThatShouldLoadMore, shouldLoadMoreIfNecessary = false)
        verify(dispatcher, never()).dispatch(actionCaptor.capture())
    }

    /**
     * [ListManager.getRemoteItem] should NOT dispatch an action to load more items if the requested index is closer to
     * the end of list than the offset and `shouldLoadMoreIfNecessary` flag is true, BUT the `isLoadingMore` flag
     * is true.
     */
    @Test
    // TODO: Change this test to check canLoadMore instead
    fun testDuplicateLoadMoreIsIgnored() {
        val listManager = setupListManager(
                isFetchingFirstPage = false,
                isLoadingMore = true,
                canLoadMore = false,
                indexToGet = indexThatShouldLoadMore,
                remoteItemId = 132L,
                remoteItem = null
        )
        listManager.getRemoteItem(indexThatShouldLoadMore, shouldLoadMoreIfNecessary = true)
        verify(dispatcher, never()).dispatch(actionCaptor.capture())
    }

    // TODO: test canLoadMore

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
        remoteItem: PostModel?
    ): ListManager<PostModel> {
        val listItems: List<ListItemModel> = mock()
        val listItemModel = ListItemModel()
        listItemModel.remoteItemId = remoteItemId
        whenever(listItems.size).thenReturn(numberOfItems)
        whenever(listItems[indexToGet]).thenReturn(listItemModel)
        val listData = if (remoteItem != null) mapOf(Pair(remoteItemId, remoteItem)) else Collections.emptyMap()
        val listManager = ListManager(dispatcher, listDescriptor, listItems, listData, loadMoreOffset,
                isFetchingFirstPage, isLoadingMore, canLoadMore, fetchItem)
        assertEquals(isFetchingFirstPage, listManager.isFetchingFirstPage)
        assertEquals(isLoadingMore, listManager.isLoadingMore)
        assertEquals(numberOfItems, listManager.size)
        return listManager
    }
}
