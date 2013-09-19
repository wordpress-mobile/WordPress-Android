package org.wordpress.android.ui.reader_native.actions;

import android.os.Handler;
import android.text.TextUtils;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONObject;
import org.wordpress.android.Constants;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.ReaderCommentTable;
import org.wordpress.android.datasets.ReaderUserTable;
import org.wordpress.android.models.ReaderComment;
import org.wordpress.android.models.ReaderCommentList;
import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.models.ReaderUser;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.ReaderLog;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by nbradbury on 9/1/13.
 */
public class ReaderCommentActions {

    /**
     * get the latest comments for this post
     **/
    public static void updateCommentsForPost(final ReaderPost post, final ReaderActions.UpdateResultListener resultListener) {
        String path = "sites/" + post.blogId + "/posts/" + post.postId + "/replies/?number=" + Integer.toString(Constants.READER_MAX_COMMENTS_TO_REQUEST);

        // get older comments first - subsequent calls to this routine will get newer ones if they exist
        path += "&order=ASC";

        // offset by the number of comments already stored locally (so we only get new comments)
        int numLocalComments = ReaderCommentTable.getNumCommentsForPost(post);
        if (numLocalComments > 0)
            path += "&offset=" + Integer.toString(numLocalComments);

        com.wordpress.rest.RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                handleUpdateCommentsResponse(jsonObject, post.blogId, resultListener);
            }
        };
        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                ReaderLog.e(volleyError);
                if (resultListener!=null)
                    resultListener.onUpdateResult(ReaderActions.UpdateResult.FAILED);

            }
        };
        ReaderLog.d("updating comments");
        WordPress.restClient.get(path, null, null, listener, errorListener);
    }
    private static void handleUpdateCommentsResponse(final JSONObject jsonObject, final long blogId, final ReaderActions.UpdateResultListener resultListener) {
        if (jsonObject==null) {
            if (resultListener!=null)
                resultListener.onUpdateResult(ReaderActions.UpdateResult.FAILED);
            return;
        }

        final Handler handler = new Handler();

        new Thread() {
            @Override
            public void run() {
                // request asks for only newer comments, so if it returns any comments then they are all new
                ReaderCommentList serverComments = ReaderCommentList.fromJson(jsonObject, blogId);
                final int numNew = serverComments.size();
                if (numNew > 0) {
                    ReaderLog.d("new comments found");
                    ReaderCommentTable.addOrUpdateComments(serverComments);
                }

                if (resultListener!=null) {
                    handler.post(new Runnable() {
                        public void run() {
                            resultListener.onUpdateResult(numNew > 0 ? ReaderActions.UpdateResult.CHANGED : ReaderActions.UpdateResult.UNCHANGED);
                        }
                    });
                }
            }
        }.start();
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

        com.wordpress.rest.RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                ReaderCommentTable.deleteComment(post, fakeCommentId);
                ReaderLog.i("comment succeeded");
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
                ReaderLog.w("comment failed");
                ReaderLog.e(volleyError);
                if (actionListener!=null)
                    actionListener.onActionResult(false, null);
            }
        };

        ReaderLog.i("submitting comment");
        WordPress.restClient.post(path, params, null, listener, errorListener);

        return newComment;
    }
}
