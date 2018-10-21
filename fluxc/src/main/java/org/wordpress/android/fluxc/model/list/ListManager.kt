package org.wordpress.android.fluxc.model.list

import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.ListActionBuilder
import org.wordpress.android.fluxc.model.list.ListManagerItem.RemoteItem
import org.wordpress.android.fluxc.store.ListStore.FetchListPayload

/**
 * A sealed class to make it easier to manage local and remote items together.
 */
sealed class ListManagerItem<T>(val value: T?) {
    class LocalItem<T>(value: T): ListManagerItem<T>(value)
    class RemoteItem<T>(val remoteItemId: Long, value: T?): ListManagerItem<T>(value)
}

/**
 * This is a helper class by which FluxC communicates a list's data and its state with its clients. It's designed to be
 * short lived as `ListStore.OnListChanged` should trigger the client to ask FluxC for a new instance.
 *
 * @param dispatcher The dispatcher to be used for FluxC actions
 * @param listDescriptor The list descriptor that will be used for FluxC actions and comparison with
 * other [ListManager]s
 * @param items List of items to manage
 * @param loadMoreOffset The offset from the end of list that'll be used to fetch the next page
 * @param isFetchingFirstPage A helper property to be used to show/hide pull-to-refresh progress bar
 * @param isLoadingMore A helper property to be used to show/hide load more progress bar
 * @param canLoadMore Tells the [ListManager] whether there is more data to be fetched
 * @param fetchItem A function which fetches the item for the given `remoteItemId` as [Long].
 * @param fetchList A function which fetches the list for the given [ListDescriptor]. It'll be passed back to
 * `ListStore` through [FetchListPayload] for it to do the eventual fetching.
 *
 * @property size The number of items in the list
 *
 */
class ListManager<T>(
    private val dispatcher: Dispatcher,
    private val listDescriptor: ListDescriptor,
    private val items: List<ListManagerItem<T>>,
    private val loadMoreOffset: Int,
    val isFetchingFirstPage: Boolean,
    val isLoadingMore: Boolean,
    val canLoadMore: Boolean,
    private val fetchItem: (Long) -> Unit,
    private val fetchList: (ListDescriptor, Int) -> Unit
) {
    val size: Int by lazy {
        items.size
    }
    /**
     * This property help us prevent duplicate requests within short time frames. Since [ListManager]
     * instances are meant to be short lived, this will not actually prevent duplicate requests and it's not actually
     * its job to do so. However, since its instances will be used by adapters, it can dispatch a lot of unnecessary
     * actions.
     */
    private val fetchRemoteItemSet = HashSet<Long>()

    /**
     * Returns the item in a given position if it's available.
     *
     * @param position The index of the item
     * @param shouldFetchIfNull Indicates whether the [ListManager] should initiate a fetch if the item is
     * not available.
     * @param shouldLoadMoreIfNecessary Indicates whether the [ListManager] should dispatch an action to load more data
     * if the end of the list is closer than the [loadMoreOffset].
     */
    fun getItem(
        position: Int,
        shouldFetchIfNull: Boolean = true,
        shouldLoadMoreIfNecessary: Boolean = true
    ): T? {
        if (shouldLoadMoreIfNecessary && position > size - loadMoreOffset) {
            loadMore()
        }
        val item = items[position]
        if (item is RemoteItem && item.value == null && shouldFetchIfNull) {
            fetchItemIfNecessary(item.remoteItemId)
        }
        return item.value
    }

    /**
     * Dispatches an action to fetch the first page of the list. Since this class is immutable, it'll not update itself.
     * `OnListChanged` should be used to observe changes to lists and a new instance should be requested from
     * `ListStore`.
     *
     * [isFetchingFirstPage] will be checked before dispatching the action.
     *
     * @return whether the refresh action is dispatched
     */
    fun refresh(): Boolean {
        if (!isFetchingFirstPage) {
            val fetchListPayload = FetchListPayload(listDescriptor, false, fetchList)
            dispatcher.dispatch(ListActionBuilder.newFetchListAction(fetchListPayload))
            return true
        }
        return false
    }

    /**
     * Returns a list containing results of applying the given [predicate] function to each non-null element.
     */
    fun findIndexedNotNull(predicate: (T) -> Boolean): List<Pair<Int, T>> {
        return items.mapIndexedNotNull { index, item ->
            if (item.value != null && predicate(item.value)) Pair(index, item.value) else null
        }
    }

    /**
     * Dispatches an action to load the next page of a list. It's auto-managed by [ListManager].
     *
     * [canLoadMore] will be checked before dispatching the action.
     */
    private fun loadMore() {
        if (canLoadMore) {
            dispatcher.dispatch(ListActionBuilder.newFetchListAction(FetchListPayload(listDescriptor, true, fetchList)))
        }
    }

    /**
     * Calls the [fetchItem] function if the given [remoteItemId] is not in the [fetchRemoteItemSet] and adds the id
     * to the [fetchRemoteItemSet]. This is to prevent fetching the same item more than once.
     */
    private fun fetchItemIfNecessary(remoteItemId: Long) {
        if (!fetchRemoteItemSet.contains(remoteItemId)) {
            fetchItem(remoteItemId)
            fetchRemoteItemSet.add(remoteItemId)
        }
    }
}
