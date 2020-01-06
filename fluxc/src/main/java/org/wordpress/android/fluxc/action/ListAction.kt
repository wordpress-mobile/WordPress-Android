package org.wordpress.android.fluxc.action

import org.wordpress.android.fluxc.annotations.Action
import org.wordpress.android.fluxc.annotations.ActionEnum
import org.wordpress.android.fluxc.annotations.action.IAction
import org.wordpress.android.fluxc.model.list.ListDescriptorTypeIdentifier
import org.wordpress.android.fluxc.store.ListStore.FetchedListItemsPayload
import org.wordpress.android.fluxc.store.ListStore.ListItemsRemovedPayload
import org.wordpress.android.fluxc.store.ListStore.RemoveExpiredListsPayload

@ActionEnum
enum class ListAction : IAction {
    @Action(payloadType = FetchedListItemsPayload::class)
    FETCHED_LIST_ITEMS,
    @Action(payloadType = ListItemsRemovedPayload::class)
    LIST_ITEMS_REMOVED,
    @Action(payloadType = ListDescriptorTypeIdentifier::class)
    LIST_REQUIRES_REFRESH,
    @Action(payloadType = ListDescriptorTypeIdentifier::class)
    LIST_DATA_INVALIDATED,
    @Action(payloadType = RemoveExpiredListsPayload::class)
    REMOVE_EXPIRED_LISTS,
    @Action
    REMOVE_ALL_LISTS
}
