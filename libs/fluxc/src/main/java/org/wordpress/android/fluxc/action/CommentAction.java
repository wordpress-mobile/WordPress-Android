package org.wordpress.android.fluxc.action;

import org.wordpress.android.fluxc.annotations.Action;
import org.wordpress.android.fluxc.annotations.ActionEnum;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.fluxc.model.CommentModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.CommentStore.FetchCommentLikesPayload;
import org.wordpress.android.fluxc.store.CommentStore.FetchCommentsPayload;
import org.wordpress.android.fluxc.store.CommentStore.FetchCommentsResponsePayload;
import org.wordpress.android.fluxc.store.CommentStore.FetchedCommentLikesResponsePayload;
import org.wordpress.android.fluxc.store.CommentStore.RemoteCommentPayload;
import org.wordpress.android.fluxc.store.CommentStore.RemoteCommentResponsePayload;
import org.wordpress.android.fluxc.store.CommentStore.RemoteCreateCommentPayload;

@ActionEnum
public enum CommentAction implements IAction {
    // Remote actions
    @Action(payloadType = FetchCommentsPayload.class)
    FETCH_COMMENTS,
    @Action(payloadType = RemoteCommentPayload.class)
    FETCH_COMMENT,
    @Action(payloadType = RemoteCreateCommentPayload.class)
    CREATE_NEW_COMMENT,
    @Action(payloadType = RemoteCommentPayload.class)
    PUSH_COMMENT,
    @Action(payloadType = RemoteCommentPayload.class)
    DELETE_COMMENT,
    @Action(payloadType = RemoteCommentPayload.class)
    LIKE_COMMENT,
    @Action(payloadType = FetchCommentLikesPayload.class)
    FETCH_COMMENT_LIKES,

    // Remote responses
    @Action(payloadType = FetchCommentsResponsePayload.class)
    FETCHED_COMMENTS,
    @Action(payloadType = RemoteCommentResponsePayload.class)
    FETCHED_COMMENT,
    @Action(payloadType = RemoteCommentResponsePayload.class)
    CREATED_NEW_COMMENT,
    @Action(payloadType = RemoteCommentResponsePayload.class)
    PUSHED_COMMENT,
    @Action(payloadType = RemoteCommentResponsePayload.class)
    DELETED_COMMENT,
    @Action(payloadType = RemoteCommentResponsePayload.class)
    LIKED_COMMENT,
    @Action(payloadType = FetchedCommentLikesResponsePayload.class)
    FETCHED_COMMENT_LIKES,

    // Local actions
    @Action(payloadType = CommentModel.class)
    UPDATE_COMMENT,
    @Action(payloadType = SiteModel.class)
    REMOVE_COMMENTS,
    @Action(payloadType = CommentModel.class)
    REMOVE_COMMENT,
    @Action
    REMOVE_ALL_COMMENTS,
}
