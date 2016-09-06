package org.wordpress.android.fluxc.store;

import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.annotations.action.Action;
import org.wordpress.android.fluxc.network.rest.wpcom.comment.CommentRestClient;
import org.wordpress.android.fluxc.network.xmlrpc.comment.CommentXMLRPCClient;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import javax.inject.Inject;

public class CommentStore extends Store {
    CommentRestClient mCommentRestClient;
    CommentXMLRPCClient mCommentXMLRPCClient;

    @Inject
    public CommentStore(Dispatcher dispatcher, CommentRestClient commentRestClient, CommentXMLRPCClient
            commentXMLRPCClient) {
        super(dispatcher);
        mCommentRestClient = commentRestClient;
        mCommentXMLRPCClient = commentXMLRPCClient;
    }

    @Override
    public void onAction(Action action) {
    }

    @Override
    public void onRegister() {
        AppLog.d(T.API, this.getClass().getName() + ": onRegister");
    }
}
