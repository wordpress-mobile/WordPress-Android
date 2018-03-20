package org.wordpress.android.fluxc.action;

import org.wordpress.android.fluxc.annotations.Action;
import org.wordpress.android.fluxc.annotations.ActionEnum;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.fluxc.store.ActivityLogStore;

@ActionEnum
public enum ActivityAction implements IAction {
    // Remote actions
    @Action(payloadType = ActivityLogStore.FetchActivitiesPayload.class)
    FETCH_ACTIVITIES,
    @Action(payloadType = ActivityLogStore.FetchRewindStatePayload.class)
    FETCH_REWIND_STATE,

    // Remote responses
    @Action(payloadType = ActivityLogStore.FetchedActivitiesPayload.class)
    FETCHED_ACTIVITIES,
    @Action(payloadType = ActivityLogStore.FetchedRewindStatePayload.class)
    FETCHED_REWIND_STATE
}
