package org.wordpress.android.fluxc.store;

import android.support.annotation.NonNull;

import org.greenrobot.eventbus.Subscribe;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.Payload;
import org.wordpress.android.fluxc.action.CommentAction;
import org.wordpress.android.fluxc.annotations.action.Action;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.fluxc.model.CommentModel;
import org.wordpress.android.fluxc.model.CommentStatus;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.network.rest.wpcom.comment.CommentRestClient;
import org.wordpress.android.fluxc.network.xmlrpc.comment.CommentXMLRPCClient;
import org.wordpress.android.fluxc.persistence.CommentSqlUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.util.List;

import javax.inject.Inject;

public class CommentStore extends Store {
    CommentRestClient mCommentRestClient;
    CommentXMLRPCClient mCommentXMLRPCClient;

    // Payloads

    public static class FetchCommentsPayload extends Payload {
        public final SiteModel site;
        public final boolean loadMore;

        public FetchCommentsPayload(@NonNull SiteModel site) {
            this.site = site;
            this.loadMore = false;
        }

        public FetchCommentsPayload(@NonNull SiteModel site, boolean loadMore) {
            this.site = site;
            this.loadMore = loadMore;
        }
    }

    public static class FetchCommentPayload extends Payload {
        public final SiteModel site;
        public final CommentModel comment;

        public FetchCommentPayload(@NonNull SiteModel site, @NonNull CommentModel comment) {
            this.site = site;
            this.comment = comment;
        }
    }

    public static class FetchCommentsResponsePayload extends Payload {
        public final List<CommentModel> comments;
        public CommentError error;
        public FetchCommentsResponsePayload(@NonNull List<CommentModel> comments) {
            this.comments = comments;
        }
    }

    public static class FetchCommentResponsePayload extends Payload {
        public final CommentModel comment;
        public CommentError error;
        public FetchCommentResponsePayload(@NonNull CommentModel comment) {
            this.comment = comment;
        }
    }

    public static class PushCommentPayload extends Payload {
        public final CommentModel comment;
        public final SiteModel site;

        public PushCommentPayload(@NonNull SiteModel site, @NonNull CommentModel comment) {
            this.comment = comment;
            this.site = site;
        }
    }

    public static class PushCommentResponsePayload extends Payload {
        public final CommentModel comment;
        public CommentError error;

        public PushCommentResponsePayload(@NonNull CommentModel comment) {
            this.comment = comment;
        }
    }

    // Errors

    public enum CommentErrorType {
        GENERIC_ERROR,
        INVALID_RESPONSE,
        INVALID_COMMENT
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
        public OnCommentChanged(int rowsAffected) {
            this.rowsAffected = rowsAffected;
        }
    }

    @Inject
    public CommentStore(Dispatcher dispatcher, CommentRestClient commentRestClient, CommentXMLRPCClient
            commentXMLRPCClient) {
        super(dispatcher);
        mCommentRestClient = commentRestClient;
        mCommentXMLRPCClient = commentXMLRPCClient;
    }

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
                fetchComment((FetchCommentPayload) action.getPayload());
                break;
            case FETCHED_COMMENT:
                handleFetchCommentResponse((FetchCommentResponsePayload) action.getPayload());
                break;
            case PUSH_COMMENT:
                pushComment((PushCommentPayload) action.getPayload());
                break;
            case PUSHED_COMMENT:
                handlePushCommentResponse((PushCommentResponsePayload) action.getPayload());
                break;
        }
    }

    private void fetchComments(FetchCommentsPayload payload) {
        int offset = 0;
        if (payload.loadMore) {
            offset = 20; // FIXME: do something here
        }
        if (payload.site.isWPCom()) {
            mCommentRestClient.fetchComments(payload.site, offset, CommentStatus.ALL);
        } else {
            mCommentXMLRPCClient.fetchComments(payload.site, offset, CommentStatus.ALL);
        }
    }

    private void handleFetchCommentsResponse(FetchCommentsResponsePayload payload) {
        int rowsAffected = 0;
        for (CommentModel comment : payload.comments) {
            rowsAffected += CommentSqlUtils.insertOrUpdateComment(comment);
        }
        OnCommentChanged event = new OnCommentChanged(rowsAffected);
        event.error = payload.error;
        emitChange(event);
    }

    private void pushComment(PushCommentPayload payload) {
        if (payload.site.isWPCom()) {
            mCommentRestClient.pushComment(payload.site, payload.comment);
        } else {
            mCommentXMLRPCClient.pushComment(payload.site, payload.comment);
        }
    }

    private void handlePushCommentResponse(PushCommentResponsePayload payload) {
        int rowsAffected = CommentSqlUtils.insertOrUpdateComment(payload.comment);
        OnCommentChanged event = new OnCommentChanged(rowsAffected);
        event.error = payload.error;
        emitChange(event);
    }

    private void fetchComment(FetchCommentPayload payload) {
        if (payload.site.isWPCom()) {
            mCommentRestClient.fetchComment(payload.site, payload.comment);
        } else {
            mCommentXMLRPCClient.fetchComment(payload.site, payload.comment);
        }
    }

    private void handleFetchCommentResponse(FetchCommentResponsePayload payload) {
        int rowsAffected = CommentSqlUtils.insertOrUpdateComment(payload.comment);
        OnCommentChanged event = new OnCommentChanged(rowsAffected);
        event.error = payload.error;
        emitChange(event);
    }

    @Override
    public void onRegister() {
        AppLog.d(T.API, this.getClass().getName() + ": onRegister");
    }

    // Getters

    public List<CommentModel> getCommentsForSite(SiteModel site) {
        return CommentSqlUtils.getCommentsForSite(site);
    }
}
