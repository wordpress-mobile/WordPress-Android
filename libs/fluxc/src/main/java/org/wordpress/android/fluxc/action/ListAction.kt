package org.wordpress.android.fluxc.action

import org.wordpress.android.fluxc.annotations.Action
import org.wordpress.android.fluxc.annotations.ActionEnum
import org.wordpress.android.fluxc.annotations.action.IAction
import org.wordpress.android.fluxc.store.ListStore.FetchListPayload
import org.wordpress.android.fluxc.store.ListStore.FetchedListItemsPayload
import org.wordpress.android.fluxc.store.ListStore.ListItemsUpdatedPayload

@ActionEnum
enum class ListAction : IAction {
    @Action(payloadType = FetchListPayload::class)
    FETCH_LIST,
    @Action(payloadType = FetchedListItemsPayload::class)
    FETCHED_LIST_ITEMS,
    @Action(payloadType = ListItemsUpdatedPayload::class)
    LIST_ITEMS_UPDATED
}
