package org.wordpress.android.fluxc.action

import org.wordpress.android.fluxc.annotations.Action
import org.wordpress.android.fluxc.annotations.ActionEnum
import org.wordpress.android.fluxc.annotations.action.IAction
import org.wordpress.android.fluxc.store.ListStore.UpdateListPayload

@ActionEnum
enum class ListAction : IAction {
    // Remote actions
    @Action(payloadType = UpdateListPayload::class)
    UPDATE_LIST
}
