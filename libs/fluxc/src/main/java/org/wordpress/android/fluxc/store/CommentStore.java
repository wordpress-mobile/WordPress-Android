package org.wordpress.android.fluxc.store;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.greenrobot.eventbus.Subscribe;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.Payload;
import org.wordpress.android.fluxc.action.CommentAction;
import org.wordpress.android.fluxc.annotations.action.Action;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.fluxc.model.CommentModel;
import org.wordpress.android.fluxc.model.CommentStatus;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.network.rest.wpcom.comment.CommentRestClient;
import org.wordpress.android.fluxc.network.xmlrpc.comment.CommentXMLRPCClient;
import org.wordpress.android.fluxc.persistence.CommentSqlUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.DateTimeUtils;

import java.util.List;

import javax.inject.Inject;

public class CommentStore extends Store {
    CommentRestClient mCommentRestClient;
    CommentXMLRPCClient mCommentXMLRPCClient;

    // Payloads

    public static class FetchCommentsPayload extends Payload {
        public final SiteModel site;
        public final int number;
        public final int offset;

        public FetchCommentsPayload(@NonNull SiteModel site, int number, int offset) {
            this.site = site;
            this.number = number;
            this.offset = offset;
        }
    }

    public static class RemoteCommentPayload extends Payload {
        public final SiteModel site;
        public final CommentModel comment;
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

    public static class InstantiateCommentPayload extends Payload {
        public final SiteModel site;

        public InstantiateCommentPayload(@NonNull SiteModel site) {
            this.site = site;
        }
    }

    public static class FetchCommentsResponsePayload extends Payload {
        public final List<CommentModel> comments;
        public CommentError error;
        public FetchCommentsResponsePayload(@NonNull List<CommentModel> comments) {
            this.comments = comments;
        }
    }

    public static class RemoteCommentResponsePayload extends Payload {
        public final CommentModel comment;
        public CommentError error;
        public RemoteCommentResponsePayload(@Nullable CommentModel comment) {
            this.comment = comment;
        }
    }

    public static class RemoveCommentsPayload extends Payload {
        public final SiteModel site;
        public RemoveCommentsPayload(@NonNull SiteModel site) {
            this.site = site;
        }
    }

    public static class RemoteCreateCommentPayload extends Payload {
        public final SiteModel site;
        public final CommentModel comment;
        public final CommentModel reply;
        public final PostModel post;

        public CommentError error;
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

    public class OnCommentChanged extends OnChanged<CommentError> {
        public int rowsAffected;
        public CommentAction causeOfChange;
        public OnCommentChanged(int rowsAffected) {
            this.rowsAffected = rowsAffected;
        }
    }

    public class OnCommentInstantiated extends OnChanged<CommentError> {
        public CommentModel comment;
        public OnCommentInstantiated(CommentModel comment) {
            this.comment = comment;
        }
    }

    // Constructor

    @Inject
    public CommentStore(Dispatcher dispatcher, CommentRestClient commentRestClient, CommentXMLRPCClient
            commentXMLRPCClient) {
        super(dispatcher);
        mCommentRestClient = commentRestClient;
        mCommentXMLRPCClient = commentXMLRPCClient;
    }

    // Getters

    public List<CommentModel> getCommentsForSite(SiteModel site, CommentStatus status) {
        return CommentSqlUtils.getCommentsForSite(site, status);
    }

    public int getNumberOfCommentsForSite(SiteModel site, CommentStatus status) {
        return CommentSqlUtils.getCommentsCountForSite(site, status);
    }

    public CommentModel getCommentBySiteAndRemoteId(SiteModel site, long remoteCommentId) {
        return CommentSqlUtils.getCommentBySiteAndRemoteId(site, remoteCommentId);
    }

    public CommentModel getCommentByLocalId(int localId) {
        return CommentSqlUtils.getCommentByLocalCommentId(localId);
    }

    // Store Methods

    @Override
    @Subscribe
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
            case INSTANTIATE_COMMENT:
                instantiateComment((InstantiateCommentPayload) action.getPayload());
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
            case DELETE_COMMENT:
                deleteComment((RemoteCommentPayload) action.getPayload());
                break;
            case DELETED_COMMENT:
                handleDeletedCommentResponse((RemoteCommentResponsePayload) action.getPayload());
                break;
        }
    }

    @Override
    public void onRegister() {
        AppLog.d(T.API, this.getClass().getName() + ": onRegister");
    }

    // Private methods

