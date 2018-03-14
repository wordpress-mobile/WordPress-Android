package org.wordpress.android.fluxc.action;

import org.wordpress.android.fluxc.annotations.Action;
import org.wordpress.android.fluxc.annotations.ActionEnum;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.fluxc.store.ActivityStore;

@ActionEnum
public enum ActivityAction implements IAction {
    // Remote actions
    @Action(payloadType = ActivityStore.FetchActivitiesPayload.class)
    FETCH_ACTIVITIES,
    @Action(payloadType = ActivityStore.FetchRewindStatePayload.class)
    FETCH_REWIND_STATE,

    // Remote responses
    @Action(payloadType = ActivityStore.FetchedActivitiesPayload.class)
    FETCHED_ACTIVITIES,
    @Action(payloadType = ActivityStore.FetchRewindStateResponsePayload.class)
    FETCHED_REWIND_STATE
}
