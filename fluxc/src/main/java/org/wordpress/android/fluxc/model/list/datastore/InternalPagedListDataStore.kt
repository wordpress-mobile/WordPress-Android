package org.wordpress.android.fluxc.model.list.datastore

import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.list.ListDescriptor

/**
 * An internal class that plays the middle man between `PagedListFactory` and [ListItemDataStoreInterface] by extracting
 * the common logic.
 *
 * It takes a snapshot of the identifiers for the list when it's created and propagates the calls from the PagedList
 * data source to [ListItemDataStoreInterface] using these identifiers.
 */
class InternalPagedListDataStore<LIST_DESCRIPTOR : ListDescriptor, ITEM_IDENTIFIER, LIST_ITEM>(
    private val listDescriptor: LIST_DESCRIPTOR,
    remoteItemIds: List<RemoteId>,
    isListFullyFetched: Boolean,
    private val itemDataStore: ListItemDataStoreInterface<LIST_DESCRIPTOR, ITEM_IDENTIFIER, LIST_ITEM>
) {
    /*
     * PagedList library needs a snapshot of the data. It does the heavy lifting by caching the items provided to it,
     * but it still needs a consistent list of identifiers to work with. In order to do that, we take a snapshot of the
     * current identifiers and work with those until a new instance is created by PagedList.
     */
    private val itemIdentifiers = itemDataStore.getItemIdentifiers(listDescriptor, remoteItemIds, isListFullyFetched)

    /**
     * Number of items the list contains.
     *
     * Since [InternalPagedListDataStore] takes a snapshot of the identifiers for the list when it's created, this
     * value will be valid and unchanged during the lifecycle of this instance.
     */
    val totalSize: Int
        get() = itemIdentifiers.size

    /**
     * Returns the list of items [LIST_ITEM] by propagating the call to [ListItemDataStoreInterface]
     *
     * @param startPosition Start position that's inclusive
     * @param endPosition End position that's exclusive
     */
    fun getItemsInRange(startPosition: Int, endPosition: Int): List<LIST_ITEM> =
            itemDataStore.getItemsAndFetchIfNecessary(listDescriptor, getItemIds(startPosition, endPosition))

    /**
     * Helper function that returns the list [ITEM_IDENTIFIER]s for the given start and end positions using the
     * internal [itemIdentifiers].
     *
     * @param startPosition Start position that's inclusive
     * @param endPosition End position that's exclusive
     */
    private fun getItemIds(startPosition: Int, endPosition: Int): List<ITEM_IDENTIFIER> {
        require(startPosition in 0..endPosition && endPosition <= totalSize) {
            "Illegal start($startPosition) or end($endPosition) position for totalSize($totalSize)"
        }

        return itemIdentifiers.subList(startPosition, endPosition)
    }
}
