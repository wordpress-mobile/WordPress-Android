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
import org.wordpress.android.util.VolleyUtils;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlrpc.android.ApiHelper;
import org.xmlrpc.android.ApiHelper.Method;
import org.xmlrpc.android.XMLRPCClientInterface;
import org.xmlrpc.android.XMLRPCException;
import org.xmlrpc.android.XMLRPCFactory;
import org.xmlrpc.android.XMLRPCFault;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
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
        void onActionResult(CommentActionResult result);
    }

    /*
     * listener when comments are moderated or deleted
     */
    public interface OnCommentsModeratedListener {
        void onCommentsModerated(final CommentList moderatedComments);
    }

    /*
     * used by comment fragments to alert container activity of a change to one or more
     * comments (moderated, deleted, added, etc.)
     */
    public enum ChangeType {EDITED, REPLIED}
    public interface OnCommentChangeListener {
        void onCommentChanged(ChangeType changeType);
    }

    public interface OnCommentActionListener {
        void onModerateComment(int accountId, Comment comment, CommentStatus newStatus);
    }

    public interface OnNoteCommentActionListener {
        void onModerateCommentForNote(Note note, CommentStatus newStatus);
    }


    /**
     * reply to an individual comment
     */
    static void submitReplyToComment(final int accountId,
                                     final Comment comment,
                                     final String replyText,
                                     final CommentActionListener actionListener) {
        final Blog blog = WordPress.getBlog(accountId);
        if (blog==null || comment==null || TextUtils.isEmpty(replyText)) {
            if (actionListener != null) {
                actionListener.onActionResult(new CommentActionResult(CommentActionResult.COMMENT_ID_ON_ERRORS, null));
            }
            return;
        }

        final Handler handler = new Handler();

        new Thread() {
            @Override
            public void run() {
                XMLRPCClientInterface client = XMLRPCFactory.instantiate(blog.getUri(), blog.getHttpuser(),
                        blog.getHttppassword());

                Map<String, Object> replyHash = new HashMap<>();
                replyHash.put("comment_parent", Long.toString(comment.commentID));
                replyHash.put("content", replyText);
                replyHash.put("author", "");
                replyHash.put("author_url", "");
                replyHash.put("author_email", "");

                Object[] params = {
                        blog.getRemoteBlogId(),
                        blog.getUsername(),
                        blog.getPassword(),
                        Long.toString(comment.postID),
                        replyHash };

                long newCommentID;
                String message = null;
                try {
                    Object newCommentIDObject = client.call(Method.NEW_COMMENT, params);
                    if (newCommentIDObject instanceof Integer) {
                        newCommentID = ((Integer) newCommentIDObject).longValue();
                    } else if (newCommentIDObject instanceof Long) {
                        newCommentID = (Long) newCommentIDObject;
                    } else {
                        AppLog.e(T.COMMENTS, "wp.newComment returned the wrong data type");
                        newCommentID = CommentActionResult.COMMENT_ID_ON_ERRORS;
                    }
                } catch (XMLRPCFault e) {
                    AppLog.e(T.COMMENTS, "Error while sending the new comment", e);
                    newCommentID = CommentActionResult.COMMENT_ID_ON_ERRORS;
                    message = e.getFaultString();
                } catch (XMLRPCException | IOException | XmlPullParserException e) {
                    AppLog.e(T.COMMENTS, "Error while sending the new comment", e);
                    newCommentID = CommentActionResult.COMMENT_ID_ON_ERRORS;
                }

                final CommentActionResult cr = new CommentActionResult(newCommentID, message);

                if (actionListener != null) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            actionListener.onActionResult(cr);
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
    public static void submitReplyToCommentNote(final Note note,
                                         final String replyText,
                                         final CommentActionListener actionListener) {
        if (note == null || TextUtils.isEmpty(replyText)) {
            if (actionListener != null)
                actionListener.onActionResult(new CommentActionResult(CommentActionResult.COMMENT_ID_ON_ERRORS, null));

            return;
        }

        RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                if (actionListener != null)
                    actionListener.onActionResult(new CommentActionResult(CommentActionResult.COMMENT_ID_UNKNOWN, null));
            }
        };
        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                if (volleyError != null)
                    AppLog.e(T.COMMENTS, volleyError.getMessage(), volleyError);
                if (actionListener != null) {
                    actionListener.onActionResult(
                            new CommentActionResult(CommentActionResult.COMMENT_ID_ON_ERRORS, VolleyUtils.messageStringFromVolleyError(volleyError))
                    );
                }
            }
        };

        Note.Reply reply = note.buildReply(replyText);
        WordPress.getRestClientUtils().replyToComment(reply.getContent(), reply.getRestPath(), listener, errorListener);
    }

    /**
     * reply to an individual comment via the WP.com REST API
     */
    public static void submitReplyToCommentRestApi(long siteId, long commentId,
                                                   final String replyText,
                                                   final CommentActionListener actionListener) {
        if (TextUtils.isEmpty(replyText)) {
            if (actionListener != null)
                actionListener.onActionResult(new CommentActionResult(CommentActionResult.COMMENT_ID_ON_ERRORS, null));
            return;
        }

        RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                if (actionListener != null)
                    actionListener.onActionResult(new CommentActionResult(CommentActionResult.COMMENT_ID_UNKNOWN, null));
            }
        };
        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                if (volleyError != null)
                    AppLog.e(T.COMMENTS, volleyError.getMessage(), volleyError);
                if (actionListener != null)
                    actionListener.onActionResult(
                            new CommentActionResult(CommentActionResult.COMMENT_ID_ON_ERRORS, VolleyUtils.messageStringFromVolleyError(volleyError))
                    );
            }
        };

        WordPress.getRestClientUtils().replyToComment(siteId, commentId, replyText, listener, errorListener);
    }

    /**
     * Moderate a comment from a WPCOM notification
     */
    public static void moderateCommentRestApi(long siteId,
                                              final long commentId,
                                              CommentStatus newStatus,
                                              final CommentActionListener actionListener) {

        WordPress.getRestClientUtils().moderateComment(
                String.valueOf(siteId),
                String.valueOf(commentId),
                CommentStatus.toRESTString(newStatus),
                new RestRequest.Listener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        if (actionListener != null) {
                            actionListener.onActionResult(new CommentActionResult(commentId, null));
                        }
                    }
                }, new RestRequest.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if (actionListener != null) {
                            actionListener.onActionResult(new CommentActionResult(CommentActionResult.COMMENT_ID_ON_ERRORS, null));
                        }
                    }
                }
        );
    }

    /**
     * Moderate a comment from a WPCOM notification
     */
    public static void moderateCommentForNote(final Note note, CommentStatus newStatus,
                                              final CommentActionListener actionListener) {
        WordPress.getRestClientUtils().moderateComment(
                String.valueOf(note.getSiteId()),
                String.valueOf(note.getCommentId()),
                CommentStatus.toRESTString(newStatus),
                new RestRequest.Listener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        if (actionListener != null) {
                            actionListener.onActionResult(new CommentActionResult(note.getCommentId(), null));
                        }
                    }
                }, new RestRequest.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if (actionListener != null) {
                            actionListener.onActionResult(new CommentActionResult(CommentActionResult.COMMENT_ID_ON_ERRORS, null));
                        }
                    }
                }
        );
    }

    /**
     * change the status of a single comment
     */
    static void moderateComment(final int accountId,
                                final Comment comment,
                                final CommentStatus newStatus,
                                final CommentActionListener actionListener) {
        // deletion is handled separately
        if (newStatus != null && (newStatus.equals(CommentStatus.TRASH) || newStatus.equals(CommentStatus.DELETE))) {
            deleteComment(accountId, comment, actionListener, newStatus.equals(CommentStatus.DELETE));
            return;
        }

        final Blog blog = WordPress.getBlog(accountId);

        if (blog==null || comment==null || newStatus==null || newStatus==CommentStatus.UNKNOWN) {
            if (actionListener != null)
                actionListener.onActionResult(new CommentActionResult(CommentActionResult.COMMENT_ID_ON_ERRORS, null));
            return;
        }

        final Handler handler = new Handler();

        new Thread() {
            @Override
            public void run() {
                final boolean success = ApiHelper.editComment(blog, comment, newStatus);

                if (success) {
                    CommentTable.updateCommentStatus(blog.getLocalTableBlogId(), comment.commentID, CommentStatus
                            .toString(newStatus));
                }

                if (actionListener != null) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            actionListener.onActionResult(new CommentActionResult(comment.commentID, null));
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
    static void moderateComments(final int accountId,
                                 final CommentList comments,
                                 final CommentStatus newStatus,
                                 final OnCommentsModeratedListener actionListener) {
        // deletion is handled separately
        if (newStatus != null && (newStatus.equals(CommentStatus.TRASH) || newStatus.equals(CommentStatus.DELETE))) {
            deleteComments(accountId, comments, actionListener, newStatus.equals(CommentStatus.DELETE));
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

        final Handler handler = new Handler();
        new Thread() {
            @Override
            public void run() {
                for (Comment comment: comments) {
                    if (ApiHelper.editComment(blog, comment, newStatus)) {
                        comment.setStatus(newStatusStr);
                        moderatedComments.add(comment);
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
                                        final CommentActionListener actionListener,
                                        final boolean deletePermanently) {
        final Blog blog = WordPress.getBlog(accountId);
        if (blog==null || comment==null) {
            if (actionListener != null)
                actionListener.onActionResult(new CommentActionResult(CommentActionResult.COMMENT_ID_ON_ERRORS, null));
            return;
        }

        final Handler handler = new Handler();

        new Thread() {
            @Override
            public void run() {
                XMLRPCClientInterface client = XMLRPCFactory.instantiate(blog.getUri(), blog.getHttpuser(),
                        blog.getHttppassword());

                Object[] params = {
                        blog.getRemoteBlogId(),
                        blog.getUsername(),
                        blog.getPassword(),
                        comment.commentID,
                        deletePermanently};

                Object result;
                try {
                    result = client.call(Method.DELETE_COMMENT, params);
                } catch (final XMLRPCException | XmlPullParserException | IOException e) {
                    AppLog.e(T.COMMENTS, "Error while deleting comment", e);
                    result = null;
                }

                //update local database
                final boolean success = (result != null && Boolean.parseBoolean(result.toString()));
                if (success){
                    if (deletePermanently) {
                        CommentTable.deleteComment(accountId, comment.commentID);
                    }
                    else {
                        // update status in SQLite of successfully moderated comments
                        CommentTable.updateCommentStatus(blog.getLocalTableBlogId(), comment.commentID,
                                CommentStatus.toString(CommentStatus.TRASH));
                    }
                }

                if (actionListener != null) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            actionListener.onActionResult(new CommentActionResult(comment.commentID, null));
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
                                       final OnCommentsModeratedListener actionListener,
                                       final boolean deletePermanently) {
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
                XMLRPCClientInterface client = XMLRPCFactory.instantiate(blog.getUri(), blog.getHttpuser(),
                        blog.getHttppassword());

                for (Comment comment: comments) {
                    Object[] params = {
                            remoteBlogId,
                            blog.getUsername(),
                            blog.getPassword(),
                            comment.commentID,
                            deletePermanently};

                    Object result;
                    try {
                        result = client.call(Method.DELETE_COMMENT, params);
                        boolean success = (result != null && Boolean.parseBoolean(result.toString()));
                        if (success)
                            deletedComments.add(comment);
                    } catch (XMLRPCException | XmlPullParserException | IOException e) {
                        AppLog.e(T.COMMENTS, "Error while deleting comment", e);
                    }
                }

                // remove successfully deleted comments from SQLite
                if (deletePermanently) {
                    CommentTable.deleteComments(localBlogId, deletedComments);
                }
                else {
                    // update status in SQLite of successfully moderated comments
                    CommentTable.updateCommentsStatus(blog.getLocalTableBlogId(), deletedComments,
                            CommentStatus.toString(CommentStatus.TRASH));
                }

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
