package org.wordpress.android.fluxc.action

import org.wordpress.android.fluxc.annotations.Action
import org.wordpress.android.fluxc.annotations.ActionEnum
import org.wordpress.android.fluxc.annotations.action.IAction
import org.wordpress.android.fluxc.model.CommentModel
import org.wordpress.android.fluxc.store.CommentStore.FetchCommentsPayload
import org.wordpress.android.fluxc.store.CommentStore.FetchCommentsResponsePayload
import org.wordpress.android.fluxc.store.CommentStore.RemoteCommentPayload
import org.wordpress.android.fluxc.store.CommentStore.RemoteCommentResponsePayload
import org.wordpress.android.fluxc.store.CommentStore.RemoteCreateCommentPayload

@Deprecated(
        "This is a temporary code for backward compatibility and will be replaced with " +
                "Comments Unification project."
)
@ActionEnum
enum class CommentsAction : IAction {
    // Remote actions
    @Action(payloadType = FetchCommentsPayload::class)
    FETCH_COMMENTS,

    @Action(payloadType = RemoteCommentPayload::class)
    FETCH_COMMENT,

    @Action(payloadType = RemoteCreateCommentPayload::class)
    CREATE_NEW_COMMENT,

    @Action(payloadType = RemoteCommentPayload::class)
    PUSH_COMMENT,

    @Action(payloadType = RemoteCommentPayload::class)
    DELETE_COMMENT,

    @Action(payloadType = RemoteCommentPayload::class)
    LIKE_COMMENT,

    // Remote responses
    @Action(payloadType = FetchCommentsResponsePayload::class)
    FETCHED_COMMENTS,

    @Action(payloadType = RemoteCommentResponsePayload::class)
    FETCHED_COMMENT,

    @Action(payloadType = RemoteCommentResponsePayload::class)
    CREATED_NEW_COMMENT,

    @Action(payloadType = RemoteCommentResponsePayload::class)
    PUSHED_COMMENT,

    @Action(payloadType = RemoteCommentResponsePayload::class)
    DELETED_COMMENT,

    @Action(payloadType = RemoteCommentResponsePayload::class)
    LIKED_COMMENT,

    // Local actions
    @Action(payloadType = CommentModel::class)
    UPDATE_COMMENT
}
