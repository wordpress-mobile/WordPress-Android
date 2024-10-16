package org.wordpress.android.fluxc.model.list.datasource

import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.list.ListDescriptor

/**
 * An interface used to tell how to take certain actions to manage a `PagedList`.
 */
interface ListItemDataSourceInterface<LIST_DESCRIPTOR : ListDescriptor, ITEM_IDENTIFIER, LIST_ITEM> {
    /**
     * Should return a list [LIST_ITEM]s for the given [LIST_DESCRIPTOR] and the list [ITEM_IDENTIFIER]s that will be
     * provided by [getItemIdentifiers].
     *
     * It should also fetch the missing items if necessary.
     */
    fun getItemsAndFetchIfNecessary(
        listDescriptor: LIST_DESCRIPTOR,
        itemIdentifiers: List<ITEM_IDENTIFIER>
    ): List<LIST_ITEM>

    /**
     * Should transform a list of remote ids for the given [LIST_DESCRIPTOR] to a list [ITEM_IDENTIFIER]s to be used by
     * [getItemsAndFetchIfNecessary]. This method allows the implementation of this interface to make the modifications
     * to the list as necessary. For example, a list could be transformed to:
     *
     * * Add a header
     * * Add an end list indicator
     * * Hide certain items
     * * Add section headers
     */
    fun getItemIdentifiers(
        listDescriptor: LIST_DESCRIPTOR,
        remoteItemIds: List<RemoteId>,
        isListFullyFetched: Boolean
    ): List<ITEM_IDENTIFIER>

    /**
     * Should fetch the list for the given [LIST_DESCRIPTOR] and an offset.
     */
    fun fetchList(listDescriptor: LIST_DESCRIPTOR, offset: Long)
}
