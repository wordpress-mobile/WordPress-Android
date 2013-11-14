package org.wordpress.android.ui.comments;

import android.os.Handler;
import android.text.TextUtils;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONObject;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.Comment;
import org.wordpress.android.models.CommentStatus;
import org.wordpress.android.util.JSONUtil;
import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;

/**
 * Created by nbradbury on 11/8/13.
 * actions related to comments - replies, etc.
 * methods below do network calls in the background & update local DB upon success
 * all methods below MUST be called from UI thread
 */

public class CommentActions {

    private CommentActions() {
        throw new AssertionError();
    }

    /*
     * result when a comment action is performed
     */
    protected interface CommentActionListener {
        public void onActionResult(boolean succeeded);
    }

    /**
     * reply to an individual comment
     */
    protected static void submitReplyToComment(final Blog blog,
                                               final Comment comment,
                                               final String replyText,
                                               final CommentActionListener actionListener) {

        if (blog==null || comment==null || TextUtils.isEmpty(replyText)) {
            if (actionListener != null)
                actionListener.onActionResult(false);
            return;
        }

        new Thread() {
            @Override
            public void run() {
                RestRequest.Listener restListener = new RestRequest.Listener() {
                    @Override
                    public void onResponse(JSONObject jsonObject) {
                        // TODO: the response contains the new comment, not just its ID, so parse
                        // it and store the comment in the comment table
                        int newCommentId = (jsonObject != null ? jsonObject.optInt("ID") : 0);
                        boolean succeeded = (newCommentId != 0);
                        // TODO: previous version updated latestCommentID but not convinced this is still necessary
                        if (succeeded)
                            WordPress.wpDB.updateLatestCommentID(blog.getId(), newCommentId);
                        if (actionListener != null)
                            actionListener.onActionResult(succeeded);
                    }
                };
                RestRequest.ErrorListener restErrListener = new RestRequest.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        if (actionListener != null)
                            actionListener.onActionResult(false);
                    }
                };

                String siteId = Integer.toString(blog.getBlogId());
                String commentId = Integer.toString(comment.commentID);
                WordPress.restClient.replyToComment(siteId, commentId, replyText, restListener, restErrListener);

                /* Pre-v2.6 XMLRPC code commented out below
                XMLRPCClient client = new XMLRPCClient(blog.getUrl(),
                        blog.getHttpuser(),
                        blog.getHttppassword());

                Map<String, Object> replyHash = new HashMap<String, Object>();
                replyHash.put("comment_parent", comment.commentID);
                replyHash.put("content", replyText);
                replyHash.put("author", "");
                replyHash.put("author_url", "");
                replyHash.put("author_email", "");

                Object[] params = { blog.getBlogId(),
                        blog.getUsername(),
                        blog.getPassword(),
                        Integer.valueOf(comment.postID),
                        replyHash };


                int newCommentID;
                try {
                    newCommentID = (Integer) client.call("wp.newComment", params);
                } catch (XMLRPCException e) {
                    newCommentID = -1;
                }

                final boolean succeeded = (newCommentID >= 0);
                if (succeeded)
                    WordPress.wpDB.updateLatestCommentID(blog.getId(), newCommentID);

                if (actionListener != null) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            actionListener.onActionResult(succeeded);
                        }
                    });
                }*/
            }
        }.start();
    }

    /**
     * change the status of a comment
     */
    protected static void setCommentStatus(final Blog blog,
                                           final Comment comment,
                                           final CommentStatus status,
                                           final CommentActionListener actionListener) {

        if (blog==null || comment==null || status==null || status== CommentStatus.UNKNOWN) {
            if (actionListener != null)
                actionListener.onActionResult(false);
            return;
        }

        new Thread() {
            @Override
            public void run() {
                RestRequest.Listener restListener = new RestRequest.Listener() {
                    @Override
                    public void onResponse(JSONObject jsonObject) {
                        String newStatus = (jsonObject != null ? JSONUtil.getString(jsonObject, "status") : null);
                        boolean successful = !TextUtils.isEmpty(newStatus);
                        if (successful)
                            WordPress.wpDB.updateCommentStatus(blog.getId(), comment.commentID, newStatus);
                        if (actionListener != null)
                            actionListener.onActionResult(successful);
                    }
                };
                RestRequest.ErrorListener restErrListener = new RestRequest.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        if (actionListener != null)
                            actionListener.onActionResult(false);
                    }
                };

                String siteId = Integer.toString(blog.getBlogId());
                String commentId = Integer.toString(comment.commentID);
                WordPress.restClient.moderateComment(siteId, commentId, CommentStatus.toString(status, CommentStatus.ApiFormat.REST), restListener, restErrListener);

                /* Pre-v2.6 XMLRPC code commented out below
                XMLRPCClient client = new XMLRPCClient(blog.getUrl(),
                    blog.getHttpuser(),
                    blog.getHttppassword());

                Map<String, String> postHash = new HashMap<String, String>();
                postHash.put("status", status.toString());
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
                    result = null;
                }

                final boolean success = (result != null && Boolean.parseBoolean(result.toString()));
                if (success)
                    WordPress.wpDB.updateCommentStatus(blog.getId(), comment.commentID, status.toString());

                if (actionListener != null) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            actionListener.onActionResult(success);
                        }
                    });
                } */
            }
        }.start();
    }

    /**
     * delete (trash) a single comment
     */
    protected static void deleteComment(final Blog blog,
                                        final Comment comment,
                                        final CommentActionListener actionListener) {
        if (blog==null || comment==null) {
            if (actionListener != null)
                actionListener.onActionResult(false);
            return;
        }

        final Handler handler = new Handler();

        new Thread() {
            @Override
            public void run() {
                XMLRPCClient client = new XMLRPCClient(blog.getUrl(),
                        blog.getHttpuser(),
                        blog.getHttppassword());

                Object[] params = { blog.getBlogId(),
                        blog.getUsername(),
                        blog.getPassword(),
                        comment.commentID };

                Object result;
                try {
                    result = client.call("wp.deleteComment", params);
                } catch (final XMLRPCException e) {
                    result = null;
                }

                final boolean success = (result != null && Boolean.parseBoolean(result.toString()));
                if (success)
                    WordPress.wpDB.deleteComment(blog.getId(), comment.postID, comment.commentID);

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
