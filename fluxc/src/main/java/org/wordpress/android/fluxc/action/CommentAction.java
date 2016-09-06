package org.wordpress.android.fluxc.action;

import org.wordpress.android.fluxc.annotations.Action;
import org.wordpress.android.fluxc.annotations.ActionEnum;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.fluxc.store.CommentStore.FetchCommentsPayload;
import org.wordpress.android.fluxc.store.CommentStore.FetchCommentsResponsePayload;

@ActionEnum
public enum CommentAction implements IAction {
    @Action(payloadType = FetchCommentsPayload.class)
    FETCH_COMMENTS,
    @Action(payloadType = FetchCommentsResponsePayload.class)
    FETCHED_COMMENTS,
}
