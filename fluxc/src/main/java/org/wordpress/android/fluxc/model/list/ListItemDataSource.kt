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
     * Should get the item (most likely from its Store) for the given [ListDescriptor] and [remoteItemId].
     */
    fun getItem(listDescriptor: ListDescriptor, remoteItemId: Long): T?
}