    private void createNewComment(RemoteCreateCommentPayload payload) {
        if (payload.reply == null) {
            // Create a new comment on a specific Post
            if (payload.site.isWPCom()) {
                mCommentRestClient.createNewComment(payload.site, payload.post, payload.comment);
            } else {
                mCommentXMLRPCClient.createNewComment(payload.site, payload.post, payload.comment);
            }
        } else {
            // Create a new reply to a specific Comment
            if (payload.site.isWPCom()) {
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
        event.error = payload.error;
        emitChange(event);
    }

    private void updateComment(CommentModel payload) {
        int rowsAffected = 0;
        if (!payload.isError()) {
            rowsAffected = CommentSqlUtils.insertOrUpdateComment(payload);
        }
        OnCommentChanged event = new OnCommentChanged(rowsAffected);
        event.causeOfChange = CommentAction.UPDATE_COMMENT;
        emitChange(event);
    }

    private void removeComment(CommentModel payload) {
        CommentSqlUtils.removeComment(payload);
    }

    private void removeComments(SiteModel payload) {
        CommentSqlUtils.removeComments(payload);
    }

    private void instantiateComment(InstantiateCommentPayload payload) {
        CommentModel comment = new CommentModel();
        comment.setLocalSiteId(payload.site.getId());
        // Init with defaults
        comment.setContent("");
        comment.setDatePublished(DateTimeUtils.iso8601FromDate(DateTimeUtils.nowUTC()));
        comment.setStatus(CommentStatus.APPROVED.toString());
        comment.setAuthorName("");
        comment.setAuthorEmail("");
        comment.setAuthorUrl("");
        // Insert in the DB
        CommentSqlUtils.insertOrUpdateComment(comment);
        emitChange(new OnCommentInstantiated(comment));
    }

    private void deleteComment(RemoteCommentPayload payload) {
        if (payload.site.isWPCom()) {
            mCommentRestClient.deleteComment(payload.site, payload.remoteCommentId, payload.comment);
        } else {
            mCommentXMLRPCClient.deleteComment(payload.site, payload.remoteCommentId, payload.comment);
        }
    }

    private void handleDeletedCommentResponse(RemoteCommentResponsePayload payload) {
        OnCommentChanged event = new OnCommentChanged(0);
        event.causeOfChange = CommentAction.DELETE_COMMENT;
        event.error = payload.error;
        if (!payload.isError()) {
            // Delete once means "send to trash", so we don't want to remove it from the DB, just update it's
            // status. Delete twice means "farewell comment, we won't you ever again". Only delete from the DB if the
            // status is "deleted".
            if (payload.comment.getStatus().equals(CommentStatus.DELETED.toString())) {
                CommentSqlUtils.removeComment(payload.comment);
            } else {
                // Update the local copy, only the status should have changed ("trash")
                CommentSqlUtils.insertOrUpdateComment(payload.comment);
            }
        }
        emitChange(event);
    }

    private void fetchComments(FetchCommentsPayload payload) {
        if (payload.site.isWPCom()) {
            mCommentRestClient.fetchComments(payload.site, payload.number, payload.offset, CommentStatus.ALL);
        } else {
            mCommentXMLRPCClient.fetchComments(payload.site, payload.number, payload.offset, CommentStatus.ALL);
        }
    }

    private void handleFetchCommentsResponse(FetchCommentsResponsePayload payload) {
        int rowsAffected = 0;
        if (!payload.isError()) {
            for (CommentModel comment : payload.comments) {
                rowsAffected += CommentSqlUtils.insertOrUpdateComment(comment);
            }
        }
        OnCommentChanged event = new OnCommentChanged(rowsAffected);
        event.causeOfChange = CommentAction.FETCH_COMMENTS;
        event.error = payload.error;
        emitChange(event);
    }

    private void pushComment(RemoteCommentPayload payload) {
        if (payload.comment == null) {
            OnCommentChanged event = new OnCommentChanged(0);
            event.causeOfChange = CommentAction.PUSH_COMMENT;
            event.error = new CommentError(CommentErrorType.INVALID_INPUT, "comment can't be null");
            emitChange(event);
            return;
        }
        if (payload.site.isWPCom()) {
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
        event.causeOfChange = CommentAction.PUSH_COMMENT;
        event.error = payload.error;
        emitChange(event);
    }

    private void fetchComment(RemoteCommentPayload payload) {
        if (payload.site.isWPCom()) {
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
        event.causeOfChange = CommentAction.FETCH_COMMENT;
        event.error = payload.error;
        emitChange(event);
    }
}
