package org.wordpress.android.fluxc.model.list.datasource

import androidx.paging.PositionalDataSource
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.list.ListDescriptor

/**
 * This component plays the middle man between the [PositionalDataSource] and [ListItemDataSourceInterface]
 * implementation. Whenever a list is invalidated, meaning it needs to be refreshed, a new instance will be created
 * just like [PositionalDataSource].
 *
 * We first ask the [ListItemDataSourceInterface] for the identifiers of each row and cache them in memory as soon as
 * a new instance is created. This is necessary because [PositionalDataSource] works with immutable values and does the
 * heavy lifting by caching the items in memory as they are loaded, however it still needs a consistent list of
 * identifiers for each index to represent.
 *
 * After the identifiers are cached, whenever [PositionalDataSource] asks for a range of items, they'll be converted
 * to identifiers and propagated to [ListItemDataSourceInterface].
 *
 * Most importantly, by separating this component, we are able to keep a single instance of
 * [ListItemDataSourceInterface] and hide the requirement for identifiers needing to be cached from it.
 */
class InternalPagedListDataSource<LIST_DESCRIPTOR : ListDescriptor, ITEM_IDENTIFIER, LIST_ITEM>(
    private val listDescriptor: LIST_DESCRIPTOR,
    remoteItemIds: List<RemoteId>,
    isListFullyFetched: Boolean,
    private val itemDataSource: ListItemDataSourceInterface<LIST_DESCRIPTOR, ITEM_IDENTIFIER, LIST_ITEM>
) {
    /*
     * PagedList library needs a snapshot of the data. It does the heavy lifting by caching the items provided to it,
     * but it still needs a consistent list of identifiers to work with. In order to do that, we take a snapshot of the
     * current identifiers and work with those until a new instance is created by PagedList.
     */
    private val itemIdentifiers = itemDataSource.getItemIdentifiers(listDescriptor, remoteItemIds, isListFullyFetched)

    /**
     * Number of items the list contains.
     *
     * Since [InternalPagedListDataSource] takes a snapshot of the identifiers for the list when it's created, this
     * value will be valid and unchanged during the lifecycle of this instance.
     */
    val totalSize: Int
        get() = itemIdentifiers.size

    /**
     * Returns the list of items [LIST_ITEM] by propagating the call to [ListItemDataSourceInterface]
     *
     * @param startPosition Start position that's inclusive
     * @param endPosition End position that's exclusive
     */
    fun getItemsInRange(startPosition: Int, endPosition: Int): List<LIST_ITEM> =
            itemDataSource.getItemsAndFetchIfNecessary(listDescriptor, getItemIds(startPosition, endPosition))

    /**
     * Helper function that returns the list [ITEM_IDENTIFIER]s for the given start and end positions using the
     * internal [itemIdentifiers].
     *
     * @param startPosition Start position that's inclusive
     * @param endPosition End position that's exclusive
     */
    private fun getItemIds(startPosition: Int, endPosition: Int): List<ITEM_IDENTIFIER> {
        require(startPosition in 0 until endPosition && endPosition <= totalSize) {
            "Illegal start($startPosition) or end($endPosition) position for totalSize($totalSize)"
        }

        return itemIdentifiers.subList(startPosition, endPosition)
    }
}
