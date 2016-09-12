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
        final public SiteModel site;
        final public boolean loadMore;

        public FetchCommentsPayload(@NonNull SiteModel site) {
            this.site = site;
            this.loadMore = false;
        }

        public FetchCommentsPayload(@NonNull SiteModel site, boolean loadMore) {
            this.site = site;
            this.loadMore = loadMore;
        }
    }

    public static class FetchCommentsResponsePayload extends Payload {
        final public List<CommentModel> comments;

        public FetchCommentsResponsePayload(@NonNull List<CommentModel> comments) {
            this.comments = comments;
        }
    }

    // Errors

    public enum CommentErrorType {
        INVALID_SITE,
        GENERIC_ERROR
    }

    public static class CommentError implements OnChangedError {
        public CommentErrorType type;

        public CommentError(CommentErrorType type) {
            this.type = type;
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
        }
    }

    private void fetchComments(FetchCommentsPayload payload) {
        int offset = 0;
        if (payload.loadMore) {
            offset = 2; // TODO:
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
        // FIXME: error management
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
