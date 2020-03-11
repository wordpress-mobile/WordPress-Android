package org.wordpress.android.fluxc.action

import org.wordpress.android.fluxc.annotations.Action
import org.wordpress.android.fluxc.annotations.ActionEnum
import org.wordpress.android.fluxc.annotations.action.IAction
import org.wordpress.android.fluxc.store.VerticalStore.FetchSegmentPromptPayload

@ActionEnum
enum class VerticalAction : IAction {
    @Action
    FETCH_SEGMENTS,
    @Action(payloadType = FetchSegmentPromptPayload::class)
    FETCH_SEGMENT_PROMPT
}
