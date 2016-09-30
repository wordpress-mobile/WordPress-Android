package org.wordpress.android.fluxc.action;

import org.wordpress.android.fluxc.annotations.Action;
import org.wordpress.android.fluxc.annotations.ActionEnum;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.fluxc.model.CommentModel;
import org.wordpress.android.fluxc.store.CommentStore.FetchCommentsPayload;
import org.wordpress.android.fluxc.store.CommentStore.FetchCommentsResponsePayload;
import org.wordpress.android.fluxc.store.CommentStore.RemoteCommentPayload;
import org.wordpress.android.fluxc.store.CommentStore.RemoteCommentResponsePayload;

@ActionEnum
public enum CommentAction implements IAction {
    @Action(payloadType = FetchCommentsPayload.class)
    FETCH_COMMENTS,
    @Action(payloadType = FetchCommentsResponsePayload.class)
    FETCHED_COMMENTS,
    @Action(payloadType = RemoteCommentPayload.class)
    FETCH_COMMENT,
    @Action(payloadType = RemoteCommentResponsePayload.class)
    FETCHED_COMMENT,
    @Action(payloadType = CommentModel.class)
    UPDATE_COMMENT,
    @Action(payloadType = RemoteCommentPayload.class)
    PUSH_COMMENT,
    @Action(payloadType = RemoteCommentResponsePayload.class)
    PUSHED_COMMENT,
    @Action(payloadType = CommentModel.class)
    REMOVE_COMMENT,
    @Action(payloadType = RemoteCommentPayload.class)
    DELETE_COMMENT,
    @Action(payloadType = RemoteCommentResponsePayload.class)
    DELETED_COMMENT,
}
