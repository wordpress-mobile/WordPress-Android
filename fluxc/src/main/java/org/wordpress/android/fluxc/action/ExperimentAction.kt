package org.wordpress.android.fluxc.action

import org.wordpress.android.fluxc.annotations.Action
import org.wordpress.android.fluxc.annotations.ActionEnum
import org.wordpress.android.fluxc.annotations.action.IAction
import org.wordpress.android.fluxc.store.ExperimentStore.FetchAssignmentsPayload

@ActionEnum
enum class ExperimentAction : IAction {
    @Action(payloadType = FetchAssignmentsPayload::class)
    FETCH_ASSIGNMENTS,
}
