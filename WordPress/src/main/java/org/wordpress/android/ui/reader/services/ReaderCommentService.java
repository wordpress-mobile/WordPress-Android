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
                currentPage = ReaderCommentTable.getLastNotLoadedPage(blogId, postId, true);
            }else if(commentsOrder == ReaderCommentTable.ORDER_BY_TIME_OF_COMMENT){
                currentPage = ReaderCommentTable.getFirstNotLoadedPage(blogId, postId);
            }else{
                throw new IllegalArgumentException("Unknown commentsOrder");
            }
        }else{
            currentPage = 1;
        }

        if( currentPage < 0 ){
            //all pages are loaded
            return START_NOT_STICKY;
        }else if( currentPage == 0 ){
            //no any comment is loaded yet
            currentPage = 1;
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
                            nextPage = ReaderCommentTable.getLastNotLoadedPage(blogId, postId, true);
                        }else{
                            nextPage = ReaderCommentTable.getFirstNotLoadedPage(blogId, postId);
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
                    + "?number=" + Integer.toString(ReaderConstants.READER_MAX_COMMENTS_TO_REQUEST)
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
                handleUpdateCommentsResponse(jsonObject, blogId, postId, pageNumber, commentOrder, resultListener);
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

    /**
     * load a single comment if it is not in database
     * @param refreshComment download fresh comment from server whether it is present in database or not
     * @param loadParents load all parents of this comment
     * @param commentsLoaded should be 0, number of comments loaded (used for recursion)
     */
    private static void updateComment(final long blogId,
                                      final long postId,
                                      final long commentId,
                                      final boolean refreshComment,
                                      final boolean loadParents,
                                      final UpdateResultListener resultListener,
                                      final int commentsLoaded){
        boolean loadCurrentComment;

        if( refreshComment ){
            loadCurrentComment = true;
        }else if( ReaderCommentTable.commentExists(blogId,postId,commentId) ){
            loadCurrentComment = false;
        }else {
            //comment not exist in database, load from server
            loadCurrentComment = true;
        }

        if( loadCurrentComment ){
            String path = "sites/" + blogId + "/comments/" + commentId + "?meta=likes";

            RestRequest.Listener listener = new RestRequest.Listener(){
                @Override
                public void onResponse(JSONObject jsonComment) {
                    if(jsonComment == null){
                        if(resultListener != null ) {
                            resultListener.onUpdateResult(commentsLoaded > 0 ? UpdateResult.HAS_NEW : UpdateResult.FAILED);
                        }
                        return;
                    }

                    ReaderComment comment = ReaderComment.fromJson(jsonComment, blogId);
                    saveLikesForComment(jsonComment,comment);
                    ReaderCommentTable.addOrUpdateComment(comment);

                    AppLog.d(AppLog.T.READER, "updating comment , commentId: " + commentId );

                    if( loadParents && comment.parentId != 0 ){
                        updateComment(blogId, postId, comment.parentId, refreshComment, loadParents, resultListener, commentsLoaded+1);
                    }else if( resultListener != null ) {
                        resultListener.onUpdateResult(UpdateResult.HAS_NEW);
                    }
                }
            };

            RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError volleyError) {
                    AppLog.e(AppLog.T.READER, volleyError);
                    if( resultListener != null ) {
                        resultListener.onUpdateResult(commentsLoaded > 0 ? UpdateResult.HAS_NEW : UpdateResult.FAILED);
                    }
                }
            };

            WordPress.getRestClientUtilsV1_1().get(path, null, null, listener, errorListener);
            return;
        }

        if( loadParents ){
            long parentId = ReaderCommentTable.getParentIdOfComment(blogId, postId, commentId);
            if( parentId != 0 ) {
                updateComment(blogId, postId, parentId, refreshComment, loadParents, resultListener, commentsLoaded);
                return;
            }
        }

        if( resultListener != null ){
            resultListener.onUpdateResult( commentsLoaded > 0 ? UpdateResult.HAS_NEW : UpdateResult.UNCHANGED );
        }
    }

    private static void handleUpdateCommentsResponse(final JSONObject jsonObject,
                                                     final long blogId,
                                                     final long postId,
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

                ReaderCommentList serverComments = new ReaderCommentList();
                ReaderDatabase.getWritableDb().beginTransaction();
                try {
                    JSONArray jsonCommentList = jsonObject.optJSONArray("comments");
                    int commentsFound = jsonObject.optInt("found");
                    if (jsonCommentList != null) {
                        for (int i = 0; i < jsonCommentList.length(); i++) {
                            JSONObject jsonComment = jsonCommentList.optJSONObject(i);

                            // extract this comment and add it to the list
                            ReaderComment comment = ReaderComment.fromJson(jsonComment, blogId);

                            if( commentOrder == ReaderCommentTable.ORDER_BY_NEWEST_COMMENT_FIRST ){
                                //calculate forward page number of comment
                                int commentIndex = ( pageNumber - 1 )*ReaderCommentTable.COMMENTS_PER_PAGE + i;
                                int forwardCommentIndex = commentsFound - commentIndex;
                                comment.pageNumber = ( forwardCommentIndex + ReaderCommentTable.COMMENTS_PER_PAGE - 1 )/
                                                            ReaderCommentTable.COMMENTS_PER_PAGE;
                            }else {
                                comment.pageNumber = pageNumber;
                            }

                            serverComments.add(comment);

                            saveLikesForComment(jsonComment,comment);
                        }
                    }

                    hasNewComments = (serverComments.size() > 0);

                    // save to db regardless of whether any are new so changes to likes are stored
                    ReaderCommentTable.addOrUpdateComments(serverComments);
                    ReaderDatabase.getWritableDb().setTransactionSuccessful();
                } finally {
                    ReaderDatabase.getWritableDb().endTransaction();
                }

                //load parents of comments if not present in database
                if( commentOrder == ReaderCommentTable.ORDER_BY_NEWEST_COMMENT_FIRST ){
                    for(ReaderComment comment : serverComments){
                        if(comment.parentId == 0){
                            continue;
                        }

                        updateComment(blogId, postId, comment.parentId, false, true, null, 0);
                    }
                }

                ReaderActions.UpdateResult result =
                        (hasNewComments ? ReaderActions.UpdateResult.HAS_NEW : ReaderActions.UpdateResult.UNCHANGED);
                resultListener.onUpdateResult(result);
            }
        }.start();
    }

    // extract and save likes for this comment
    private static void saveLikesForComment(JSONObject jsonComment,ReaderComment comment){
        JSONObject jsonLikes = JSONUtils.getJSONChild(jsonComment, "meta/data/likes");
        if (jsonLikes != null) {
            ReaderUserList likingUsers = ReaderUserList.fromJsonLikes(jsonLikes);
            ReaderUserTable.addOrUpdateUsers(likingUsers);
            ReaderLikeTable.setLikesForComment(comment, likingUsers.getUserIds());
        }
    }
}
