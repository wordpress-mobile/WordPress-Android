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
        val listManager = setupListManager(
                isFetchingFirstPage = false,
                isLoadingMore = false,
                canLoadMore = true,
                indexToGet = 11, // doesn't matter
                remoteItemId = 222L, // doesn't matter
                remoteItem = PostModel()
        )
        assertTrue(listManager.refresh())
        assertFalse(listManager.refresh())
        verify(dispatcher, times(1)).dispatch(actionCaptor.capture())
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
     * Tests [ListManager.getRemoteItem] triggering `fetchItem` function if the remote item is `null`.
     *
     * Calling [ListManager.getRemoteItem] a second time should not trigger the `fetchItem` again.
     */
    @Test
    fun testGetRemoteItemTriggersItemFetch() {
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
        listManager.getRemoteItem(indexToGet, shouldFetchIfNull = true)
        assertTrue(fetchedFirstTime)
        listManager.getRemoteItem(indexToGet, shouldFetchIfNull = true)
        assertFalse(fetchedSecondTime)
    }

    /**
     * Tests [ListManager.getRemoteItem] NOT triggering `fetchItem` function if the item is not null.
     */
    @Test
    fun testGetRemoteItemDoesNotTriggerFetchIfTheItemIsNotNull() {
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
        listManager.getRemoteItem(indexToGet, shouldFetchIfNull = true)
        assertFalse(fetched)
    }
    /**
     * Tests [ListManager.getRemoteItem] NOT triggering `fetchItem` function if `shouldFetchIfNull` flag is `false`.
     */
    @Test
    fun testGetRemoteItemDoesNotTriggerItemFetchIfShouldFetchIfNullIsFalse() {
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
        listManager.getRemoteItem(indexToGet, shouldFetchIfNull = false)
        assertFalse(fetched)
    }

    /**
     * Tests [ListManager.getRemoteItem] triggering load more when the requested index is closer to the end of the list
     * than the offset.
     *
     * Calling [ListManager.getRemoteItem] a second time should not dispatch a second action.
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
        listManager.getRemoteItem(indexThatShouldLoadMore, shouldLoadMoreIfNecessary = true)
        verify(dispatcher, times(1)).dispatch(actionCaptor.capture())
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
    fun testGetRemoteItemDoesNotTriggerLoadMoreDueToIndex() {
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
    fun testGetRemoteItemDoesNotTriggerLoadMoreIfShouldLoadMoreIfNecessaryIsFalse() {
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
     * the end of list than the offset and `shouldLoadMoreIfNecessary` flag is true, BUT the `canLoadMore` flag
     * is false.
     */
    @Test
    fun testGetRemoteItemDoesNotTriggerLoadMoreIfCanLoadMoreIsFalse() {
        val listManager = setupListManager(
                isFetchingFirstPage = false,
                isLoadingMore = false,
                canLoadMore = false,
                indexToGet = indexThatShouldLoadMore,
                remoteItemId = 132L,
                remoteItem = null
        )
        listManager.getRemoteItem(indexThatShouldLoadMore, shouldLoadMoreIfNecessary = true)
        verify(dispatcher, never()).dispatch(actionCaptor.capture())
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
        fetchItem: ((Long) -> Unit)? = null
    ): ListManager<PostModel> {
        val listItems: List<ListItemModel> = mock()
        val listItemModel = ListItemModel()
        listItemModel.remoteItemId = remoteItemId
        whenever(listItems.size).thenReturn(numberOfItems)
        whenever(listItems[indexToGet]).thenReturn(listItemModel)
        val listData = if (remoteItem != null) mapOf(Pair(remoteItemId, remoteItem)) else Collections.emptyMap()
        val fetchFunction = fetchItem ?: {}
        val listManager = ListManager(dispatcher, listDescriptor, listItems, listData, loadMoreOffset,
                isFetchingFirstPage, isLoadingMore, canLoadMore, fetchFunction)
        assertEquals(isFetchingFirstPage, listManager.isFetchingFirstPage)
        assertEquals(isLoadingMore, listManager.isLoadingMore)
        assertEquals(numberOfItems, listManager.size)
        return listManager
    }
}
