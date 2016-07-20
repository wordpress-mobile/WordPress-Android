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

    public static final int COMMENTS_PER_PAGE = ReaderConstants.READER_MAX_COMMENTS_TO_REQUEST;

    private static final String ARG_POST_ID     = "post_id";
    private static final String ARG_BLOG_ID     = "blog_id";
    private static final String ARG_COMMENT_ID  = "comment_id";
    private static final String ARG_NEXT_PAGE   = "next_page";
    private static final String ARG_COMMENTS_ORDER = "comments_order";

    public static void startService(Context context, long blogId, long postId, boolean requestNextPage,
                                    @ReaderCommentTable.CommentsOrderBy int commentsOrder) {
        if (context == null) return;

        Intent intent = new Intent(context, ReaderCommentService.class);
        intent.putExtra(ARG_BLOG_ID, blogId);
        intent.putExtra(ARG_POST_ID, postId);
        intent.putExtra(ARG_NEXT_PAGE, requestNextPage);
        intent.putExtra(ARG_COMMENTS_ORDER, commentsOrder);
        context.startService(intent);
    }

    // Requests comments until the passed commentId is found
    public static void startServiceForComment(Context context, long blogId, long postId, long commentId) {
        if (context == null) return;

        Intent intent = new Intent(context, ReaderCommentService.class);
        intent.putExtra(ARG_BLOG_ID, blogId);
        intent.putExtra(ARG_POST_ID, postId);
        intent.putExtra(ARG_COMMENT_ID, commentId);
        intent.putExtra(ARG_COMMENTS_ORDER, ReaderCommentTable.ORDER_BY_DEFAULT);
        context.startService(intent);
    }

    public static void stopService(Context context) {
        if (context == null) return;

        Intent intent = new Intent(context, ReaderCommentService.class);
        context.stopService(intent);
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

        final long blogId = intent.getLongExtra(ARG_BLOG_ID, 0);
        final long postId = intent.getLongExtra(ARG_POST_ID, 0);
        final long commentId = intent.getLongExtra(ARG_COMMENT_ID, 0);
        boolean requestNextPage = intent.getBooleanExtra(ARG_NEXT_PAGE, false);
        final int commentsOrder = intent.getIntExtra(ARG_COMMENTS_ORDER,ReaderCommentTable.ORDER_BY_DEFAULT);

        int currentPage;

        if(requestNextPage){
            if( commentsOrder == ReaderCommentTable.ORDER_BY_NEWEST_COMMENT_FIRST ){
                currentPage = ReaderCommentTable.getLastNotLoadedPage(blogId, postId, COMMENTS_PER_PAGE);
            }else if(commentsOrder == ReaderCommentTable.ORDER_BY_TIME_OF_COMMENT){
                currentPage = ReaderCommentTable.getFirstNotLoadedPage(blogId, postId, COMMENTS_PER_PAGE);
            }else{
                throw new IllegalArgumentException("Unknown commentsOrder");
            }
        }else{
            currentPage = 1;
        }

        if(currentPage <= 0){
            return START_NOT_STICKY;
        }

        updateCommentsForPost(blogId, postId, currentPage, commentsOrder, new UpdateResultListener() {
            @Override
            public void onUpdateResult(UpdateResult result) {
                if (commentId > 0) {
                    if (ReaderCommentTable.commentExists(blogId, postId, commentId) || !result.isNewOrChanged()) {
                        EventBus.getDefault().post(new ReaderEvents.UpdateCommentsEnded(result));
                        stopSelf();
                    } else {
                        // Comment not found yet, request the next page
                        int nextPage;
                        if(commentsOrder == ReaderCommentTable.ORDER_BY_NEWEST_COMMENT_FIRST){
                            nextPage = ReaderCommentTable.getLastNotLoadedPage(blogId,postId,COMMENTS_PER_PAGE);
                        }else{
                            nextPage = ReaderCommentTable.getFirstNotLoadedPage(blogId,postId,COMMENTS_PER_PAGE);
                        }

                        updateCommentsForPost(blogId, postId, nextPage , commentsOrder, this);
                    }
                } else {
                    EventBus.getDefault().post(new ReaderEvents.UpdateCommentsEnded(result));
                    stopSelf();
                }
            }
        });

        return START_NOT_STICKY;
    }

    private static void updateCommentsForPost(final long blogId,
                                              final long postId,
                                              final int pageNumber,
                                              final int commentOrder,
                                              final ReaderActions.UpdateResultListener resultListener) {
        String path = "sites/" + blogId + "/posts/" + postId + "/replies/"
                    + "?number=" + Integer.toString(COMMENTS_PER_PAGE)
                    + "&meta=likes"
                    + "&hierarchical=true"
                    + "&page=" + pageNumber;

        if(commentOrder == ReaderCommentTable.ORDER_BY_NEWEST_COMMENT_FIRST){
            path += "&order=DESC";
        }else{
            path += "&order=ASC";
        }

        RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                handleUpdateCommentsResponse(jsonObject, blogId, pageNumber, commentOrder, resultListener);
            }
        };
        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.e(AppLog.T.READER, volleyError);
                resultListener.onUpdateResult(ReaderActions.UpdateResult.FAILED);
            }
        };

        AppLog.d(AppLog.T.READER, "updating comments - pageNumber: " + pageNumber +
                (commentOrder == ReaderCommentTable.ORDER_BY_NEWEST_COMMENT_FIRST?" backward":" forward") );

        WordPress.getRestClientUtilsV1_1().get(path, null, null, listener, errorListener);
    }
    private static void handleUpdateCommentsResponse(final JSONObject jsonObject,
                                                     final long blogId,
                                                     final int pageNumber,
                                                     final int commentOrder,
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
                    int commentsFound = jsonObject.optInt("found");
                    if (jsonCommentList != null) {
                        for (int i = 0; i < jsonCommentList.length(); i++) {
                            JSONObject jsonComment = jsonCommentList.optJSONObject(i);

                            // extract this comment and add it to the list
                            ReaderComment comment = ReaderComment.fromJson(jsonComment, blogId);

                            if( commentOrder == ReaderCommentTable.ORDER_BY_NEWEST_COMMENT_FIRST ){
                                //calculate forward page number of comment
                                int commentIndex = ( pageNumber - 1 )*COMMENTS_PER_PAGE + i;
                                int forwardCommentIndex = commentsFound - commentIndex;
                                comment.pageNumber = ( forwardCommentIndex + COMMENTS_PER_PAGE - 1 )/COMMENTS_PER_PAGE;
                            }else {
                                comment.pageNumber = pageNumber;
                            }

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
