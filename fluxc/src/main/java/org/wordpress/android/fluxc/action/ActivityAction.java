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

    // Remote responses
    @Action(payloadType = ActivityStore.FetchActivitiesResponsePayload.class)
    FETCHED_ACTIVITIES
}
