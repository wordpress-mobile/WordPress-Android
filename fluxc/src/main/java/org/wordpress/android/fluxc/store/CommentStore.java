package org.wordpress.android.fluxc.store;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.yarolegovich.wellsql.SelectQuery;
import com.yarolegovich.wellsql.SelectQuery.Order;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.Payload;
import org.wordpress.android.fluxc.action.CommentAction;
import org.wordpress.android.fluxc.annotations.action.Action;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.fluxc.model.CommentModel;
import org.wordpress.android.fluxc.model.CommentStatus;
import org.wordpress.android.fluxc.model.LikeModel;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError;
import org.wordpress.android.fluxc.network.rest.wpcom.comment.CommentRestClient;
import org.wordpress.android.fluxc.network.xmlrpc.comment.CommentXMLRPCClient;
import org.wordpress.android.fluxc.persistence.CommentSqlUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class CommentStore extends Store {
    private final CommentRestClient mCommentRestClient;
    private final CommentXMLRPCClient mCommentXMLRPCClient;

    // Payloads

    public static class FetchCommentsPayload extends Payload<BaseNetworkError> {
        @NonNull public final SiteModel site;
        @NonNull public final CommentStatus status;
        public final int number;
        public final int offset;

        public FetchCommentsPayload(@NonNull SiteModel site, int number, int offset) {
            this.site = site;
            this.status = CommentStatus.ALL;
            this.number = number;
            this.offset = offset;
        }

        @SuppressWarnings("unused")
        public FetchCommentsPayload(@NonNull SiteModel site, @NonNull CommentStatus status, int number, int offset) {
            this.site = site;
            this.status = status;
            this.number = number;
            this.offset = offset;
        }
    }

    public static class RemoteCommentPayload extends Payload<BaseNetworkError> {
        @NonNull public final SiteModel site;
        @Nullable public final CommentModel comment;
        public final long remoteCommentId;

        public RemoteCommentPayload(@NonNull SiteModel site, @NonNull CommentModel comment) {
            this.site = site;
            this.comment = comment;
            this.remoteCommentId = 0;
        }

        public RemoteCommentPayload(@NonNull SiteModel site, long remoteCommentId) {
            this.site = site;
            this.comment = null;
            this.remoteCommentId = remoteCommentId;
        }
    }

    public static class RemoteLikeCommentPayload extends RemoteCommentPayload {
        public final boolean like;
        public RemoteLikeCommentPayload(@NonNull SiteModel site, @NonNull CommentModel comment, boolean like) {
            super(site, comment);
            this.like = like;
        }

        @SuppressWarnings("unused")
        public RemoteLikeCommentPayload(@NonNull SiteModel site, long remoteCommentId, boolean like) {
            super(site, remoteCommentId);
            this.like = like;
        }
    }

    public static class FetchCommentsResponsePayload extends Payload<CommentError> {
        @NonNull public final List<CommentModel> comments;
        @NonNull public final SiteModel site;
        public final int number;
        public final int offset;
        @Nullable public final CommentStatus requestedStatus;

        public FetchCommentsResponsePayload(@NonNull List<CommentModel> comments, @NonNull SiteModel site, int number,
                                            int offset, @Nullable CommentStatus status) {
            this.comments = comments;
            this.site = site;
            this.number = number;
            this.offset = offset;
            this.requestedStatus = status;
        }
    }

    public static class RemoteCommentResponsePayload extends Payload<CommentError> {
        @Nullable public final CommentModel comment;
        public RemoteCommentResponsePayload(@Nullable CommentModel comment) {
            this.comment = comment;
        }
    }

    public static class RemoteCreateCommentPayload extends Payload<CommentError> {
        @NonNull public final SiteModel site;
        @NonNull public final CommentModel comment;
        @Nullable public final CommentModel reply;
        @Nullable public final PostModel post;

        // Create a new comment on a specific Post
        public RemoteCreateCommentPayload(@NonNull SiteModel site, @NonNull PostModel post,
                                          @NonNull CommentModel comment) {
            this.site = site;
            this.post = post;
            this.comment = comment;
            this.reply = null;
        }

        // Create a new reply to a specific Comment
        public RemoteCreateCommentPayload(@NonNull SiteModel site, @NonNull CommentModel comment,
                                          @NonNull CommentModel reply) {
            this.site = site;
            this.comment = comment;
            this.reply = reply;
            this.post = null;
        }
    }

    public static class FetchCommentLikesPayload extends Payload<BaseNetworkError> {
        public final long siteId;
        public final long remoteCommentId;
        public final boolean requestNextPage;
        public final int pageLength;

        public FetchCommentLikesPayload(long siteId, long remoteCommentId, boolean requestNextPage, int pageLength) {
            this.siteId = siteId;
            this.remoteCommentId = remoteCommentId;
            this.requestNextPage = requestNextPage;
            this.pageLength = pageLength;
        }
    }

    public static class FetchedCommentLikesResponsePayload extends Payload<CommentError> {
        @NonNull public final List<LikeModel> likes;
        public final long siteId;
        public final long commentRemoteId;
        public final boolean hasMore;
        public final boolean isRequestNextPage;

        public FetchedCommentLikesResponsePayload(
                @NonNull List<LikeModel> likes,
                long siteId,
                long commentRemoteId,
                boolean isRequestNextPage,
                boolean hasMore
        ) {
            this.likes = likes;
            this.siteId = siteId;
            this.commentRemoteId = commentRemoteId;
            this.hasMore = hasMore;
            this.isRequestNextPage = isRequestNextPage;
        }
    }

    // Errors

    public enum CommentErrorType {
        GENERIC_ERROR,
        AUTHORIZATION_REQUIRED,
        INVALID_RESPONSE,
        INVALID_INPUT,
        UNKNOWN_COMMENT,
        UNKNOWN_POST,
        DUPLICATE_COMMENT
    }

    public static class CommentError implements OnChangedError {
        @NonNull public CommentErrorType type;
        @NonNull public String message;
        public CommentError(@NonNull CommentErrorType type, @NonNull String message) {
            this.type = type;
            this.message = message;
        }
    }

    // Actions

    public static class OnCommentChanged extends OnChanged<CommentError> {
        public int rowsAffected;
        public int offset;
        @NonNull public CommentAction causeOfChange;
        @Nullable public CommentStatus requestedStatus;
        @NonNull public List<Integer> changedCommentsLocalIds = new ArrayList<>();
        public OnCommentChanged(int rowsAffected, @NonNull CommentAction causeOfChange) {
            this.rowsAffected = rowsAffected;
            this.causeOfChange = causeOfChange;
        }
    }

    public static class OnCommentLikesChanged extends OnChanged<CommentError> {
        @NonNull public CommentAction causeOfChange;
        public final long siteId;
        public final long commentId;
        @NonNull public List<LikeModel> commentLikes = new ArrayList<>();
        public final boolean hasMore;

        public OnCommentLikesChanged(
                long siteId,
                long commentId,
                boolean hasMore,
                @NonNull CommentAction causeOfChange
        ) {
            this.siteId = siteId;
            this.commentId = commentId;
            this.hasMore = hasMore;
            this.causeOfChange = causeOfChange;
        }
    }

    // Constructor

    @Inject public CommentStore(Dispatcher dispatcher, CommentRestClient commentRestClient,
                        CommentXMLRPCClient commentXMLRPCClient) {
        super(dispatcher);
        mCommentRestClient = commentRestClient;
        mCommentXMLRPCClient = commentXMLRPCClient;
    }

    // Getters

    /**
     * Get a list of comment for a specific site.
     *
     * @param site Site model to get comment for.
     * @param orderByDateAscending If true order the results by ascending published date.
     *                             If false, order the results by descending published date.
     * @param statuses Array of status or CommentStatus.ALL to get all of them.
     * @param limit Maximum number of comments to return. 0 is unlimited.
     */
    @NonNull
    @SuppressLint("WrongConstant")
    public List<CommentModel> getCommentsForSite(
            @NonNull SiteModel site,
            boolean orderByDateAscending,
            int limit,
            @NonNull CommentStatus... statuses) {
        @Order int order = orderByDateAscending ? SelectQuery.ORDER_ASCENDING : SelectQuery.ORDER_DESCENDING;
        return CommentSqlUtils.getCommentsForSite(site, order, limit, statuses);
    }

    public int getNumberOfCommentsForSite(
            @NonNull SiteModel site,
            @NonNull CommentStatus... statuses) {
        return CommentSqlUtils.getCommentsCountForSite(site, statuses);
    }

    @Nullable
    @SuppressWarnings("UnusedReturnValue")
    public CommentModel getCommentBySiteAndRemoteId(@NonNull SiteModel site, long remoteCommentId) {
        return CommentSqlUtils.getCommentBySiteAndRemoteId(site, remoteCommentId);
    }

    @Nullable
    public CommentModel getCommentByLocalId(int localId) {
        return CommentSqlUtils.getCommentByLocalCommentId(localId);
    }

    // Store Methods

    @Override
    @Subscribe(threadMode = ThreadMode.ASYNC)
    @SuppressWarnings("rawtypes")
    public void onAction(Action action) {
        IAction actionType = action.getType();
        if (!(actionType instanceof CommentAction)) {
            return;
        }

        switch ((CommentAction) actionType) {
            case FETCH_COMMENTS:
                fetchComments((FetchCommentsPayload) action.getPayload());
                break;
            case FETCHED_COMMENTS:
                handleFetchCommentsResponse((FetchCommentsResponsePayload) action.getPayload());
                break;
            case FETCH_COMMENT:
                fetchComment((RemoteCommentPayload) action.getPayload());
                break;
            case FETCHED_COMMENT:
                handleFetchCommentResponse((RemoteCommentResponsePayload) action.getPayload());
                break;
            case CREATE_NEW_COMMENT:
                createNewComment((RemoteCreateCommentPayload) action.getPayload());
                break;
            case CREATED_NEW_COMMENT:
                handleCreatedNewComment((RemoteCommentResponsePayload) action.getPayload());
                break;
            case UPDATE_COMMENT:
                updateComment((CommentModel) action.getPayload());
                break;
            case PUSH_COMMENT:
                pushComment((RemoteCommentPayload) action.getPayload());
                break;
            case PUSHED_COMMENT:
                handlePushCommentResponse((RemoteCommentResponsePayload) action.getPayload());
                break;
            case REMOVE_COMMENTS:
                removeComments((SiteModel) action.getPayload());
                break;
            case REMOVE_COMMENT:
                removeComment((CommentModel) action.getPayload());
                break;
            case REMOVE_ALL_COMMENTS:
                removeAllComments();
                break;
            case DELETE_COMMENT:
                deleteComment((RemoteCommentPayload) action.getPayload());
                break;
            case DELETED_COMMENT:
                handleDeletedCommentResponse((RemoteCommentResponsePayload) action.getPayload());
                break;
            case LIKE_COMMENT:
                likeComment((RemoteLikeCommentPayload) action.getPayload());
                break;
            case LIKED_COMMENT:
                handleLikedCommentResponse((RemoteCommentResponsePayload) action.getPayload());
                break;
            case FETCH_COMMENT_LIKES:
                fetchCommentLikes((FetchCommentLikesPayload) action.getPayload());
                break;
            case FETCHED_COMMENT_LIKES:
                handleFetchedCommentLikes((FetchedCommentLikesResponsePayload) action.getPayload());
                break;
        }
    }

    @Override
    public void onRegister() {
        AppLog.d(T.API, this.getClass().getName() + ": onRegister");
    }

    // Private methods

    private void createNewComment(@NonNull RemoteCreateCommentPayload payload) {
        if (payload.post != null && payload.reply == null) {
            // Create a new comment on a specific Post
            if (payload.site.isUsingWpComRestApi()) {
                mCommentRestClient.createNewComment(payload.site, payload.post, payload.comment);
            } else {
                mCommentXMLRPCClient.createNewComment(payload.site, payload.post, payload.comment);
            }
        } else if (payload.reply != null && payload.post == null) {
            // Create a new reply to a specific Comment
            if (payload.site.isUsingWpComRestApi()) {
                mCommentRestClient.createNewReply(payload.site, payload.comment, payload.reply);
            } else {
                mCommentXMLRPCClient.createNewReply(payload.site, payload.comment, payload.reply);
            }
        } else {
            throw new IllegalStateException(
                    "Either post or reply must be not null and both can't be not null at the same time!"
            );
        }
    }

    private void handleCreatedNewComment(@NonNull RemoteCommentResponsePayload payload) {
        OnCommentChanged event = new OnCommentChanged(1, CommentAction.CREATE_NEW_COMMENT);

        // Update the comment from the DB
        if (!payload.isError()) {
            CommentSqlUtils.insertOrUpdateComment(payload.comment);
        }
        if (payload.comment != null) {
            event.changedCommentsLocalIds.add(payload.comment.getId());
        }
        event.error = payload.error;
        emitChange(event);
    }

    private void updateComment(@NonNull CommentModel payload) {
        int rowsAffected = 0;
        if (!payload.isError()) {
            rowsAffected = CommentSqlUtils.insertOrUpdateComment(payload);
        }
        OnCommentChanged event = new OnCommentChanged(rowsAffected, CommentAction.UPDATE_COMMENT);
        event.changedCommentsLocalIds.add(payload.getId());
        emitChange(event);
    }

    private void removeComment(@NonNull CommentModel payload) {
        int rowsAffected = CommentSqlUtils.removeComment(payload);
        OnCommentChanged event = new OnCommentChanged(rowsAffected, CommentAction.REMOVE_COMMENT);
        event.changedCommentsLocalIds.add(payload.getId());
        emitChange(event);
    }

    private void removeComments(@NonNull SiteModel payload) {
        int rowsAffected = CommentSqlUtils.removeComments(payload);
        OnCommentChanged event = new OnCommentChanged(rowsAffected, CommentAction.REMOVE_COMMENTS);
        // Doesn't make sense to update here event.changedCommentsLocalIds
        emitChange(event);
    }

    private void removeAllComments() {
        int rowsAffected = CommentSqlUtils.deleteAllComments();
        OnCommentChanged event = new OnCommentChanged(rowsAffected, CommentAction.REMOVE_ALL_COMMENTS);
        emitChange(event);
    }

    private void deleteComment(@NonNull RemoteCommentPayload payload) {
        // If the comment is stored locally, we want to update it locally (needed because in some
        // cases we use this to update comments by remote id).
        CommentModel comment = payload.comment;
        if (payload.comment == null) {
            getCommentBySiteAndRemoteId(payload.site, payload.remoteCommentId);
        }
        if (payload.site.isUsingWpComRestApi()) {
            mCommentRestClient.deleteComment(payload.site, getPrioritizedRemoteCommentId(payload), comment);
        } else {
            mCommentXMLRPCClient.deleteComment(payload.site, getPrioritizedRemoteCommentId(payload), comment);
        }
    }

    private void handleDeletedCommentResponse(@NonNull RemoteCommentResponsePayload payload) {
        OnCommentChanged event = new OnCommentChanged(0, CommentAction.DELETE_COMMENT);
        if (payload.comment != null) {
            event.changedCommentsLocalIds.add(payload.comment.getId());
        }
        event.error = payload.error;
        if (!payload.isError()) {
            // Delete once means "send to trash", so we don't want to remove it from the DB, just update it's
            // status. Delete twice means "farewell comment, we won't see you ever again". Only delete from the DB if
            // the status is "deleted".
            if (payload.comment != null && payload.comment.getStatus().equals(CommentStatus.DELETED.toString())) {
                CommentSqlUtils.removeComment(payload.comment);
            } else {
                // Update the local copy, only the status should have changed ("trash")
                CommentSqlUtils.insertOrUpdateComment(payload.comment);
            }
        }
        emitChange(event);
    }

    private void fetchComments(@NonNull FetchCommentsPayload payload) {
        if (payload.site.isUsingWpComRestApi()) {
            mCommentRestClient.fetchComments(payload.site, payload.number, payload.offset, payload.status);
        } else {
            mCommentXMLRPCClient.fetchComments(payload.site, payload.number, payload.offset, payload.status);
        }
    }

    private void handleFetchCommentsResponse(@NonNull FetchCommentsResponsePayload payload) {
        int rowsAffected = 0;
        OnCommentChanged event = new OnCommentChanged(rowsAffected, CommentAction.FETCH_COMMENTS);
        if (!payload.isError()) {
            // Find comments that were deleted or moved to a different status on the server and remove them from
            // local DB.
            CommentSqlUtils.removeCommentGaps(
                    payload.site, payload.comments, payload.number, payload.offset, payload.requestedStatus);

            for (CommentModel comment : payload.comments) {
                rowsAffected += CommentSqlUtils.insertOrUpdateComment(comment);
                event.changedCommentsLocalIds.add(comment.getId());
            }
        }
        event.error = payload.error;
        event.requestedStatus = payload.requestedStatus;
        event.offset = payload.offset;
        emitChange(event);
    }

    private void pushComment(@NonNull RemoteCommentPayload payload) {
        if (payload.comment != null) {
            if (payload.site.isUsingWpComRestApi()) {
                mCommentRestClient.pushComment(payload.site, payload.comment);
            } else {
                mCommentXMLRPCClient.pushComment(payload.site, payload.comment);
            }
        } else {
            OnCommentChanged event = new OnCommentChanged(0, CommentAction.PUSH_COMMENT);
            event.error = new CommentError(CommentErrorType.INVALID_INPUT, "Comment can't be null");
            emitChange(event);
        }
    }

    private void handlePushCommentResponse(@NonNull RemoteCommentResponsePayload payload) {
        int rowsAffected = 0;
        if (!payload.isError()) {
            rowsAffected = CommentSqlUtils.insertOrUpdateComment(payload.comment);
        }
        OnCommentChanged event = new OnCommentChanged(rowsAffected, CommentAction.PUSH_COMMENT);
        if (payload.comment != null) {
            event.changedCommentsLocalIds.add(payload.comment.getId());
        }
        event.error = payload.error;
        emitChange(event);
    }

    private void fetchComment(@NonNull RemoteCommentPayload payload) {
        if (payload.site.isUsingWpComRestApi()) {
            mCommentRestClient.fetchComment(payload.site, getPrioritizedRemoteCommentId(payload), payload.comment);
        } else {
            mCommentXMLRPCClient.fetchComment(payload.site, getPrioritizedRemoteCommentId(payload), payload.comment);
        }
    }

    private long getPrioritizedRemoteCommentId(@NonNull RemoteCommentPayload payload) {
        if (payload.comment != null) {
            return payload.comment.getRemoteCommentId();
        } else {
            return payload.remoteCommentId;
        }
    }

    private void handleFetchCommentResponse(@NonNull RemoteCommentResponsePayload payload) {
        int rowsAffected = 0;
        if (!payload.isError()) {
            rowsAffected = CommentSqlUtils.insertOrUpdateComment(payload.comment);
        }
        OnCommentChanged event = new OnCommentChanged(rowsAffected, CommentAction.FETCH_COMMENT);
        if (payload.comment != null) {
            event.changedCommentsLocalIds.add(payload.comment.getId());
        }
        event.error = payload.error;
        emitChange(event);
    }

    private void likeComment(@NonNull RemoteLikeCommentPayload payload) {
        // If the comment is stored locally, we want to update it locally (needed because in some
        // cases we use this to update comments by remote id).
        CommentModel comment = payload.comment;
        if (payload.comment == null) {
            getCommentBySiteAndRemoteId(payload.site, payload.remoteCommentId);
        }
        if (payload.site.isUsingWpComRestApi()) {
            mCommentRestClient.likeComment(payload.site, getPrioritizedRemoteCommentId(payload), comment, payload.like);
        } else {
            OnCommentChanged event = new OnCommentChanged(0, CommentAction.LIKE_COMMENT);
            if (payload.comment != null) {
                event.changedCommentsLocalIds.add(payload.comment.getId());
            }
            event.error = new CommentError(CommentErrorType.INVALID_INPUT, "Can't like a comment on XMLRPC API");
            emitChange(event);
        }
    }

    private void handleLikedCommentResponse(@NonNull RemoteCommentResponsePayload payload) {
        int rowsAffected = 0;
        if (!payload.isError()) {
            rowsAffected = CommentSqlUtils.insertOrUpdateComment(payload.comment);
        }
        OnCommentChanged event = new OnCommentChanged(rowsAffected, CommentAction.LIKE_COMMENT);
        if (payload.comment != null) {
            event.changedCommentsLocalIds.add(payload.comment.getId());
        }
        event.error = payload.error;
        emitChange(event);
    }

    private void fetchCommentLikes(@NonNull FetchCommentLikesPayload payload) {
        mCommentRestClient.fetchCommentLikes(
                payload.siteId,
                payload.remoteCommentId,
                payload.requestNextPage,
                payload.pageLength
        );
    }

    private void handleFetchedCommentLikes(@NonNull FetchedCommentLikesResponsePayload payload) {
        OnCommentLikesChanged event = new OnCommentLikesChanged(
                payload.siteId,
                payload.commentRemoteId,
                payload.hasMore,
                CommentAction.FETCHED_COMMENT_LIKES
        );
        if (!payload.isError()) {
            if (!payload.isRequestNextPage) {
                CommentSqlUtils.deleteCommentLikesAndPurgeExpired(payload.siteId, payload.commentRemoteId);
            }

            for (LikeModel like : payload.likes) {
                CommentSqlUtils.insertOrUpdateCommentLikes(payload.siteId, payload.commentRemoteId, like);
            }
            event.commentLikes.addAll(CommentSqlUtils.getCommentLikesByCommentId(
                    payload.siteId,
                    payload.commentRemoteId
            ));
        } else {
            List<LikeModel> cachedLikes = CommentSqlUtils.getCommentLikesByCommentId(
                    payload.siteId,
                    payload.commentRemoteId
            );
            event.commentLikes.addAll(cachedLikes);
        }

        event.error = payload.error;
        emitChange(event);
    }
}
