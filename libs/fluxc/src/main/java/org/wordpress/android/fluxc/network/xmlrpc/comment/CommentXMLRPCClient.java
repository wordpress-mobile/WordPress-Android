package org.wordpress.android.fluxc.network.xmlrpc.comment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.volley.RequestQueue;
import com.android.volley.Response.Listener;

import org.apache.commons.text.StringEscapeUtils;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.CommentActionBuilder;
import org.wordpress.android.fluxc.generated.endpoint.XMLRPC;
import org.wordpress.android.fluxc.model.CommentModel;
import org.wordpress.android.fluxc.model.CommentStatus;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.network.HTTPAuthManager;
import org.wordpress.android.fluxc.network.UserAgent;
import org.wordpress.android.fluxc.network.xmlrpc.BaseXMLRPCClient;
import org.wordpress.android.fluxc.network.xmlrpc.XMLRPCRequest;
import org.wordpress.android.fluxc.network.xmlrpc.XMLRPCUtils;
import org.wordpress.android.fluxc.store.CommentStore.CommentError;
import org.wordpress.android.fluxc.store.CommentStore.CommentErrorType;
import org.wordpress.android.fluxc.store.CommentStore.FetchCommentsResponsePayload;
import org.wordpress.android.fluxc.store.CommentStore.RemoteCommentResponsePayload;
import org.wordpress.android.fluxc.utils.CommentErrorUtils;
import org.wordpress.android.util.DateTimeUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
public class CommentXMLRPCClient extends BaseXMLRPCClient {
    @Inject public CommentXMLRPCClient(
            Dispatcher dispatcher,
            @Named("custom-ssl") RequestQueue requestQueue,
            UserAgent userAgent,
            HTTPAuthManager httpAuthManager) {
        super(dispatcher, requestQueue, userAgent, httpAuthManager);
    }

    public void fetchComments(
            @NonNull final SiteModel site,
            final int number,
            final int offset,
            @NonNull final CommentStatus status) {
        List<Object> params = new ArrayList<>(4);
        Map<String, Object> commentParams = new HashMap<>();
        commentParams.put("number", number);
        commentParams.put("offset", offset);
        if (status != CommentStatus.ALL) {
            commentParams.put("status", getXMLRPCCommentStatus(status));
        }

        params.add(site.getSelfHostedSiteId());
        params.add(site.getUsername());
        params.add(site.getPassword());
        params.add(commentParams);
        final XMLRPCRequest request = new XMLRPCRequest(
                site.getXmlRpcUrl(), XMLRPC.GET_COMMENTS, params,
                (Listener<Object>) response -> {
                    List<CommentModel> comments = commentsResponseToCommentList(response, site);
                    FetchCommentsResponsePayload payload = new FetchCommentsResponsePayload(
                            comments, site, number, offset, status
                    );
                    mDispatcher.dispatch(CommentActionBuilder.newFetchedCommentsAction(payload));
                },
                error -> mDispatcher.dispatch(CommentActionBuilder.newFetchedCommentsAction(
                        CommentErrorUtils.commentErrorToFetchCommentsPayload(error, site))));
        add(request);
    }

    public void pushComment(
            @NonNull final SiteModel site,
            @NonNull final CommentModel comment) {
        List<Object> params = new ArrayList<>(5);
        Map<String, Object> commentParams = new HashMap<>();
        commentParams.put("content", comment.getContent());
        commentParams.put("date", comment.getDatePublished());
        String status = getXMLRPCCommentStatus(CommentStatus.fromString(comment.getStatus()));
        commentParams.put("status", status);

        params.add(site.getSelfHostedSiteId());
        params.add(site.getUsername());
        params.add(site.getPassword());
        params.add(comment.getRemoteCommentId());
        params.add(commentParams);
        final XMLRPCRequest request = new XMLRPCRequest(
                site.getXmlRpcUrl(), XMLRPC.EDIT_COMMENT, params,
                (Listener<Object>) response -> {
                    RemoteCommentResponsePayload payload = new RemoteCommentResponsePayload(comment);
                    mDispatcher.dispatch(CommentActionBuilder.newPushedCommentAction(payload));
                },
                error -> mDispatcher.dispatch(CommentActionBuilder.newPushedCommentAction(
                        CommentErrorUtils.commentErrorToPushCommentPayload(error, comment))));
        add(request);
    }

