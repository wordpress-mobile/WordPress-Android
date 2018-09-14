package org.wordpress.android.fluxc.model.list

import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.ListActionBuilder
import org.wordpress.android.fluxc.store.ListStore.FetchListPayload

/**
 * This is an immutable class which helps us expose the list details to a client. It's designed to be initiated from
 * `ListStore`.
 *
 * @param dispatcher The dispatcher to be used for FluxC actions
 * @param listDescriptor The list descriptor that will be used for FluxC actions and comparison with
 * other [ListManager]s
 * @param items The [ListItemModel]s which contains the `remoteItemId`s
 * @param listData The map that holds the items as values for the `remoteItemId`s as keys
 * @param loadMoreOffset Tells how many items before last one should trigger loading more data
 * @param isFetchingFirstPage A helper property to be used to show/hide pull-to-refresh progress bar
 * @param isLoadingMore A helper property to be used to show/hide load more progress bar
 *
 * @property size The number of items in the list
 *
 */
class ListManager<T>(
    private val dispatcher: Dispatcher,
    private val listDescriptor: ListDescriptor,
    private val items: List<ListItemModel>,
    private val listData: Map<Long, T>,
    private val loadMoreOffset: Int,
    val isFetchingFirstPage: Boolean,
    val isLoadingMore: Boolean,
    val canLoadMore: Boolean,
    private val fetchItem: (Long) -> Unit
) {
    val size: Int = items.size

    /**
     * Returns the remote id of the item in the given [position].
     */
    fun getRemoteItemId(position: Int): Long = items[position].remoteItemId

    /**
     * Returns the item in a given position if it's available.
     *
     * @param position The index of the item
     * @param shouldFetchIfNull Indicates whether the [ListManager] should initiate a fetch if the item is
     * not available.
     * @param shouldLoadMoreIfNecessary Indicates whether the [ListManager] should dispatch an action to load more data
     * if the end of the list is closer than the [loadMoreOffset].
     */
    fun getRemoteItem(
        position: Int,
        shouldFetchIfNull: Boolean = true,
        shouldLoadMoreIfNecessary: Boolean = true
    ): T? {
        if (shouldLoadMoreIfNecessary && position > size - loadMoreOffset) {
            loadMore()
        }
        val remoteItemId = getRemoteItemId(position)
        val item = listData[remoteItemId]
        if (item == null && shouldFetchIfNull) {
            fetchItem(remoteItemId)
        }
        return item
    }

    /**
     * Dispatches an action to fetch the first page of the list. Since this class is immutable, it'll not update itself.
     * `OnListChanged` should be used to observe changes to lists and a new instance should be requested from
     * `ListStore`.
     *
     * [isFetchingFirstPage] will be checked before dispatching the action to prevent duplicate requests.
     *
     * @return whether the refresh action is dispatched
     */
    fun refresh() {
        if (!isFetchingFirstPage) {
            dispatcher.dispatch(ListActionBuilder.newFetchListAction(FetchListPayload(listDescriptor)))
        }
    }

    /**
     * Dispatches an action to load the next page of a list. It's auto-managed by [ListManager]. See [getRemoteItem]
     * for more details.
     *
     * [canLoadMore] will be checked before dispatching the action.
     */
    private fun loadMore() {
        if (canLoadMore) {
            dispatcher.dispatch(ListActionBuilder.newFetchListAction(FetchListPayload(listDescriptor, true)))
        }
    }
}
