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
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError;
import org.wordpress.android.fluxc.network.rest.wpcom.comment.CommentRestClient;
import org.wordpress.android.fluxc.network.xmlrpc.comment.CommentXMLRPCClient;
import org.wordpress.android.fluxc.persistence.CommentSqlUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.DateTimeUtils;

import java.util.ArrayList;
import java.util.Date;
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

        public FetchCommentsResponsePayload(@NonNull List<CommentModel> comments, @NonNull SiteModel site, int number,
                                            int offset) {
            this.comments = comments;
            this.site = site;
            this.number = number;
            this.offset = offset;
        }
    }

    public static class RemoteCommentResponsePayload extends Payload<CommentError> {
        @Nullable public final CommentModel comment;
        public RemoteCommentResponsePayload(@Nullable CommentModel comment) {
            this.comment = comment;
        }
    }

    public static class RemoteCreateCommentPayload extends Payload<CommentError> {
        public final SiteModel site;
        public final CommentModel comment;
        public final CommentModel reply;
        public final PostModel post;

        public RemoteCreateCommentPayload(@NonNull SiteModel site, @NonNull PostModel post,
                                          @NonNull CommentModel comment) {
            this.site = site;
            this.post = post;
            this.comment = comment;
            this.reply = null;
        }

        public RemoteCreateCommentPayload(@NonNull SiteModel site, @NonNull CommentModel comment,
                                          @NonNull CommentModel reply) {
            this.site = site;
            this.comment = comment;
            this.reply = reply;
            this.post = null;
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
        public CommentErrorType type;
        public String message;
        public CommentError(CommentErrorType type, @NonNull String message) {
            this.type = type;
            this.message = message;
        }
    }

    // Actions

    public static class OnCommentChanged extends OnChanged<CommentError> {
        public int rowsAffected;
        public CommentAction causeOfChange;
        public List<Integer> changedCommentsLocalIds = new ArrayList<>();
        public OnCommentChanged(int rowsAffected) {
            this.rowsAffected = rowsAffected;
        }
    }

    // Constructor

    @Inject
    public CommentStore(Dispatcher dispatcher, CommentRestClient commentRestClient,
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
     */
    @SuppressLint("WrongConstant")
    public List<CommentModel> getCommentsForSite(SiteModel site, boolean orderByDateAscending,
                                                 CommentStatus... statuses) {
        @Order int order = orderByDateAscending ? SelectQuery.ORDER_ASCENDING : SelectQuery.ORDER_DESCENDING;
        return CommentSqlUtils.getCommentsForSite(site, order, statuses);
    }

    public int getNumberOfCommentsForSite(SiteModel site, CommentStatus... statuses) {
        return CommentSqlUtils.getCommentsCountForSite(site, statuses);
    }

    public CommentModel getCommentBySiteAndRemoteId(SiteModel site, long remoteCommentId) {
        return CommentSqlUtils.getCommentBySiteAndRemoteId(site, remoteCommentId);
    }

    public CommentModel getCommentByLocalId(int localId) {
        return CommentSqlUtils.getCommentByLocalCommentId(localId);
    }

    // Store Methods

    @Override
    @Subscribe(threadMode = ThreadMode.ASYNC)
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
        }
    }

    @Override
    public void onRegister() {
        AppLog.d(T.API, this.getClass().getName() + ": onRegister");
    }

    public CommentModel instantiateCommentModel(SiteModel site) {
        CommentModel comment = new CommentModel();
        comment.setLocalSiteId(site.getId());
        // Init with defaults
        comment.setContent("");
        comment.setDatePublished(DateTimeUtils.iso8601UTCFromDate(new Date()));
        comment.setStatus(CommentStatus.APPROVED.toString());
        comment.setAuthorName("");
        comment.setAuthorEmail("");
        comment.setAuthorUrl("");
        comment.setUrl("");
        // Insert in the DB
        comment = CommentSqlUtils.insertCommentForResult(comment);

        if (comment.getId() == -1) {
            comment = null;
        }

        return comment;
    }

    // Private methods

    private void createNewComment(RemoteCreateCommentPayload payload) {
        if (payload.reply == null) {
            // Create a new comment on a specific Post
            if (payload.site.isUsingWpComRestApi()) {
                mCommentRestClient.createNewComment(payload.site, payload.post, payload.comment);
            } else {
                mCommentXMLRPCClient.createNewComment(payload.site, payload.post, payload.comment);
            }
        } else {
            // Create a new reply to a specific Comment
            if (payload.site.isUsingWpComRestApi()) {
                mCommentRestClient.createNewReply(payload.site, payload.comment, payload.reply);
            } else {
                mCommentXMLRPCClient.createNewReply(payload.site, payload.comment, payload.reply);
            }
        }
    }

    private void handleCreatedNewComment(RemoteCommentResponsePayload payload) {
        OnCommentChanged event = new OnCommentChanged(1);
        event.causeOfChange = CommentAction.CREATE_NEW_COMMENT;

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

    private void updateComment(CommentModel payload) {
        int rowsAffected = 0;
        if (!payload.isError()) {
            rowsAffected = CommentSqlUtils.insertOrUpdateComment(payload);
        }
        OnCommentChanged event = new OnCommentChanged(rowsAffected);
        event.changedCommentsLocalIds.add(payload.getId());
        event.causeOfChange = CommentAction.UPDATE_COMMENT;
        emitChange(event);
    }

    private void removeComment(CommentModel payload) {
        int rowsAffected = CommentSqlUtils.removeComment(payload);
        OnCommentChanged event = new OnCommentChanged(rowsAffected);
        event.causeOfChange = CommentAction.REMOVE_COMMENT;
        event.changedCommentsLocalIds.add(payload.getId());
        emitChange(event);
    }

    private void removeComments(SiteModel payload) {
        int rowsAffected = CommentSqlUtils.removeComments(payload);
        OnCommentChanged event = new OnCommentChanged(rowsAffected);
        // Doesn't make sense to update here event.changedCommentsLocalIds
        event.causeOfChange = CommentAction.REMOVE_COMMENTS;
        emitChange(event);
    }

    private void removeAllComments() {
        int rowsAffected = CommentSqlUtils.deleteAllComments();
        OnCommentChanged event = new OnCommentChanged(rowsAffected);
        event.causeOfChange = CommentAction.REMOVE_ALL_COMMENTS;
        emitChange(event);
    }

    private void deleteComment(RemoteCommentPayload payload) {
        // If the comment is stored locally, we want to update it locally (needed because in some
        // cases we use this to update comments by remote id).
        CommentModel comment = payload.comment;
        if (payload.comment == null) {
            getCommentBySiteAndRemoteId(payload.site, payload.remoteCommentId);
        }
        if (payload.site.isUsingWpComRestApi()) {
            mCommentRestClient.deleteComment(payload.site, payload.remoteCommentId, comment);
        } else {
            mCommentXMLRPCClient.deleteComment(payload.site, payload.remoteCommentId, comment);
        }
    }

    private void handleDeletedCommentResponse(RemoteCommentResponsePayload payload) {
        OnCommentChanged event = new OnCommentChanged(0);
        if (payload.comment != null) {
            event.changedCommentsLocalIds.add(payload.comment.getId());
        }
        event.causeOfChange = CommentAction.DELETE_COMMENT;
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

    private void fetchComments(FetchCommentsPayload payload) {
        if (payload.site.isUsingWpComRestApi()) {
            mCommentRestClient.fetchComments(payload.site, payload.number, payload.offset, payload.status);
        } else {
            mCommentXMLRPCClient.fetchComments(payload.site, payload.number, payload.offset, payload.status);
        }
    }

    private void handleFetchCommentsResponse(FetchCommentsResponsePayload payload) {
        int rowsAffected = 0;
        OnCommentChanged event = new OnCommentChanged(rowsAffected);
        if (!payload.isError()) {
            // Clear existing comments in case some were deleted on the server. Only remove them if we request the
            // first comments (offset == 0).
            if (payload.offset == 0) {
                CommentSqlUtils.removeComments(payload.site);
            }

            for (CommentModel comment : payload.comments) {
                rowsAffected += CommentSqlUtils.insertOrUpdateComment(comment);
                event.changedCommentsLocalIds.add(comment.getId());
            }
        }
        event.causeOfChange = CommentAction.FETCH_COMMENTS;
        event.error = payload.error;
        emitChange(event);
    }

    private void pushComment(RemoteCommentPayload payload) {
        if (payload.comment == null) {
            OnCommentChanged event = new OnCommentChanged(0);
            event.causeOfChange = CommentAction.PUSH_COMMENT;
            event.error = new CommentError(CommentErrorType.INVALID_INPUT, "Comment can't be null");
            emitChange(event);
            return;
        }
        if (payload.site.isUsingWpComRestApi()) {
            mCommentRestClient.pushComment(payload.site, payload.comment);
        } else {
            mCommentXMLRPCClient.pushComment(payload.site, payload.comment);
        }
    }

    private void handlePushCommentResponse(RemoteCommentResponsePayload payload) {
        int rowsAffected = 0;
        if (!payload.isError()) {
            rowsAffected = CommentSqlUtils.insertOrUpdateComment(payload.comment);
        }
        OnCommentChanged event = new OnCommentChanged(rowsAffected);
        if (payload.comment != null) {
            event.changedCommentsLocalIds.add(payload.comment.getId());
        }
        event.causeOfChange = CommentAction.PUSH_COMMENT;
        event.error = payload.error;
        emitChange(event);
    }

    private void fetchComment(RemoteCommentPayload payload) {
        if (payload.site.isUsingWpComRestApi()) {
            mCommentRestClient.fetchComment(payload.site, payload.remoteCommentId, payload.comment);
        } else {
            mCommentXMLRPCClient.fetchComment(payload.site, payload.remoteCommentId, payload.comment);
        }
    }

    private void handleFetchCommentResponse(RemoteCommentResponsePayload payload) {
        int rowsAffected = 0;
        if (!payload.isError()) {
            rowsAffected = CommentSqlUtils.insertOrUpdateComment(payload.comment);
        }
        OnCommentChanged event = new OnCommentChanged(rowsAffected);
        if (payload.comment != null) {
            event.changedCommentsLocalIds.add(payload.comment.getId());
        }
        event.causeOfChange = CommentAction.FETCH_COMMENT;
        event.error = payload.error;
        emitChange(event);
    }

    private void likeComment(RemoteLikeCommentPayload payload) {
        // If the comment is stored locally, we want to update it locally (needed because in some
        // cases we use this to update comments by remote id).
        CommentModel comment = payload.comment;
        if (payload.comment == null) {
            getCommentBySiteAndRemoteId(payload.site, payload.remoteCommentId);
        }
        if (payload.site.isUsingWpComRestApi()) {
            mCommentRestClient.likeComment(payload.site, payload.remoteCommentId, comment, payload.like);
        } else {
            OnCommentChanged event = new OnCommentChanged(0);
            event.causeOfChange = CommentAction.LIKE_COMMENT;
            if (payload.comment != null) {
                event.changedCommentsLocalIds.add(payload.comment.getId());
            }
            event.error = new CommentError(CommentErrorType.INVALID_INPUT, "Can't like a comment on XMLRPC API");
            emitChange(event);
        }
    }

    private void handleLikedCommentResponse(RemoteCommentResponsePayload payload) {
        int rowsAffected = 0;
        if (!payload.isError()) {
            rowsAffected = CommentSqlUtils.insertOrUpdateComment(payload.comment);
        }
        OnCommentChanged event = new OnCommentChanged(rowsAffected);
        if (payload.comment != null) {
            event.changedCommentsLocalIds.add(payload.comment.getId());
        }
        event.causeOfChange = CommentAction.LIKE_COMMENT;
        event.error = payload.error;
        emitChange(event);
    }
}