    public void fetchComment(
            @NonNull final SiteModel site,
            long remoteCommentId,
            @Nullable final CommentModel comment) {
        List<Object> params = new ArrayList<>(4);
        params.add(site.getSelfHostedSiteId());
        params.add(site.getUsername());
        params.add(site.getPassword());
        params.add(remoteCommentId);
        final XMLRPCRequest request = new XMLRPCRequest(
                site.getXmlRpcUrl(), XMLRPC.GET_COMMENT, params,
                (Listener<Object>) response -> {
                    CommentModel updatedComment = commentResponseToComment(response, site);
                    RemoteCommentResponsePayload payload = new RemoteCommentResponsePayload(updatedComment);
                    mDispatcher.dispatch(CommentActionBuilder.newFetchedCommentAction(payload));
                },
                error -> mDispatcher.dispatch(CommentActionBuilder.newFetchedCommentAction(
                        CommentErrorUtils.commentErrorToFetchCommentPayload(error, comment))));
        add(request);
    }

    public void deleteComment(
            @NonNull final SiteModel site,
            long remoteCommentId,
            @Nullable final CommentModel comment) {
        List<Object> params = new ArrayList<>(4);
        params.add(site.getSelfHostedSiteId());
        params.add(site.getUsername());
        params.add(site.getPassword());
        params.add(remoteCommentId);
        final XMLRPCRequest request = new XMLRPCRequest(
                site.getXmlRpcUrl(), XMLRPC.DELETE_COMMENT, params,
                (Listener<Object>) response -> {
                    RemoteCommentResponsePayload payload = new RemoteCommentResponsePayload(comment);
                    if (comment != null) {
                        // This is ugly but the XMLRPC response doesn't contain any info about the update comment.
                        // So we're copying the logic here: if the comment status was "trash" before and the delete
                        // call is successful, then we want to delete this comment. Setting the "deleted" status
                        // will ensure the comment is deleted in the CommentStore.
                        if (CommentStatus.TRASH.toString().equals(comment.getStatus())) {
                            comment.setStatus(CommentStatus.DELETED.toString());
                        } else {
                            comment.setStatus(CommentStatus.TRASH.toString());
                        }
                    }
                    mDispatcher.dispatch(CommentActionBuilder.newDeletedCommentAction(payload));
                },
                error -> mDispatcher.dispatch(CommentActionBuilder.newDeletedCommentAction(
                        CommentErrorUtils.commentErrorToFetchCommentPayload(error, comment))));
        add(request);
    }

    /**
     * Create a new reply to a Comment
     */
    public void createNewReply(
            @NonNull final SiteModel site,
            @NonNull final CommentModel comment,
            @NonNull final CommentModel reply) {
        // Comment parameters
        Map<String, Object> replyParams = new HashMap<>(5);

        // Reply parameters
        replyParams.put("content", reply.getContent());

        // Use remote comment id as reply comment parent
        replyParams.put("comment_parent", comment.getRemoteCommentId());

        if (reply.getAuthorName() != null) {
            replyParams.put("author", reply.getAuthorName());
        }
        if (reply.getAuthorUrl() != null) {
            replyParams.put("author_url", reply.getAuthorUrl());
        }
        if (reply.getAuthorEmail() != null) {
            replyParams.put("author_email", reply.getAuthorEmail());
        }

        newComment(site, comment.getRemotePostId(), reply, comment.getRemoteCommentId(), replyParams);
    }

    /**
     * Create a new comment to a Post
     */
    public void createNewComment(
            @NonNull final SiteModel site,
            @NonNull final PostModel post,
            @NonNull final CommentModel comment) {
        // Comment parameters
        Map<String, Object> commentParams = new HashMap<>(5);
        commentParams.put("content", comment.getContent());
        if (comment.getParentId() != 0) {
            commentParams.put("comment_parent", comment.getParentId());
        }
        if (comment.getAuthorName() != null) {
            commentParams.put("author", comment.getAuthorName());
        }
        if (comment.getAuthorUrl() != null) {
            commentParams.put("author_url", comment.getAuthorUrl());
        }
        if (comment.getAuthorEmail() != null) {
            commentParams.put("author_email", comment.getAuthorEmail());
        }
        newComment(site, post.getRemotePostId(), comment, comment.getParentId(), commentParams);
    }

    // Private methods

