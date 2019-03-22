package org.wordpress.android.fluxc.model.list.datastore

import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.list.ListDescriptor

/**
 * An interface used to tell how to take certain actions to manage a list to `PagedList` where [T] represent the item
 * type that'll be used by `PagedList`.
 */
interface ListItemDataStoreInterface<T> {
    /**
     * Should return a list of items [T] for the given list of [ListItemIdentifier]s that will be provided by
     * [getItemIdentifiers]. It should also fetch the missing items if necessary.
     */
    fun getItemsAndFetchIfNecessary(itemIdentifiers: List<ListItemIdentifier>): List<T>

    /**
     * Should transform a list of remote ids for the list to a list of [ListItemIdentifier]s to be used by
     * [getItemsAndFetchIfNecessary]. This method allows the implementation of this interface to make the modifications
     * to the list as necessary. For example, a list could be transformed to:
     *
     * * Add a header
     * * Add an end list indicator
     * * Hide certain items
     * * Add section headers
     */
    fun getItemIdentifiers(remoteItemIds: List<RemoteId>, isListFullyFetched: Boolean): List<ListItemIdentifier>

    /**
     * Should fetch the list for the given [ListDescriptor] and an offset.
     */
    fun fetchList(listDescriptor: ListDescriptor, offset: Long)
}
