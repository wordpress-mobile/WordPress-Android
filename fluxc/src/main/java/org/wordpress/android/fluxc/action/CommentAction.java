package org.wordpress.android.fluxc.action;

import org.wordpress.android.fluxc.annotations.Action;
import org.wordpress.android.fluxc.annotations.ActionEnum;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.fluxc.model.CommentModel;
import org.wordpress.android.fluxc.store.CommentStore.FetchCommentPayload;
import org.wordpress.android.fluxc.store.CommentStore.FetchCommentResponsePayload;
import org.wordpress.android.fluxc.store.CommentStore.FetchCommentsPayload;
import org.wordpress.android.fluxc.store.CommentStore.FetchCommentsResponsePayload;
import org.wordpress.android.fluxc.store.CommentStore.PushCommentPayload;
import org.wordpress.android.fluxc.store.CommentStore.PushCommentResponsePayload;

@ActionEnum
public enum CommentAction implements IAction {
    @Action(payloadType = FetchCommentsPayload.class)
    FETCH_COMMENTS,
    @Action(payloadType = FetchCommentsResponsePayload.class)
    FETCHED_COMMENTS,
    @Action(payloadType = FetchCommentPayload.class)
    FETCH_COMMENT,
    @Action(payloadType = FetchCommentResponsePayload.class)
    FETCHED_COMMENT,
    @Action(payloadType = PushCommentPayload.class)
    PUSH_COMMENT,
    @Action(payloadType = PushCommentResponsePayload.class)
    PUSHED_COMMENT,
    @Action(payloadType = CommentModel.class)
    DELETE_COMMENT,
    @Action(payloadType = CommentModel.class)
    DELETED_COMMENT,
}
