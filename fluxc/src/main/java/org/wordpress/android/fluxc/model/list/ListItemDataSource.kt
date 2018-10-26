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
     * Should fetch the list for the given [ListDescriptor] and an offset.
     */
    fun fetchList(listDescriptor: ListDescriptor, offset: Int)

    /**
     * Should return the items available for the given [ListDescriptor] and [remoteItemIds].
     */
    fun getItems(listDescriptor: ListDescriptor, remoteItemIds: List<Long>): Map<Long, T>

    /**
     * Optional function to return the ordered local items to be shown at the top of the list
     */
    fun localItems(listDescriptor: ListDescriptor): List<T>? = null

    /**
     * Optional function to return list of remote items ids that must be included in the list. If they are not in the
     * list they'll be added in between local items and the remote items.
     *
     * This is used for a workaround where local items become remote items after a network request but the remote ids
     * of those items are not yet added to the list.
     */
    fun remoteItemIdsToInclude(listDescriptor: ListDescriptor): List<Long>? = null

    /**
     * Optional function to return list of remote item ids that must not be included in the list.
     *
     * This is used to be able to temporarily hide items in the list to show the user an undo action.
     */
    fun remoteItemsToHide(listDescriptor: ListDescriptor): List<Long>? = null
}
