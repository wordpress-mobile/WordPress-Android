package org.wordpress.android.fluxc.network.xmlrpc.comment;

import android.support.annotation.NonNull;

import com.android.volley.RequestQueue;
import com.android.volley.Response.Listener;

import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.CommentActionBuilder;
import org.wordpress.android.fluxc.generated.endpoint.XMLRPC;
import org.wordpress.android.fluxc.model.CommentModel;
import org.wordpress.android.fluxc.model.CommentStatus;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.network.BaseRequest.BaseErrorListener;
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError;
import org.wordpress.android.fluxc.network.HTTPAuthManager;
import org.wordpress.android.fluxc.network.UserAgent;
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken;
import org.wordpress.android.fluxc.network.xmlrpc.BaseXMLRPCClient;
import org.wordpress.android.fluxc.network.xmlrpc.XMLRPCRequest;
import org.wordpress.android.fluxc.network.xmlrpc.XMLRPCUtils;
import org.wordpress.android.fluxc.store.CommentStore.FetchCommentResponsePayload;
import org.wordpress.android.fluxc.store.CommentStore.FetchCommentsResponsePayload;
import org.wordpress.android.fluxc.store.CommentStore.PushCommentResponsePayload;
import org.wordpress.android.fluxc.utils.CommentErrorUtils;
import org.wordpress.android.util.DateTimeUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

public class CommentXMLRPCClient extends BaseXMLRPCClient {
    private static final int DEFAULT_NUMBER_COMMENTS = 20;

    @Inject
    public CommentXMLRPCClient(Dispatcher dispatcher, RequestQueue requestQueue, AccessToken accessToken,
                               UserAgent userAgent, HTTPAuthManager httpAuthManager) {
        super(dispatcher, requestQueue, accessToken, userAgent, httpAuthManager);
    }

