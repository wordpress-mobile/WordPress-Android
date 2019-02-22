package org.wordpress.android.fluxc.model.list.datastore

import org.wordpress.android.fluxc.model.list.ListDescriptor

/**
 * This is an interface used to tell how to fetch and get items for the specific model used in the list.
 */
interface ListDataStoreInterface<T> {
    /**
     * Should fetch the item for the given [ListDescriptor] and [remoteItemId].
     */
    fun fetchItem(listDescriptor: ListDescriptor, remoteItemId: Long)

    /**
     * Should fetch the list for the given [ListDescriptor] and an offset.
     */
    fun fetchList(listDescriptor: ListDescriptor, offset: Long)

    /**
     * Should return the item for the given [ListDescriptor] and [remoteItemId] or null if it's not available.
     */
    fun getItemByRemoteId(listDescriptor: ListDescriptor, remoteItemId: Long): T?

    /**
     * Optional function to return the ordered local items to be shown at the top of the list
     */
    fun localItems(listDescriptor: ListDescriptor): List<T> = emptyList()

    /**
     * Optional function to return list of pair of local and remote item ids that must not be included in the list.
     *
     * This is used to be able to temporarily hide items in the list to show the user an undo action.
     */
    fun getItemIdsToHide(listDescriptor: ListDescriptor): List<Pair<Int?, Long?>> = emptyList()
}
