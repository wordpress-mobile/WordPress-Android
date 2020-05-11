package org.wordpress.android.fluxc.action

import org.wordpress.android.fluxc.annotations.Action
import org.wordpress.android.fluxc.annotations.ActionEnum
import org.wordpress.android.fluxc.annotations.action.IAction
import org.wordpress.android.fluxc.store.WhatsNewStore.WhatsNewFetchPayload

@ActionEnum
enum class WhatsNewAction : IAction {
    // Remote actions
    @Action(payloadType = WhatsNewFetchPayload::class)
    FETCH_WHATS_NEW
}
