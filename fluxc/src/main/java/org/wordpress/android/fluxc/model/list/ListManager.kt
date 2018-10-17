package org.wordpress.android.fluxc.model.list

import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.ListActionBuilder
import org.wordpress.android.fluxc.store.ListStore.FetchListPayload

/**
 * This is a helper class by which FluxC communicates a list's data and its state with its clients. It's designed to be
 * short lived as `ListStore.OnListChanged` should trigger the client to ask FluxC for a new instance.
 *
 * @param dispatcher The dispatcher to be used for FluxC actions
 * @param listDescriptor The list descriptor that will be used for FluxC actions and comparison with
 * other [ListManager]s
 * @param localItems The list of ordered local items that should be shown at the top of the list
 * @param items The [ListItemModel]s which contains the `remoteItemId`s
 * @param listData The map that holds the items as values for the `remoteItemId`s as keys
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
    private val localItems: List<T>?,
    private val items: List<ListItemModel>,
    private val listData: Map<Long, T>,
    private val loadMoreOffset: Int,
    val isFetchingFirstPage: Boolean,
    val isLoadingMore: Boolean,
    val canLoadMore: Boolean,
    private val fetchItem: (Long) -> Unit,
    private val fetchList: (ListDescriptor, Int) -> Unit
) {
    companion object {
        /**
         * A helper function to calculate whether two items from two different [ListManager]s are the same. Since
         * [ListManager] supports local items, we can't always rely on the `remoteItemId` to compare two different
         * items. This function tries to compare `remoteItemId`s of the items if both of them are remote items. If at
         * least one of them is a local item, it'll use the [areItemsTheSame] to defer the comparison to the caller.
         *
         * Here is the full set of rules:
         *
         * 1. Check if both items are remote items, if so compare their `remoteItemId`s and return the result.
         * 2. Get the items from the [listData] and check if both of them are null. If so, for our intents these are
         * the same items, since no other data about them are exposed.
         * 3. Check if either item is null in which case it means that the items are different since we know both of
         * them can't be null due to step 2.
         * 4. Defer the comparison of the items to the caller. The caller should most likely use the local ids.
         *
         * @param new The new [ListManager]
         * @param old The old [ListManager]
         * @param newPosition The position of the item in the new [ListManager]
         * @param oldPosition The position of the item in the old [ListManager]
         * @param areItemsTheSame A function to compare the two items which should not compare the contents of them.
         *
         * @return whether two items are the same regardless of their contents.
         */
        fun <T> areItemsTheSame(
            new: ListManager<T>,
            old: ListManager<T>,
            newPosition: Int,
            oldPosition: Int,
            areItemsTheSame: (T, T) -> Boolean
        ): Boolean {
            if (newPosition >= new.localItemSize && oldPosition >= old.localItemSize) {
                return new.remoteItemId(newPosition - new.localItemSize) ==
                        old.remoteItemId(oldPosition - old.localItemSize)
            }
            val oldItem = old.getItem(oldPosition, false)
            val newItem = new.getItem(newPosition, false)
            if (oldItem == null && newItem == null) {
                return true
            }
            if (oldItem == null || newItem == null) {
                return false
            }
            return areItemsTheSame(oldItem, newItem)
        }
    }

    private val localItemSize: Int = localItems?.size ?: 0
    val size: Int = items.size + localItemSize
    /**
     * These three private properties help us prevent duplicate requests within short time frames. Since [ListManager]
     * instances are meant to be short lived, this will not actually prevent duplicate requests and it's not actually
     * its job to do so. However, since its instances will be used by adapters, it can dispatch a lot of unnecessary
     * actions.
     */
    private var dispatchedRefreshAction = false
    private var dispatchedLoadMoreAction = false
    private val fetchRemoteItemSet = HashSet<Long>()

    private fun remoteItemId(remoteItemIndex: Int): Long {
        return items[remoteItemIndex].remoteItemId
    }

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
        localItems?.let {
            if (position < it.size) {
                return it[position]
            }
        }
        val remoteItemIndex = position - localItemSize
        val remoteItemId = items[remoteItemIndex].remoteItemId
        val item = listData[remoteItemId]
        if (item == null && shouldFetchIfNull) {
            fetchItemIfNecessary(remoteItemId)
        }
        return item
    }

    /**
     * Dispatches an action to fetch the first page of the list. Since this class is immutable, it'll not update itself.
     * `OnListChanged` should be used to observe changes to lists and a new instance should be requested from
     * `ListStore`.
     *
     * [isFetchingFirstPage] & [dispatchedRefreshAction] will be checked before dispatching the action to prevent
     * duplicate requests.
     *
     * @return whether the refresh action is dispatched
     */
    fun refresh(): Boolean {
        if (!isFetchingFirstPage && !dispatchedRefreshAction) {
            val fetchListPayload = FetchListPayload(listDescriptor, false, fetchList)
            dispatcher.dispatch(ListActionBuilder.newFetchListAction(fetchListPayload))
            dispatchedRefreshAction = true
            return true
        }
        return false
    }

    fun hasRemoteItem(remoteItemId: Long): Boolean {
        return items.any { it.remoteItemId == remoteItemId }
    }

    fun findIndices(predicate: (T) -> Boolean): List<Int> {
        val localIndices = localItems?.mapIndexedNotNull { index, localItem ->
            if (predicate(localItem)) index else null
        } ?: emptyList()
        val remoteIndices = items.asSequence().mapIndexedNotNull { index, listItem ->
            val item = listData[listItem.remoteItemId] ?: return@mapIndexedNotNull null
            if (predicate(item)) index else null
        }.map { it + localItemSize }.toList()
        return localIndices.plus(remoteIndices)
    }

    /**
     * Dispatches an action to load the next page of a list. It's auto-managed by [ListManager].
     *
     * [canLoadMore] & [dispatchedLoadMoreAction] will be checked before dispatching the action.
     */
    private fun loadMore() {
        if (canLoadMore && !dispatchedLoadMoreAction) {
            dispatcher.dispatch(ListActionBuilder.newFetchListAction(FetchListPayload(listDescriptor, true, fetchList)))
            dispatchedLoadMoreAction = true
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
