package org.wordpress.android.fluxc.action;

import org.wordpress.android.fluxc.annotations.Action;
import org.wordpress.android.fluxc.annotations.ActionEnum;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.fluxc.store.FetchActivitiesPayload;
import org.wordpress.android.fluxc.store.FetchRewindStatePayload;
import org.wordpress.android.fluxc.store.FetchRewindStateResponsePayload;
import org.wordpress.android.fluxc.store.FetchedActivitiesPayload;

@ActionEnum
public enum ActivityAction implements IAction {
    // Remote actions
    @Action(payloadType = FetchActivitiesPayload.class)
    FETCH_ACTIVITIES,
    @Action(payloadType = FetchRewindStatePayload.class)
    FETCH_REWIND_STATE,

    // Remote responses
    @Action(payloadType = FetchedActivitiesPayload.class)
    FETCHED_ACTIVITIES,
    @Action(payloadType = FetchRewindStateResponsePayload.class)
    FETCHED_REWIND_STATE
}
