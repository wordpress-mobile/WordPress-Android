package org.wordpress.android.ui.comments;

import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONObject;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.Comment;
import org.wordpress.android.models.CommentStatus;
import org.wordpress.android.models.Note;
import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;

import java.util.HashMap;
import java.util.List;
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

    public interface OnCommentChangeListener {
        public void onCommentModerated(final Comment comment, final Note note);
        public void onCommentsModerated(final List<Comment> comments);
        public void onCommentAdded();
        public void onCommentDeleted();
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
                        blog.getBlogId(),
                        blog.getUsername(),
                        blog.getPassword(),
                        postID,
                        commentHash};

                int newCommentID;
                try {
                    newCommentID = (Integer) client.call("wp.newComment", params);
                } catch (XMLRPCException e) {
                    Log.e(WordPress.TAG, e.getMessage(), e);
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
                        blog.getBlogId(),
                        blog.getUsername(),
                        blog.getPassword(),
                        Integer.valueOf(comment.postID),
                        replyHash };


                int newCommentID;
                try {
                    newCommentID = (Integer) client.call("wp.newComment", params);
                } catch (XMLRPCException e) {
                    Log.e(WordPress.TAG, e.getMessage(), e);
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
                    Log.e(WordPress.TAG, volleyError.getMessage(), volleyError);
                if (actionListener != null)
                    actionListener.onActionResult(false);
            }
        };

        Note.Reply reply = note.buildReply(replyText);
        WordPress.restClient.replyToComment(reply, listener, errorListener);
    }

    /**
     * change the status of a comment
     */
    protected static void moderateComment(final int accountId,
                                          final Comment comment,
                                          final CommentStatus newStatus,
                                          final CommentActionListener actionListener) {

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
                postHash.put("content", comment.comment);
                postHash.put("author", comment.name);
                postHash.put("author_url", comment.authorURL);
                postHash.put("author_email", comment.authorEmail);

                Object[] params = { blog.getBlogId(),
                        blog.getUsername(),
                        blog.getPassword(),
                        comment.commentID,
                        postHash};

                Object result;
                try {
                    result = client.call("wp.editComment", params);
                } catch (final XMLRPCException e) {
                    Log.e(WordPress.TAG, e.getMessage(), e);
                    result = null;
                }

                final boolean success = (result != null && Boolean.parseBoolean(result.toString()));
                if (success)
                    WordPress.wpDB.updateCommentStatus(blog.getId(), comment.commentID, CommentStatus.toString(newStatus));

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
     * delete (trash) a single comment
     */
    protected static void deleteComment(final int accountId,
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
                        blog.getBlogId(),
                        blog.getUsername(),
                        blog.getPassword(),
                        comment.commentID };

                Object result;
                try {
                    result = client.call("wp.deleteComment", params);
                } catch (final XMLRPCException e) {
                    Log.e(WordPress.TAG, e.getMessage(), e);
                    result = null;
                }

                final boolean success = (result != null && Boolean.parseBoolean(result.toString()));
                if (success) {
                    WordPress.wpDB.deleteComment(accountId, comment.commentID);
                }

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
}