    public void fetchComments(final SiteModel site, int offset, CommentStatus status) {
        List<Object> params = new ArrayList<>(2);
        Map<String, Object> commentParams = new HashMap<>();
        commentParams.put("number", DEFAULT_NUMBER_COMMENTS);
        commentParams.put("offset", offset);

        params.add(site.getSiteId());
        params.add(site.getUsername());
        params.add(site.getPassword());
        params.add(commentParams);
        final XMLRPCRequest request = new XMLRPCRequest(
                site.getXmlRpcUrl(), XMLRPC.GET_COMMENTS, params,
                new Listener<Object>() {
                    @Override
                    public void onResponse(Object response) {
                        List<CommentModel> comments = commentsResponseToCommentList(response, site);
                        FetchCommentsResponsePayload payload = new FetchCommentsResponsePayload(comments);
                        mDispatcher.dispatch(CommentActionBuilder.newFetchedCommentsAction(payload));
                    }
                },
                new BaseErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull BaseNetworkError error) {
                        mDispatcher.dispatch(CommentActionBuilder.newFetchedCommentsAction(
                                CommentErrorUtils.commentErrorToFetchCommentsPayload(error)));
                    }
                }
        );
        add(request);
    }

    public void pushComment(final SiteModel site, final CommentModel comment) {
        List<Object> params = new ArrayList<>(2);
        Map<String, Object> commentParams = new HashMap<>();
        commentParams.put("content", comment.getContent());
        commentParams.put("date", comment.getDatePublished());
        String status = getXMLRPCCommentStatus(CommentStatus.fromString(comment.getStatus()));
        commentParams.put("status", status);

        params.add(site.getSiteId());
        params.add(site.getUsername());
        params.add(site.getPassword());
        params.add(comment.getRemoteCommentId());
        params.add(commentParams);
        final XMLRPCRequest request = new XMLRPCRequest(
                site.getXmlRpcUrl(), XMLRPC.EDIT_COMMENT, params,
                new Listener<Object>() {
                    @Override
                    public void onResponse(Object response) {
                        CommentModel updatedComment = commentResponseToComment(response, site);
                        PushCommentResponsePayload payload = new PushCommentResponsePayload(updatedComment);
                        mDispatcher.dispatch(CommentActionBuilder.newPushedCommentAction(payload));
                    }
                },
                new BaseErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull BaseNetworkError error) {
                        mDispatcher.dispatch(CommentActionBuilder.newPushedCommentAction(
                                CommentErrorUtils.commentErrorToPushCommentPayload(error, comment)));
                    }
                }
        );
        add(request);
    }

    public void fetchComment(final SiteModel site, final CommentModel comment) {
        List<Object> params = new ArrayList<>(2);
        params.add(site.getSiteId());
        params.add(site.getUsername());
        params.add(site.getPassword());
        //params.add(comment.getRemoteCommentId());
        final XMLRPCRequest request = new XMLRPCRequest(
                site.getXmlRpcUrl(), XMLRPC.GET_COMMENT, params,
                new Listener<Object>() {
                    @Override
                    public void onResponse(Object response) {
                        CommentModel updatedComment = commentResponseToComment(response, site);
                        FetchCommentResponsePayload payload = new FetchCommentResponsePayload(updatedComment);
                        mDispatcher.dispatch(CommentActionBuilder.newFetchedCommentAction(payload));
                    }
                },
                new BaseErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull BaseNetworkError error) {
                        mDispatcher.dispatch(CommentActionBuilder.newFetchedCommentAction(
                                CommentErrorUtils.commentErrorToFetchCommentPayload(error, comment)));
                    }
                }
        );
        add(request);
    }

    // Private methods

    private String getXMLRPCCommentStatus(CommentStatus status) {
        switch (status) {
            case APPROVED:
                return "approve";
            case UNAPPROVED:
                return "hold";
            case SPAM:
                return "spam";
            case TRASH:
                return "trash";
            default:
            case ALL:
            case UNSPAM:
            case UNTRASH:
                return "approve";
        }
    }

    private List<CommentModel> commentsResponseToCommentList(Object response, SiteModel site) {
        List<CommentModel> comments = new ArrayList<>();
        if (!(response instanceof Object[])) {
            return comments;
        }
        Object[] responseArray = (Object[]) response;
        for (Object commentObject: responseArray) {
            CommentModel commentModel = commentResponseToComment(commentObject, site);
            if (commentModel != null) {
                comments.add(commentModel);
            }
        }
        return comments;
    }

    private CommentModel commentResponseToComment(Object commentObject, SiteModel site) {
        if (!(commentObject instanceof HashMap)) {
            return null;
        }
        HashMap<?, ?> commentMap = (HashMap<?, ?>) commentObject;
        CommentModel comment = new CommentModel();

        comment.setRemoteCommentId(
                Long.valueOf(XMLRPCUtils.safeGetMapValue(commentMap, "comment_id", "0")));
        comment.setLocalSiteId(site.getId());
        comment.setRemoteSiteId(site.getSiteId());
        comment.setStatus(XMLRPCUtils.safeGetMapValue(commentMap, "status", "approve"));
        Date datePublished = XMLRPCUtils.safeGetMapValue(commentMap, "date_created_gmt", new Date());
        comment.setDatePublished(DateTimeUtils.iso8601UTCFromDate(datePublished));
        comment.setContent(XMLRPCUtils.safeGetMapValue(commentMap, "content", ""));

        // Author
        comment.setAuthorUrl(XMLRPCUtils.safeGetMapValue(commentMap, "author_url", ""));
        comment.setAuthorName(XMLRPCUtils.safeGetMapValue(commentMap, "author", ""));
        comment.setAuthorEmail(XMLRPCUtils.safeGetMapValue(commentMap, "author_email", ""));
        // TODO: comment.setAuthorProfileImageUrl(); - get the hash from the email address?

        // Post
        comment.setRemotePostId(XMLRPCUtils.safeGetMapValue(commentMap, "post_id", 0));
        comment.setPostTitle(XMLRPCUtils.safeGetMapValue(commentMap, "post_title", ""));

        return comment;
    }
}
