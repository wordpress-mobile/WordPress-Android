package org.wordpress.android.ui.comments;

import android.os.Handler;
import android.text.TextUtils;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONObject;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.CommentTable;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.Comment;
import org.wordpress.android.models.CommentList;
import org.wordpress.android.models.CommentStatus;
import org.wordpress.android.models.Note;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by nbradbury on 11/8/13.
 * actions related to comments - replies, moderating, etc.
 * methods below do network calls in the background & update local DB upon success
 * all methods below MUST be called from UI thread
 */

public class CommentActions {

    private CommentActions() {
        throw new AssertionError();
    }

    /*
     * listener when a comment action is performed
     */
    public interface CommentActionListener {
        public void onActionResult(boolean succeeded);
    }

    /*
     * listener when comments are moderated or deleted
     */
    public interface OnCommentsModeratedListener {
        public void onCommentsModerated(final CommentList moderatedComments);
    }

    /*
     * used by comment fragments to alert container activity of a change to one or more
     * comments (moderated, deleted, added, etc.)
     */
    public static enum ChangedFrom {COMMENT_LIST, COMMENT_DETAIL}
    public static interface OnCommentChangeListener {
        public void onCommentChanged(ChangedFrom changedFrom);
    }


    /*
     * add a comment for the passed post
     */
    public static void addComment(final int accountId,
                                  final String postID,
                                  final String commentText,
                                  final CommentActionListener actionListener) {
        final Blog blog = WordPress.getBlog(accountId);
        if (blog==null || TextUtils.isEmpty(commentText)) {
            if (actionListener != null)
                actionListener.onActionResult(false);
            return;
        }

        final Handler handler = new Handler();

        new Thread() {
            @Override
            public void run() {
                XMLRPCClient client = new XMLRPCClient(
                        blog.getUrl(),
                        blog.getHttpuser(),
                        blog.getHttppassword());

                Map<String, Object> commentHash = new HashMap<String, Object>();
                commentHash.put("content", commentText);
                commentHash.put("author", "");
                commentHash.put("author_url", "");
                commentHash.put("author_email", "");

                Object[] params = {
                        blog.getRemoteBlogId(),
                        blog.getUsername(),
                        blog.getPassword(),
                        postID,
                        commentHash};

                int newCommentID;
                try {
                    newCommentID = (Integer) client.call("wp.newComment", params);
                } catch (XMLRPCException e) {
                    AppLog.e(T.COMMENTS, e.getMessage(), e);
                    newCommentID = -1;
                }

                final boolean succeeded = (newCommentID >= 0);
                if (succeeded)
                    WordPress.wpDB.updateLatestCommentID(accountId, newCommentID);

                if (actionListener != null) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            actionListener.onActionResult(succeeded);
                        }
                    });
                }
            }
        }.start();
    }

    /**
     * reply to an individual comment
     */
    protected static void submitReplyToComment(final int accountId,
                                               final Comment comment,
                                               final String replyText,
                                               final CommentActionListener actionListener) {

        final Blog blog = WordPress.getBlog(accountId);
        if (blog==null || comment==null || TextUtils.isEmpty(replyText)) {
            if (actionListener != null)
                actionListener.onActionResult(false);
            return;
        }

        final Handler handler = new Handler();

        new Thread() {
            @Override
            public void run() {
                XMLRPCClient client = new XMLRPCClient(
                        blog.getUrl(),
                        blog.getHttpuser(),
                        blog.getHttppassword());

                Map<String, Object> replyHash = new HashMap<String, Object>();
                replyHash.put("comment_parent", comment.commentID);
                replyHash.put("content", replyText);
                replyHash.put("author", "");
                replyHash.put("author_url", "");
                replyHash.put("author_email", "");

                Object[] params = {
                        blog.getRemoteBlogId(),
                        blog.getUsername(),
                        blog.getPassword(),
                        Integer.valueOf(comment.postID),
                        replyHash };


                int newCommentID;
                try {
                    newCommentID = (Integer) client.call("wp.newComment", params);
                } catch (XMLRPCException e) {
                    AppLog.e(T.COMMENTS, e.getMessage(), e);
                    newCommentID = -1;
                }

                final boolean succeeded = (newCommentID >= 0);
                if (succeeded)
                    WordPress.wpDB.updateLatestCommentID(accountId, newCommentID);

                if (actionListener != null) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            actionListener.onActionResult(succeeded);
                        }
                    });
                }
            }
        }.start();
    }

    /**
     * reply to an individual comment that came from a notification - this differs from
     * submitReplyToComment() in that it enables responding to a reply to a comment this
     * user made on someone else's blog
     */
    protected static void submitReplyToCommentNote(final Note note,
                                                   final String replyText,
                                                   final CommentActionListener actionListener) {
        if (note == null || TextUtils.isEmpty(replyText)) {
            if (actionListener != null)
                actionListener.onActionResult(false);
            return;
        }

        RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                if (actionListener != null)
                    actionListener.onActionResult(true);
            }
        };
        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                if (volleyError != null)
                    AppLog.e(T.COMMENTS, volleyError.getMessage(), volleyError);
                if (actionListener != null)
                    actionListener.onActionResult(false);
            }
        };

        Note.Reply reply = note.buildReply(replyText);
        WordPress.restClient.replyToComment(reply, listener, errorListener);
    }

    /**
     * change the status of a single comment
     */
    protected static void moderateComment(final int accountId,
                                          final Comment comment,
                                          final CommentStatus newStatus,
                                          final CommentActionListener actionListener) {

        // deletion is handled separately
        if (newStatus != null && newStatus.equals(CommentStatus.TRASH)) {
            deleteComment(accountId, comment, actionListener);
            return;
        }

        final Blog blog = WordPress.getBlog(accountId);

        if (blog==null || comment==null || newStatus==null || newStatus==CommentStatus.UNKNOWN) {
            if (actionListener != null)
                actionListener.onActionResult(false);
            return;
        }

        final Handler handler = new Handler();

        new Thread() {
            @Override
            public void run() {
                XMLRPCClient client = new XMLRPCClient(
                    blog.getUrl(),
                    blog.getHttpuser(),
                    blog.getHttppassword());

                Map<String, String> postHash = new HashMap<String, String>();
                postHash.put("status", CommentStatus.toString(newStatus));
                postHash.put("content", comment.getCommentText());
                postHash.put("author", comment.getAuthorName());
                postHash.put("author_url", comment.getAuthorUrl());
                postHash.put("author_email", comment.getAuthorEmail());

                Object[] params = { blog.getRemoteBlogId(),
                        blog.getUsername(),
                        blog.getPassword(),
                        comment.commentID,
                        postHash};

                Object result;
                try {
                    result = client.call("wp.editComment", params);
                } catch (final XMLRPCException e) {
                    AppLog.e(T.COMMENTS, e.getMessage(), e);
                    result = null;
                }

                final boolean success = (result != null && Boolean.parseBoolean(result.toString()));
                if (success)
                    CommentTable.updateCommentStatus(blog.getLocalTableBlogId(), comment.commentID, CommentStatus.toString(newStatus));

                if (actionListener != null) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            actionListener.onActionResult(success);
                        }
                    });
                }
            }
        }.start();
    }

    /**
     * change the status of multiple comments
     * TODO: investigate using system.multiCall to perform a single call to moderate the list
     */
    protected static void moderateComments(final int accountId,
                                           final CommentList comments,
                                           final CommentStatus newStatus,
                                           final OnCommentsModeratedListener actionListener) {

        // deletion is handled separately
        if (newStatus != null && newStatus.equals(CommentStatus.TRASH)) {
            deleteComments(accountId, comments, actionListener);
            return;
        }

        final Blog blog = WordPress.getBlog(accountId);

        if (blog==null || comments==null || comments.size() == 0 || newStatus==null || newStatus==CommentStatus.UNKNOWN) {
            if (actionListener != null)
                actionListener.onCommentsModerated(new CommentList());
            return;
        }

        final CommentList moderatedComments = new CommentList();
        final String newStatusStr = CommentStatus.toString(newStatus);
        final int localBlogId = blog.getLocalTableBlogId();
        final int remoteBlogId = blog.getRemoteBlogId();

        final Handler handler = new Handler();
        new Thread() {
            @Override
            public void run() {
                XMLRPCClient client = new XMLRPCClient(
                        blog.getUrl(),
                        blog.getHttpuser(),
                        blog.getHttppassword());

                for (Comment comment: comments) {
                    Map<String, String> postHash = new HashMap<String, String>();
                    postHash.put("status", newStatusStr);
                    postHash.put("content", comment.getCommentText());
                    postHash.put("author", comment.getAuthorName());
                    postHash.put("author_url", comment.getAuthorUrl());
                    postHash.put("author_email", comment.getAuthorEmail());

                    Object[] params = {
                            remoteBlogId,
                            blog.getUsername(),
                            blog.getPassword(),
                            comment.commentID,
                            postHash};

                    Object result;
                    try {
                        result = client.call("wp.editComment", params);
                        boolean success = (result != null && Boolean.parseBoolean(result.toString()));
                        if (success) {
                            comment.setStatus(newStatusStr);
                            moderatedComments.add(comment);
                        }
                    } catch (final XMLRPCException e) {
                        AppLog.e(T.COMMENTS, e.getMessage(), e);
                    }
                }

                // update status in SQLite of successfully moderated comments
                CommentTable.updateCommentsStatus(localBlogId, moderatedComments, newStatusStr);

                if (actionListener != null) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            actionListener.onCommentsModerated(moderatedComments);
                        }
                    });
                }
            }
        }.start();
    }

    /**
     * delete (trash) a single comment
     */
    private static void deleteComment(final int accountId,
                                        final Comment comment,
                                        final CommentActionListener actionListener) {
        final Blog blog = WordPress.getBlog(accountId);
        if (blog==null || comment==null) {
            if (actionListener != null)
                actionListener.onActionResult(false);
            return;
        }

        final Handler handler = new Handler();

        new Thread() {
            @Override
            public void run() {
                XMLRPCClient client = new XMLRPCClient(
                        blog.getUrl(),
                        blog.getHttpuser(),
                        blog.getHttppassword());

                Object[] params = {
                        blog.getRemoteBlogId(),
                        blog.getUsername(),
                        blog.getPassword(),
                        comment.commentID };

                Object result;
                try {
                    result = client.call("wp.deleteComment", params);
                } catch (final XMLRPCException e) {
                    AppLog.e(T.COMMENTS, e.getMessage(), e);
                    result = null;
                }

                final boolean success = (result != null && Boolean.parseBoolean(result.toString()));
                if (success)
                    CommentTable.deleteComment(accountId, comment.commentID);

                if (actionListener != null) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            actionListener.onActionResult(success);
                        }
                    });
                }
            }
        }.start();
    }

    /**
     * delete multiple comments
     */
    private static void deleteComments(final int accountId,
                                       final CommentList comments,
                                       final OnCommentsModeratedListener actionListener) {

        final Blog blog = WordPress.getBlog(accountId);

        if (blog==null || comments==null || comments.size() == 0) {
            if (actionListener != null)
                actionListener.onCommentsModerated(new CommentList());
            return;
        }

        final CommentList deletedComments = new CommentList();
        final int localBlogId = blog.getLocalTableBlogId();
        final int remoteBlogId = blog.getRemoteBlogId();

        final Handler handler = new Handler();
        new Thread() {
            @Override
            public void run() {
                XMLRPCClient client = new XMLRPCClient(
                        blog.getUrl(),
                        blog.getHttpuser(),
                        blog.getHttppassword());

                for (Comment comment: comments) {
                    Object[] params = {
                            remoteBlogId,
                            blog.getUsername(),
                            blog.getPassword(),
                            comment.commentID};

                    Object result;
                    try {
                        result = client.call("wp.deleteComment", params);
                        boolean success = (result != null && Boolean.parseBoolean(result.toString()));
                        if (success)
                            deletedComments.add(comment);
                    } catch (final XMLRPCException e) {
                        AppLog.e(T.COMMENTS, e.getMessage(), e);
                    }
                }

                // remove successfully deleted comments from SQLite
                CommentTable.deleteComments(localBlogId, deletedComments);

                if (actionListener != null) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            actionListener.onCommentsModerated(deletedComments);
                        }
                    });
                }
            }
        }.start();
    }
}
