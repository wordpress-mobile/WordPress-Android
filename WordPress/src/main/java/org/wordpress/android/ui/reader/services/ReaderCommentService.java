package org.wordpress.android.ui.reader.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

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
import org.wordpress.android.models.ReaderUserList;
import org.wordpress.android.ui.reader.ReaderConstants;
import org.wordpress.android.ui.reader.ReaderEvents;
import org.wordpress.android.ui.reader.actions.ReaderActions;
import org.wordpress.android.ui.reader.actions.ReaderActions.UpdateResult;
import org.wordpress.android.ui.reader.actions.ReaderActions.UpdateResultListener;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.JSONUtils;

import de.greenrobot.event.EventBus;

public class ReaderCommentService extends Service {

    private static final String ARG_POST_ID   = "post_id";
    private static final String ARG_BLOG_ID   = "blog_id";
    private static final String ARG_NEXT_PAGE = "next_page";

    public static void startService(Context context, ReaderPost post, boolean requestNextPage) {
        Intent intent = new Intent(context, ReaderCommentService.class);
        intent.putExtra(ARG_BLOG_ID, post.blogId);
        intent.putExtra(ARG_POST_ID, post.postId);
        intent.putExtra(ARG_NEXT_PAGE, requestNextPage);
        context.startService(intent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        AppLog.i(AppLog.T.READER, "reader comment service > created");
    }

    @Override
    public void onDestroy() {
        AppLog.i(AppLog.T.READER, "reader comment service > destroyed");
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return START_NOT_STICKY;
        }

        EventBus.getDefault().post(new ReaderEvents.UpdateCommentsStarted());

        long blogId = intent.getLongExtra(ARG_BLOG_ID, 0);
        long postId = intent.getLongExtra(ARG_POST_ID, 0);
        boolean requestNextPage = intent.getBooleanExtra(ARG_NEXT_PAGE, false);

        int pageNumber;
        if (requestNextPage) {
            int prevPage = ReaderCommentTable.getLastPageNumberForPost(blogId, postId);
            pageNumber = prevPage + 1;
        } else {
            pageNumber = 1;
        }

        updateCommentsForPost(blogId, postId, pageNumber, new UpdateResultListener() {
            @Override
            public void onUpdateResult(UpdateResult result) {
                EventBus.getDefault().post(new ReaderEvents.UpdateCommentsEnded(result));
                stopSelf();
            }
        });

        return START_NOT_STICKY;
    }

    private static void updateCommentsForPost(final long blogId,
                                              final long postId,
                                              final int pageNumber,
                                              final ReaderActions.UpdateResultListener resultListener) {
        String path = "sites/" + blogId + "/posts/" + postId + "/replies/"
                    + "?number=" + Integer.toString(ReaderConstants.READER_MAX_COMMENTS_TO_REQUEST)
                    + "&meta=likes"
                    + "&hierarchical=true"
                    + "&order=ASC"
                    + "&page=" + pageNumber;

        RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                handleUpdateCommentsResponse(jsonObject, blogId, pageNumber, resultListener);
            }
        };
        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.e(AppLog.T.READER, volleyError);
                resultListener.onUpdateResult(ReaderActions.UpdateResult.FAILED);
            }
        };
        AppLog.d(AppLog.T.READER, "updating comments");
        WordPress.getRestClientUtilsV1_1().get(path, null, null, listener, errorListener);
    }
    private static void handleUpdateCommentsResponse(final JSONObject jsonObject,
                                                     final long blogId,
                                                     final int pageNumber,
                                                     final ReaderActions.UpdateResultListener resultListener) {
        if (jsonObject == null) {
            resultListener.onUpdateResult(ReaderActions.UpdateResult.FAILED);
            return;
        }

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
                            comment.pageNumber = pageNumber;
                            serverComments.add(comment);

                            // extract and save likes for this comment
                            JSONObject jsonLikes = JSONUtils.getJSONChild(jsonComment, "meta/data/likes");
                            if (jsonLikes != null) {
                                ReaderUserList likingUsers = ReaderUserList.fromJsonLikes(jsonLikes);
                                ReaderUserTable.addOrUpdateUsers(likingUsers);
                                ReaderLikeTable.setLikesForComment(comment, likingUsers.getUserIds());
                            }
                        }
                    }

                    hasNewComments = (serverComments.size() > 0);

                    // save to db regardless of whether any are new so changes to likes are stored
                    ReaderCommentTable.addOrUpdateComments(serverComments);
                    ReaderDatabase.getWritableDb().setTransactionSuccessful();
                } finally {
                    ReaderDatabase.getWritableDb().endTransaction();
                }

                ReaderActions.UpdateResult result =
                        (hasNewComments ? ReaderActions.UpdateResult.HAS_NEW : ReaderActions.UpdateResult.UNCHANGED);
                resultListener.onUpdateResult(result);
            }
        }.start();
    }
}
