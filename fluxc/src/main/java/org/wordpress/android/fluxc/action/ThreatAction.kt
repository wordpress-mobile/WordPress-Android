package org.wordpress.android.fluxc.action

import org.wordpress.android.fluxc.annotations.Action
import org.wordpress.android.fluxc.annotations.ActionEnum
import org.wordpress.android.fluxc.annotations.action.IAction
import org.wordpress.android.fluxc.store.ThreatStore

@ActionEnum
enum class ThreatAction : IAction {
    @Action(payloadType = ThreatStore.FetchThreatPayload::class)
    FETCH_THREAT
}
