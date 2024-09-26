package org.wordpress.android.fluxc.action;

import org.wordpress.android.fluxc.annotations.Action;
import org.wordpress.android.fluxc.annotations.ActionEnum;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.store.PostStore.DeletedPostPayload;
import org.wordpress.android.fluxc.store.PostStore.FetchPostLikesPayload;
import org.wordpress.android.fluxc.store.PostStore.FetchPostListPayload;
import org.wordpress.android.fluxc.store.PostStore.FetchPostListResponsePayload;
import org.wordpress.android.fluxc.store.PostStore.FetchPostResponsePayload;
import org.wordpress.android.fluxc.store.PostStore.FetchPostStatusResponsePayload;
import org.wordpress.android.fluxc.store.PostStore.FetchPostsPayload;
import org.wordpress.android.fluxc.store.PostStore.FetchPostsResponsePayload;
import org.wordpress.android.fluxc.store.PostStore.FetchRevisionsPayload;
import org.wordpress.android.fluxc.store.PostStore.FetchRevisionsResponsePayload;
import org.wordpress.android.fluxc.store.PostStore.FetchedPostLikesResponsePayload;
import org.wordpress.android.fluxc.store.PostStore.RemoteAutoSavePostPayload;
import org.wordpress.android.fluxc.store.PostStore.RemotePostPayload;

@ActionEnum
public enum PostAction implements IAction {
    // Remote actions
    @Action(payloadType = FetchPostListPayload.class)
    FETCH_POST_LIST,
    @Action(payloadType = FetchPostsPayload.class)
    FETCH_POSTS,
    @Action(payloadType = FetchPostsPayload.class)
    FETCH_PAGES,
    @Action(payloadType = RemotePostPayload.class)
    FETCH_POST,
    @Action(payloadType = RemotePostPayload.class)
    FETCH_POST_STATUS,
    @Action(payloadType = RemotePostPayload.class)
    PUSH_POST,
    @Action(payloadType = RemotePostPayload.class)
    DELETE_POST,
    @Action(payloadType = RemotePostPayload.class)
    RESTORE_POST,
    @Action(payloadType = FetchRevisionsPayload.class)
    FETCH_REVISIONS,
    @Action(payloadType = RemotePostPayload.class)
    REMOTE_AUTO_SAVE_POST,
    @Action(payloadType = FetchPostLikesPayload.class)
    FETCH_POST_LIKES,

    // Remote responses
    @Action(payloadType = FetchPostListResponsePayload.class)
    FETCHED_POST_LIST,
    @Action(payloadType = FetchPostsResponsePayload.class)
    FETCHED_POSTS,
    @Action(payloadType = FetchPostResponsePayload.class)
    FETCHED_POST,
    @Action(payloadType = FetchPostStatusResponsePayload.class)
    FETCHED_POST_STATUS,
    @Action(payloadType = RemotePostPayload.class)
    PUSHED_POST,
    @Action(payloadType = DeletedPostPayload.class)
    DELETED_POST,
    @Action(payloadType = RemotePostPayload.class)
    RESTORED_POST,
    @Action(payloadType = FetchRevisionsResponsePayload.class)
    FETCHED_REVISIONS,
    @Action(payloadType = RemoteAutoSavePostPayload.class)
    REMOTE_AUTO_SAVED_POST,
    @Action(payloadType = FetchedPostLikesResponsePayload.class)
    FETCHED_POST_LIKES,

    // Local actions
    @Action(payloadType = PostModel.class)
    UPDATE_POST,
    @Action(payloadType = PostModel.class)
    REMOVE_POST,
    @Action
    REMOVE_ALL_POSTS
}

