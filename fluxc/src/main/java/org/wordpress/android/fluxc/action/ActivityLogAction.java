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
    @Action(payloadType = ActivityLogStore.DownloadPayload.class)
    DOWNLOAD
}
