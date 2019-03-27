package org.wordpress.android.fluxc.model.list.datastore

import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.list.ListDescriptor

/**
 * An internal class that plays the middle man between `PagedListFactory` and [ListItemDataStoreInterface] by extracting
 * the common logic.
 *
 * It takes a snapshot of the identifiers for the list when it's created and propagates the calls from the PagedList
 * data source to [ListItemDataStoreInterface] using these identifiers.
 *
 * [LD] represents the ListDescriptor
 * [ID] represents the identifier for the type [T]
 * [T] represents the end result type that'll be used by `PagedList`.
 */
class InternalPagedListDataStore<LD: ListDescriptor, ID, T>(
    private val listDescriptor: LD,
    remoteItemIds: List<RemoteId>,
    isListFullyFetched: Boolean,
    private val itemDataStore: ListItemDataStoreInterface<LD, ID, T>
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
     * Returns the list of items [T] by propagating the call to [ListItemDataStoreInterface]
     */
    fun getItemsInRange(startPosition: Int, endPosition: Int): List<T> =
            itemDataStore.getItemsAndFetchIfNecessary(listDescriptor, getItemIds(startPosition, endPosition))

    /**
     * Helper function that returns the list of identifiers [ID] for the given start and end positions using the
     * internal [itemIdentifiers].
     */
    private fun getItemIds(startPosition: Int, endPosition: Int): List<ID> {
        if (startPosition < 0 || endPosition < 0 || startPosition > endPosition || endPosition > totalSize) {
            throw IllegalArgumentException(
                    "Illegal start($startPosition) or end($endPosition) position for totalSize($totalSize)"
            )
        }

        return itemIdentifiers.subList(startPosition, endPosition)
    }
}
