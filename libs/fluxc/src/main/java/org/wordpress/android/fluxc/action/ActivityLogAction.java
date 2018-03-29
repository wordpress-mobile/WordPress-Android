package org.wordpress.android.fluxc.action;

import org.wordpress.android.fluxc.annotations.Action;
import org.wordpress.android.fluxc.annotations.ActionEnum;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.fluxc.store.ActivityLogStore;

@ActionEnum
public enum ActivityLogAction implements IAction {
    // Remote actions
    @Action(payloadType = ActivityLogStore.FetchActivityLogPayload.class)
    FETCH_ACTIVITIES,
    @Action(payloadType = ActivityLogStore.FetchRewindStatePayload.class)
    FETCH_REWIND_STATE,
    @Action(payloadType = ActivityLogStore.RewindPayload.class)
    REWIND,

    // Remote responses
    @Action(payloadType = ActivityLogStore.FetchedActivityLogPayload.class)
    FETCHED_ACTIVITIES,
    @Action(payloadType = ActivityLogStore.FetchedRewindStatePayload.class)
    FETCHED_REWIND_STATE,
    @Action(payloadType = ActivityLogStore.RewindResultPayload.class)
    REWIND_RESULT
}
