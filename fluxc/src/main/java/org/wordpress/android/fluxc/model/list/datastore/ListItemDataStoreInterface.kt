package org.wordpress.android.fluxc.model.list.datastore

import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.list.ListDescriptor

/**
 * An interface used to tell how to take certain actions to manage a `PagedList`.
 *
 * [LD] represents the ListDescriptor
 * [ID] represents the identifier for the type [T]
 * [T] represents the end result type that'll be used by `PagedList`.
 */
interface ListItemDataStoreInterface<LD : ListDescriptor, ID, T> {
    /**
     * Should return a list of items [T] for the given [ListDescriptor] and the list of identifiers [ID] that
     * will be provided by [getItemIdentifiers]. It should also fetch the missing items if necessary.
     */
    fun getItemsAndFetchIfNecessary(listDescriptor: LD, itemIdentifiers: List<ID>): List<T>

    /**
     * Should transform a list of remote ids for the given [ListDescriptor] to a list of identifiers [ID] to be
     * used by [getItemsAndFetchIfNecessary]. This method allows the implementation of this interface to make the
     * modifications to the list as necessary. For example, a list could be transformed to:
     *
     * * Add a header
     * * Add an end list indicator
     * * Hide certain items
     * * Add section headers
     */
    fun getItemIdentifiers(
        listDescriptor: LD,
        remoteItemIds: List<RemoteId>,
        isListFullyFetched: Boolean
    ): List<ID>

    /**
     * Should fetch the list for the given [ListDescriptor] and an offset.
     */
    fun fetchList(listDescriptor: LD, offset: Long)
}
