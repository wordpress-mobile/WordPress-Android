package org.wordpress.android.fluxc.model.list

/**
 * This is an interface used to tell how to fetch and get items for the specific model used in the list.
 */
interface ListItemDataSource<T> {
    /**
     * Should fetch the item for the given [ListDescriptor] and [remoteItemId].
     */
    fun fetchItem(listDescriptor: ListDescriptor, remoteItemId: Long)

    /**
     * Should fetch the list for the given [ListDescriptor], whether to load more data and an offset provided by
     * `ListStore`.
     */
    fun fetchList(listDescriptor: ListDescriptor, loadMore: Boolean, offset: Int)

    /**
     * Should return the items available for the given [ListDescriptor] and [remoteItemIds].
     */
    fun getItems(listDescriptor: ListDescriptor, remoteItemIds: List<Long>): Map<Long, T>
}
