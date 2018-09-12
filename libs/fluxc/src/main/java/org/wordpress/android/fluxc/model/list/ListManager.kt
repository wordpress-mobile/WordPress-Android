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
 * @param dataSource The data source which knows actions like how to query an item by `remoteItemId` or how to fetch it.
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
    private val dataSource: ListItemDataSource<T>,
    private val loadMoreOffset: Int,
    val isFetchingFirstPage: Boolean,
    val isLoadingMore: Boolean,
    val canLoadMore: Boolean
) {
    val size: Int = items.size

    /**
     * Returns the item in a given position. It's meant to be used by adapters.
     *
     * The [ListItemDataSource.getItem] function will be utilized to get the item by its `remoteItemId`.
     *
     * @param position The index of the item
     * @param shouldFetchIfNull Indicates whether the [ListManager] should initiate a fetch
     * @param shouldLoadMoreIfNecessary Indicates whether the [ListManager] should dispatch an action to load more data
     * if the end of the list is closer than the [loadMoreOffset].
     * if [ListItemDataSource.getItem] returns `null`
     */
    fun getRemoteItem(
        position: Int,
        shouldFetchIfNull: Boolean = true,
        shouldLoadMoreIfNecessary: Boolean = true
    ): T? {
        if (shouldLoadMoreIfNecessary && position > size - loadMoreOffset) {
            loadMore()
        }
        val listItemModel = items[position]
        val remoteItemId = listItemModel.remoteItemId
        val item = dataSource.getItem(listDescriptor, remoteItemId)
        if (item == null && shouldFetchIfNull) {
            dataSource.fetchItem(listDescriptor, remoteItemId)
        }
        return item
    }

    /**
     * Returns the index of an item for [remoteItemId] if it exists. It's meant to be paired up with
     * `notifyItemChanged(position)` of recycler view (or similar for other list views) when an item [T] is updated
     * by FluxC.
     */
    fun indexOfItem(remoteItemId: Long): Int? {
        val index = items.indexOfFirst { it.remoteItemId == remoteItemId }
        return if (index != -1) index else null
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
     * [isFetchingFirstPage] will be checked before dispatching the action to prevent duplicate requests.
     */
    private fun loadMore() {
        if (canLoadMore) {
            dispatcher.dispatch(ListActionBuilder.newFetchListAction(FetchListPayload(listDescriptor, true)))
        }
    }

    /**
     * Compares the listDescriptors and remoteItemIds with another [ListManager] and returns the result. It's added
     * to be used with `OnListChanged` to decide whether to reload the full list or not.
     *
     * IMPORTANT: It will NOT compare the contents of items.
     *
     * This is likely to be re-worked soon, but for now, it provides a convenient function for what we need.
     */
    fun hasDataChanged(otherListManager: ListManager<T>): Boolean {
        if (listDescriptor != otherListManager.listDescriptor || items.size != otherListManager.items.size)
            return true
        return !items.zip(otherListManager.items).fold(true) { result, pair ->
            result && pair.first.remoteItemId == pair.second.remoteItemId
        }
    }
}
