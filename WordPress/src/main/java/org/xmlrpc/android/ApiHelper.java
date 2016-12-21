package org.xmlrpc.android;

import android.support.annotation.Nullable;

import org.wordpress.android.models.Comment;
import org.wordpress.android.models.CommentList;
import org.wordpress.android.models.CommentStatus;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.StringUtils;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApiHelper {

    public static final class Method {
        public static final String GET_POST_FORMATS   = "wp.getPostFormats";
        public static final String GET_CATEGORIES     = "wp.getCategories";
        public static final String GET_MEDIA_ITEM     = "wp.getMediaItem";
        public static final String GET_COMMENT        = "wp.getComment";
        public static final String GET_COMMENTS       = "wp.getComments";
        public static final String GET_OPTIONS        = "wp.getOptions";
        public static final String GET_TERM           = "wp.getTerm";

        public static final String DELETE_COMMENT     = "wp.deleteComment";

        public static final String NEW_CATEGORY       = "wp.newCategory";
        public static final String NEW_COMMENT        = "wp.newComment";

        public static final String EDIT_COMMENT       = "wp.editComment";

        public static final String SET_OPTIONS        = "wp.setOptions";

        public static final String UPLOAD_FILE        = "wp.uploadFile";
    }

    public static final class Param {
        public static final String SHOW_SUPPORTED_POST_FORMATS = "show-supported";
    }

    public enum ErrorType {
        NO_ERROR, UNKNOWN_ERROR, INVALID_CURRENT_BLOG, NETWORK_XMLRPC, INVALID_CONTEXT,
        INVALID_RESULT, NO_UPLOAD_FILES_CAP, CAST_EXCEPTION, TASK_CANCELLED, UNAUTHORIZED
    }

    public interface GenericErrorCallback {
        void onFailure(ErrorType errorType, String errorMessage, Throwable throwable);
    }

    public interface GenericCallback extends GenericErrorCallback {
        void onSuccess();
    }

    public interface DatabasePersistCallback {
        void onDataReadyToSave(List list);
    }

    public static CommentList refreshComments(SiteModel site, Object[] commentParams,
                                              DatabasePersistCallback dbCallback)
            throws XMLRPCException, IOException, XmlPullParserException {
        XMLRPCClientInterface client = XMLRPCFactory.instantiate(URI.create(site.getXmlRpcUrl()), "", "");
        Object[] result;
        result = (Object[]) client.call(Method.GET_COMMENTS, commentParams);

        if (result.length == 0) {
            return null;
        }

        Map<?, ?> contentHash;
        long commentID, postID;
        String authorName, content, status, authorEmail, authorURL, postTitle, pubDate;
        java.util.Date date;
        CommentList comments = new CommentList();

        for (int ctr = 0; ctr < result.length; ctr++) {
            contentHash = (Map<?, ?>) result[ctr];
            content = contentHash.get("content").toString();
            status = contentHash.get("status").toString();
            postID = Long.parseLong(contentHash.get("post_id").toString());
            commentID = Long.parseLong(contentHash.get("comment_id").toString());
            authorName = contentHash.get("author").toString();
            authorURL = contentHash.get("author_url").toString();
            authorEmail = contentHash.get("author_email").toString();
            postTitle = contentHash.get("post_title").toString();
            date = (java.util.Date) contentHash.get("date_created_gmt");
            pubDate = DateTimeUtils.iso8601FromDate(date);

            Comment comment = new Comment(
                    postID,
                    commentID,
                    authorName,
                    pubDate,
                    content,
                    status,
                    postTitle,
                    authorURL,
                    authorEmail,
                    null);

            comments.add(comment);
        }

        if (dbCallback != null){
            dbCallback.onDataReadyToSave(comments);
        }

        return comments;
    }

    public static boolean editComment(SiteModel site, Comment comment, CommentStatus newStatus) {
        XMLRPCClientInterface client = XMLRPCFactory.instantiate(URI.create(site.getXmlRpcUrl()), "", "");

        Map<String, String> postHash = new HashMap<>();
        postHash.put("status", CommentStatus.toString(newStatus));
        postHash.put("content", comment.getCommentText());
        postHash.put("author", comment.getAuthorName());
        postHash.put("author_url", comment.getAuthorUrl());
        postHash.put("author_email", comment.getAuthorEmail());

        Object[] params = {
                String.valueOf(site.getSiteId()),
                StringUtils.notNullStr(site.getUsername()),
                StringUtils.notNullStr(site.getPassword()),
                Long.toString(comment.commentID),
                postHash
        };

        try {
            Object result = client.call(Method.EDIT_COMMENT, params);
            return (result != null && Boolean.parseBoolean(result.toString()));
        } catch (XMLRPCFault xmlrpcFault) {
            if (xmlrpcFault.getFaultCode() == 500) {
                // let's check whether the comment is already marked as _newStatus_
                CommentStatus remoteStatus = getCommentStatus(site, comment);
                if (remoteStatus != null && remoteStatus.equals(newStatus)) {
                    // Happy days! Remote is already marked as the desired status
                    return true;
                }
            }
            AppLog.e(T.COMMENTS, "Error while editing comment", xmlrpcFault);
        } catch (XMLRPCException e) {
            AppLog.e(T.COMMENTS, "Error while editing comment", e);
        } catch (IOException e) {
            AppLog.e(T.COMMENTS, "Error while editing comment", e);
        } catch (XmlPullParserException e) {
            AppLog.e(T.COMMENTS, "Error while editing comment", e);
        }

        return false;
    }

    /**
     * Fetches the status of a comment
     * @param comment the comment to fetch its status
     * @return the status of the comment on the server, null if error
     */
    public static @Nullable CommentStatus getCommentStatus(SiteModel site, Comment comment) {
        if (site == null || comment == null) {
            return null;
        }

        XMLRPCClientInterface client = XMLRPCFactory.instantiate(URI.create(site.getXmlRpcUrl()), "", "");

        Object[] params = {
                String.valueOf(site.getSiteId()),
                StringUtils.notNullStr(site.getUsername()),
                StringUtils.notNullStr(site.getPassword()),
                Long.toString(comment.commentID)
        };

        try {
            Map<?, ?> contentHash = (Map<?, ?>) client.call(Method.GET_COMMENT, params);
            final Object status = contentHash.get("status");
            return status == null ? null : CommentStatus.fromString(status.toString());
        } catch (XMLRPCException e) {
            AppLog.e(T.COMMENTS, "Error while getting comment", e);
        } catch (IOException e) {
            AppLog.e(T.COMMENTS, "Error while getting comment", e);
        } catch (XmlPullParserException e) {
            AppLog.e(T.COMMENTS, "Error while getting comment", e);
        }

        return null;
    }
}
