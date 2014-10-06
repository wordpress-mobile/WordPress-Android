package org.wordpress.android.ui.reader.actions;

import android.os.Handler;
import android.text.TextUtils;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONArray;
import org.json.JSONObject;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.ReaderCommentTable;
import org.wordpress.android.datasets.ReaderDatabase;
import org.wordpress.android.datasets.ReaderLikeTable;
import org.wordpress.android.datasets.ReaderUserTable;
import org.wordpress.android.models.ReaderComment;
import org.wordpress.android.models.ReaderCommentList;
import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.models.ReaderUser;
import org.wordpress.android.models.ReaderUserList;
import org.wordpress.android.ui.reader.ReaderConstants;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.JSONUtil;
import org.wordpress.android.util.VolleyUtils;

import java.util.HashMap;
import java.util.Map;

public class ReaderCommentActions {
    /**
     * get the latest comments for this post
     **/
    public static void updateCommentsForPost(final ReaderPost post,
                                             final boolean requestNewer,
                                             final ReaderActions.UpdateResultListener resultListener) {
        String path = "sites/" + post.blogId + "/posts/" + post.postId + "/replies/"
                    + "?number=" + Integer.toString(ReaderConstants.READER_MAX_COMMENTS_TO_REQUEST)
                    + "&meta=likes";

        // get older comments first - subsequent calls to this routine will get newer ones if they exist
        path += "&order=ASC";

        // offset by the number of comments already stored locally (so we only get new comments)
        if (requestNewer) {
            int numLocalComments = ReaderCommentTable.getNumCommentsForPost(post);
            if (numLocalComments > 0) {
                path += "&offset=" + Integer.toString(numLocalComments);
            }
        }

        RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                handleUpdateCommentsResponse(jsonObject, post.blogId, requestNewer, resultListener);
            }
        };
        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.e(T.READER, volleyError);
                if (resultListener != null) {
                    resultListener.onUpdateResult(ReaderActions.UpdateResult.FAILED);
                }
            }
        };
        AppLog.d(T.READER, "updating comments");
        WordPress.getRestClientUtils().get(path, null, null, listener, errorListener);
    }
    private static void handleUpdateCommentsResponse(final JSONObject jsonObject,
                                                     final long blogId,
                                                     final boolean requestNewer,
                                                     final ReaderActions.UpdateResultListener resultListener) {
        if (jsonObject == null) {
            if (resultListener != null) {
                resultListener.onUpdateResult(ReaderActions.UpdateResult.FAILED);
            }
            return;
        }

        final Handler handler = new Handler();

        new Thread() {
            @Override
            public void run() {
                final boolean hasNewComments;

                ReaderDatabase.getWritableDb().beginTransaction();
                try {
                    ReaderCommentList serverComments = new ReaderCommentList();
                    JSONArray jsonCommentList = jsonObject.optJSONArray("comments");
                    if (jsonCommentList != null) {
                        for (int i = 0; i < jsonCommentList.length(); i++) {
                            JSONObject jsonComment = jsonCommentList.optJSONObject(i);

                            // extract this comment and add it to the list
                            ReaderComment comment = ReaderComment.fromJson(jsonComment, blogId);
                            serverComments.add(comment);

                            // extract and save likes for this comment
                            JSONObject jsonLikes = JSONUtil.getJSONChild(jsonComment, "meta/data/likes");
                            if (jsonLikes != null) {
                                ReaderUserList likingUsers = ReaderUserList.fromJsonLikes(jsonLikes);
                                ReaderUserTable.addOrUpdateUsers(likingUsers);
                                ReaderLikeTable.setLikesForComment(comment, likingUsers.getUserIds());
                            }
                        }
                    }

                    if (requestNewer) {
                        hasNewComments = (serverComments.size() > 0);
                    } else {
                        hasNewComments = ReaderCommentTable.hasNewComments(serverComments);
                    }

                    // save to db regardless of whether any are new so changes to likes are stored
                    ReaderCommentTable.addOrUpdateComments(serverComments);
                    ReaderDatabase.getWritableDb().setTransactionSuccessful();
                } finally {
                    ReaderDatabase.getWritableDb().endTransaction();
                }

                if (resultListener != null) {
                    handler.post(new Runnable() {
                        public void run() {
                            ReaderActions.UpdateResult result =
                                    (hasNewComments ? ReaderActions.UpdateResult.CHANGED
                                                    : ReaderActions.UpdateResult.UNCHANGED);
                            resultListener.onUpdateResult(result);
                        }
                    });
                }
            }
        }.start();
    }

    /*
     * request liking users for the passed comment - UNTESTED
     */
    private static void updateCommentLikes(final ReaderComment comment,
                                           final ReaderActions.UpdateResultListener resultListener) {
        if (comment == null) {
            if (resultListener != null) {
                resultListener.onUpdateResult(ReaderActions.UpdateResult.FAILED);
            }
            return;
        }

        RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                handleUpdateCommentLikesResponse(comment, jsonObject, resultListener);
            }
        };
        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.e(T.READER, volleyError);
                if (resultListener != null) {
                    resultListener.onUpdateResult(ReaderActions.UpdateResult.FAILED);
                }

            }
        };

        AppLog.d(T.READER, "updating comment likes");
        String path = "sites/" + comment.blogId + "/comments/" + comment.commentId + "/likes/";
        WordPress.getRestClientUtils().get(path, null, null, listener, errorListener);
    }

    private static void handleUpdateCommentLikesResponse(final ReaderComment comment,
                                                         final JSONObject jsonObject,
                                                         final ReaderActions.UpdateResultListener resultListener) {
        if (comment == null || jsonObject == null) {
            if (resultListener != null) {
                resultListener.onUpdateResult(ReaderActions.UpdateResult.FAILED);
            }
            return;
        }

        int numLikes = jsonObject.optInt("found");
        boolean isLikedByCurrentUser = JSONUtil.getBool(jsonObject, "i_like");
        boolean isChanged = (comment.numLikes != numLikes || comment.isLikedByCurrentUser != isLikedByCurrentUser);

        if (isChanged) {
            comment.isLikedByCurrentUser = isLikedByCurrentUser;
            comment.numLikes = numLikes;
            ReaderCommentTable.addOrUpdateComment(comment);
            ReaderLikeTable.setCurrentUserLikesComment(comment, isLikedByCurrentUser);
        }

        ReaderUserList likingUsers = ReaderUserList.fromJsonLikes(jsonObject);
        ReaderUserTable.addOrUpdateUsers(likingUsers);
        ReaderLikeTable.setLikesForComment(comment, likingUsers.getUserIds());

        if (resultListener != null) {
            if (isChanged) {
                resultListener.onUpdateResult(ReaderActions.UpdateResult.CHANGED);
            } else {
                resultListener.onUpdateResult(ReaderActions.UpdateResult.UNCHANGED);
            }
        }
    }

    /*
     * used by post detail to generate a temporary "fake" comment id (see below)
     */
    public static long generateFakeCommentId() {
        return System.currentTimeMillis();
    }

    /*
     * add the passed comment text to the passed post - caller must pass a unique "fake" comment id
     * to give the comment that's generated locally
     */
    public static ReaderComment submitPostComment(final ReaderPost post,
                                                  final long fakeCommentId,
                                                  final String commentText,
                                                  final long replyToCommentId,
                                                  final ReaderActions.CommentActionListener actionListener) {
        if (post==null || TextUtils.isEmpty(commentText))
            return null;

        // create a "fake" comment that's added to the db so it can be shown right away - will be
        // replaced with actual comment if it succeeds to be posted, or deleted if comment fails
        // to be posted
        ReaderComment newComment = new ReaderComment();
        newComment.commentId = fakeCommentId;
        newComment.postId = post.postId;
        newComment.blogId = post.blogId;
        newComment.parentId = replyToCommentId;
        newComment.setText(commentText);
        String published = DateTimeUtils.nowUTC().toString();
        newComment.setPublished(published);
        newComment.timestamp = DateTimeUtils.iso8601ToTimestamp(published);
        ReaderUser currentUser = ReaderUserTable.getCurrentUser();
        if (currentUser!=null) {
            newComment.setAuthorAvatar(currentUser.getAvatarUrl());
            newComment.setAuthorName(currentUser.getDisplayName());
        }
        ReaderCommentTable.addOrUpdateComment(newComment);

        // different endpoint depending on whether the new comment is a reply to another comment
        final String path;
        if (replyToCommentId==0) {
            path = "sites/" + post.blogId + "/posts/" + post.postId + "/replies/new";
        } else {
            path = "sites/" + post.blogId + "/comments/" + Long.toString(replyToCommentId) + "/replies/new";
        }

        Map<String, String> params = new HashMap<String, String>();
        params.put("content", commentText);

        RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                ReaderCommentTable.deleteComment(post, fakeCommentId);
                AppLog.i(T.READER, "comment succeeded");
                ReaderComment newComment = ReaderComment.fromJson(jsonObject, post.blogId);
                ReaderCommentTable.addOrUpdateComment(newComment);
                if (actionListener!=null)
                    actionListener.onActionResult(true, newComment);
            }
        };
        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                ReaderCommentTable.deleteComment(post, fakeCommentId);
                AppLog.w(T.READER, "comment failed");
                AppLog.e(T.READER, volleyError);
                if (actionListener!=null)
                    actionListener.onActionResult(false, null);
            }
        };

        AppLog.i(T.READER, "submitting comment");
        WordPress.getRestClientUtils().post(path, params, null, listener, errorListener);

        return newComment;
    }

    /*
     * like or unlike the passed comment
     */
    public static boolean performLikeAction(final ReaderComment comment, boolean isAskingToLike) {
        if (comment == null) {
            return false;
        }

        // make sure like status is changing
        boolean isCurrentlyLiked = ReaderCommentTable.isCommentLikedByCurrentUser(comment);
        if (isCurrentlyLiked == isAskingToLike) {
            AppLog.w(T.READER, "comment like unchanged");
            return false;
        }

        // update like status and like count in local db
        int newNumLikes = (isAskingToLike ? comment.numLikes + 1 : comment.numLikes - 1);
        ReaderCommentTable.setLikesForComment(comment, newNumLikes, isAskingToLike);
        ReaderLikeTable.setCurrentUserLikesComment(comment, isAskingToLike);

        // sites/$site/comments/$comment_ID/likes/new
        final String actionName = isAskingToLike ? "like" : "unlike";
        String path = "sites/" + comment.blogId + "/comments/" + comment.commentId + "/likes/";
        if (isAskingToLike) {
            path += "new";
        } else {
            path += "mine/delete";
        }

        RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                boolean success = (jsonObject != null && JSONUtil.getBool(jsonObject, "success"));
                if (success) {
                    AppLog.d(T.READER, String.format("comment %s succeeded", actionName));
                } else {
                    AppLog.w(T.READER, String.format("comment %s failed", actionName));
                    ReaderCommentTable.setLikesForComment(comment, comment.numLikes, comment.isLikedByCurrentUser);
                    ReaderLikeTable.setCurrentUserLikesComment(comment, comment.isLikedByCurrentUser);
                }
            }
        };

        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                String error = VolleyUtils.errStringFromVolleyError(volleyError);
                if (TextUtils.isEmpty(error)) {
                    AppLog.w(T.READER, String.format("comment %s failed", actionName));
                } else {
                    AppLog.w(T.READER, String.format("comment %s failed (%s)", actionName, error));
                }
                AppLog.e(T.READER, volleyError);
                ReaderCommentTable.setLikesForComment(comment, comment.numLikes, comment.isLikedByCurrentUser);
                ReaderLikeTable.setCurrentUserLikesComment(comment, comment.isLikedByCurrentUser);
            }
        };

        WordPress.getRestClientUtils().post(path, listener, errorListener);
        return true;
    }
}
