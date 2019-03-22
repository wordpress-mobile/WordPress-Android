package org.wordpress.android.fluxc.model.list.datastore

import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId

/**
 * An internal class that plays the middle man between `PagedListFactory` and [ListItemDataStoreInterface] by extracting
 * the common logic.
 *
 * It takes a snapshot of the identifiers for the list when it's created and propagates the calls from the PagedList
 * data source to [ListItemDataStoreInterface] using these identifiers.
 */
class InternalPagedListDataStore<T>(
    remoteItemIds: List<RemoteId>,
    isListFullyFetched: Boolean,
    private val itemDataStore: ListItemDataStoreInterface<T>
) {
    /*
     * PagedList library takes a snapshot of the data. It does the heavy lifting by caching the items provided to it,
     * but it still needs a consistent list of identifiers to work with. In order to do that, we take a snapshot of the
     * current identifiers and work with those until a new instance is created by PagedList.
     */
    private val itemIdentifiers = itemDataStore.getItemIdentifiers(remoteItemIds, isListFullyFetched)

    /**
     * Number of items the list contains.
     *
     * Since [InternalPagedListDataStore] takes a snapshot of the identifiers for the list when it's created, this
     * value will be valid and unchanged during the lifecycle of this instance.
     */
    val totalSize: Int = itemIdentifiers.size

    /**
     * Returns the list of items [T] by propagating the call to [ListItemDataStoreInterface]
     */
    fun getItemsInRange(startPosition: Int, endPosition: Int): List<T> =
            itemDataStore.getItemsAndFetchIfNecessary(getItemIds(startPosition, endPosition))

    /**
     * Helper function that returns the list of [ListItemIdentifier]s from [itemIdentifiers].
     */
    private fun getItemIds(startPosition: Int, endPosition: Int): List<ListItemIdentifier> {
        if (startPosition < 0 || endPosition < 0 || startPosition > endPosition || endPosition >= totalSize) {
            throw IllegalArgumentException("Illegal start or end position")
        }

        return itemIdentifiers.subList(startPosition, endPosition)
    }
}
