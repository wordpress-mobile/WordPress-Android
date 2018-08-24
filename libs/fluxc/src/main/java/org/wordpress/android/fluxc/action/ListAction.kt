package org.wordpress.android.fluxc.action

import org.wordpress.android.fluxc.annotations.Action
import org.wordpress.android.fluxc.annotations.ActionEnum
import org.wordpress.android.fluxc.annotations.action.IAction
import org.wordpress.android.fluxc.store.ListStore.FetchListPayload
import org.wordpress.android.fluxc.store.ListStore.UpdateListPayload

@ActionEnum
enum class ListAction : IAction {
    @Action(payloadType = FetchListPayload::class)
    FETCH_LIST,
    @Action(payloadType = UpdateListPayload::class)
    UPDATE_LIST
}