    private void newComment(
            @NonNull final SiteModel site,
            long remotePostId,
            @NonNull final CommentModel comment,
            final long parentId,
            @NonNull Map<String, Object> commentParams) {
        List<Object> params = new ArrayList<>(5);
        params.add(site.getSelfHostedSiteId());
        params.add(site.getUsername());
        params.add(site.getPassword());
        params.add(remotePostId);
        params.add(commentParams);
        final XMLRPCRequest request = new XMLRPCRequest(
                site.getXmlRpcUrl(), XMLRPC.NEW_COMMENT, params,
                (Listener<Object>) response -> {
                    RemoteCommentResponsePayload payload = new RemoteCommentResponsePayload(comment);
                    comment.setParentId(parentId);
                    if (response instanceof Integer) {
                        comment.setRemoteCommentId((int) response);
                    } else {
                        payload.error = new CommentError(CommentErrorType.GENERIC_ERROR, "");
                    }
                    mDispatcher.dispatch(CommentActionBuilder.newCreatedNewCommentAction(payload));
                },
                error -> mDispatcher.dispatch(CommentActionBuilder.newCreatedNewCommentAction(
                        CommentErrorUtils.commentErrorToFetchCommentPayload(error, comment))));
        add(request);
    }

    @NonNull
    private String getXMLRPCCommentStatus(@NonNull CommentStatus status) {
        switch (status) {
            case APPROVED:
                return "approve";
            case UNAPPROVED:
                return "hold";
            case SPAM:
                return "spam";
            case TRASH:
                return "trash";
            // Defaults (don't exist in XMLRPC)
            default:
            case DELETED:
            case ALL:
            case UNSPAM:
            case UNTRASH:
            case UNREPLIED:
                return "approve";
        }
    }

    @NonNull
    @SuppressWarnings("DuplicateBranchesInSwitch")
    private CommentStatus getCommentStatusFromXMLRPCStatusString(@NonNull String stringStatus) {
        switch (stringStatus) {
            case "approve":
                return CommentStatus.APPROVED;
            case "hold":
                return CommentStatus.UNAPPROVED;
            case "spam":
                return CommentStatus.SPAM;
            case "trash":
                return CommentStatus.TRASH;
            default: // Defaults (don't exist in XMLRPC)
                return CommentStatus.APPROVED;
        }
    }

    @NonNull
    private List<CommentModel> commentsResponseToCommentList(
            @NonNull Object response,
            @NonNull SiteModel site) {
        List<CommentModel> comments = new ArrayList<>();
        if (!(response instanceof Object[])) {
            return comments;
        }
        Object[] responseArray = (Object[]) response;
        for (Object commentObject : responseArray) {
            CommentModel commentModel = commentResponseToComment(commentObject, site);
            if (commentModel != null) {
                comments.add(commentModel);
            }
        }
        return comments;
    }

    @Nullable
    private CommentModel commentResponseToComment(
            @NonNull Object commentObject,
            @NonNull SiteModel site) {
        if (!(commentObject instanceof HashMap)) {
            return null;
        }
        HashMap<?, ?> commentMap = (HashMap<?, ?>) commentObject;
        CommentModel comment = new CommentModel();

        comment.setRemoteCommentId(XMLRPCUtils.safeGetMapValue(commentMap, "comment_id", 0L));
        comment.setLocalSiteId(site.getId());
        comment.setRemoteSiteId(site.getSelfHostedSiteId());
        String stringStatus = XMLRPCUtils.safeGetMapValue(commentMap, "status", "approve");
        comment.setStatus(getCommentStatusFromXMLRPCStatusString(stringStatus).toString());
        Date datePublished = XMLRPCUtils.safeGetMapValue(commentMap, "date_created_gmt", new Date());
        comment.setDatePublished(DateTimeUtils.iso8601UTCFromDate(datePublished));
        comment.setPublishedTimestamp(DateTimeUtils.timestampFromIso8601(comment.getDatePublished()));
        comment.setContent(XMLRPCUtils.safeGetMapValue(commentMap, "content", ""));
        comment.setUrl(XMLRPCUtils.safeGetMapValue(commentMap, "link", ""));

        // Parent
        comment.setParentId(XMLRPCUtils.safeGetMapValue(commentMap, "parent", 0L));
        if (comment.getParentId() > 0) {
            comment.setParentId(comment.getParentId());
            comment.setHasParent(true);
        } else {
            comment.setHasParent(false);
        }

        // Author
        comment.setAuthorUrl(XMLRPCUtils.safeGetMapValue(commentMap, "author_url", ""));
        comment.setAuthorName(StringEscapeUtils.unescapeHtml4(XMLRPCUtils.safeGetMapValue(commentMap, "author", "")));
        comment.setAuthorEmail(XMLRPCUtils.safeGetMapValue(commentMap, "author_email", ""));
        // TODO: comment.setAuthorProfileImageUrl(); - get the hash from the email address?

        // Post
        comment.setRemotePostId(XMLRPCUtils.safeGetMapValue(commentMap, "post_id", 0L));
        comment.setPostTitle(StringEscapeUtils.unescapeHtml4(XMLRPCUtils.safeGetMapValue(commentMap,
                "post_title", "")));

        return comment;
    }
}
