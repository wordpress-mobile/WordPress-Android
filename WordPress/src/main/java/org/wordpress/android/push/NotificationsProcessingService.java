package org.wordpress.android.push;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.models.CommentStatus;
import org.wordpress.android.models.Note;
import org.wordpress.android.ui.comments.CommentActionResult;
import org.wordpress.android.ui.comments.CommentActions;
import org.wordpress.android.util.AppLog;

import java.util.HashMap;

/**
 * service which makes it possible to process Notifications quick actions in the background,
 * such as:
 * - like
 * - reply-to-comment
 * - approve
 */

public class NotificationsProcessingService extends Service {

    private static final String ARG_ACTION_TYPE = "action_type";
    private static final String ARG_ACTION_LIKE = "action_like";
    private static final String ARG_ACTION_REPLY = "action_reply";
    private static final String ARG_ACTION_APPROVE = "action_approve";
    private static final String ARG_ACTION_REPLY_TEXT = "action_reply_text";
    private static final String ARG_NOTE_ID = "note_id";
    private String mNoteId;
    private String mReplyText;
    private String mActionType;
    private Note mNote;

    public static void startServiceForLike(Context context, String noteId) {
        Intent intent = new Intent(context, NotificationsProcessingService.class);
        intent.putExtra(ARG_ACTION_TYPE, ARG_ACTION_LIKE);
        intent.putExtra(ARG_NOTE_ID, noteId);
        context.startService(intent);
    }

    public static void startServiceForApprove(Context context, String noteId) {
        Intent intent = new Intent(context, NotificationsProcessingService.class);
        intent.putExtra(ARG_ACTION_TYPE, ARG_ACTION_APPROVE);
        intent.putExtra(ARG_NOTE_ID, noteId);
        context.startService(intent);
    }

    public static void startServiceForReply(Context context, String noteId, String replyToComment) {
        Intent intent = new Intent(context, NotificationsProcessingService.class);
        intent.putExtra(ARG_ACTION_TYPE, ARG_ACTION_REPLY);
        intent.putExtra(ARG_NOTE_ID, noteId);
        intent.putExtra(ARG_ACTION_REPLY_TEXT, replyToComment);
        context.startService(intent);
    }

    public static void stopService(Context context) {
        if (context == null) return;

        Intent intent = new Intent(context, NotificationsProcessingService.class);
        context.stopService(intent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        AppLog.i(AppLog.T.NOTIFS, "notifications action processing service > created");
    }

    @Override
    public void onDestroy() {
        AppLog.i(AppLog.T.NOTIFS, "notifications action processing service > destroyed");
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mNoteId = intent.getStringExtra(ARG_NOTE_ID);
        mActionType = intent.getStringExtra(ARG_ACTION_TYPE);
        if (intent.hasExtra(ARG_ACTION_REPLY_TEXT)) {
            mReplyText = intent.getStringExtra(ARG_ACTION_REPLY_TEXT);
        }

        if (mActionType != null) {
            RestRequest.Listener listener =
                    new RestRequest.Listener() {
                        @Override
                        public void onResponse(JSONObject response) {
                            if (response != null && !response.optBoolean("success")) {
                                //build the Note object here
                                buildNoteFromJSONObject(response);
                                performRequestedAction();
                            }
                        }
                    };

            RestRequest.ErrorListener errorListener =
                    new RestRequest.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            requestFailed();
                        }
                    };

            getNoteFromNoteId(mNoteId, listener, errorListener);

        } else {
            requestFailed();
        }

        return START_NOT_STICKY;
    }

    private void buildNoteFromJSONObject(JSONObject jsonObject) {
        try {
            if (jsonObject.has("notes")) {
                JSONArray jsonArray = jsonObject.getJSONArray("notes");
                if (jsonArray != null && jsonArray.length() == 1) {
                    jsonObject = jsonArray.getJSONObject(0);
                }
            }
            mNote = new Note.Schema().build(mNoteId, jsonObject);

        } catch (JSONException e) {
            AppLog.e(AppLog.T.NOTIFS, e.getMessage());
        }
    }

    private void performRequestedAction(){
        if (mNote != null) {
            if (mActionType.equals(ARG_ACTION_LIKE)) {
                likeComment();
            } else if (mActionType.equals(ARG_ACTION_APPROVE)) {
                approveComment();
            } else if (mActionType.equals(ARG_ACTION_REPLY)) {
                replyToComment();
            } else {
                //no op
                requestFailed();
            }
        } else {
            requestFailed();
        }
    }

    /*
     * called when action has been completed successfully
     */
    private void requestCompleted() {
        //TODO show a success notification
        //EventBus.getDefault().post(new PlanEvents.PlansUpdated(mLocalBlogId, mSitePlans));
    }

    /*
     * called when action failed
     */
    private void requestFailed() {
        //EventBus.getDefault().post(new PlanEvents.PlansUpdateFailed());
        showErrorToUserAndFinish();
    }

    private void showErrorToUserAndFinish() {
        //TODO implement a notification in the system dashboard to tell the user how this went
    }


    private void getNoteFromNoteId(String noteId, RestRequest.Listener listener, RestRequest.ErrorListener errorListener) {
        if (noteId == null) return;

        HashMap<String, String> params = new HashMap<>();
        WordPress.getRestClientUtils().getNotification(params,
                noteId, listener, errorListener
        );
    }


    // Like or unlike a comment via the REST API
    private void likeComment() {
        if (mNote == null) return;

        // Bump analytics
        AnalyticsTracker.track(AnalyticsTracker.Stat.NOTIFICATION_LIKED);

        WordPress.getRestClientUtils().likeComment(String.valueOf(mNote.getSiteId()),
                String.valueOf(mNote.getCommentId()),
                true,
                new RestRequest.Listener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        if (response != null && !response.optBoolean("success")) {
                            requestCompleted();
                        }
                    }
                }, new RestRequest.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        requestFailed();
                    }
                });
    }


    private void approveComment(){
        if (mNote == null) return;

        // Bump analytics
        AnalyticsTracker.track(AnalyticsTracker.Stat.NOTIFICATION_APPROVED);

        CommentActions.moderateCommentForNote(mNote, CommentStatus.APPROVED, new CommentActions.CommentActionListener() {
            @Override
            public void onActionResult(CommentActionResult result) {
                if (result != null && result.isSuccess()) {

                } else {
                    requestFailed();
                }
            }
        });

    }


    private void replyToComment(){
        if (mNote == null) return;

        // Bump analytics
        AnalyticsTracker.track(AnalyticsTracker.Stat.NOTIFICATION_REPLIED_TO);

        CommentActions.submitReplyToCommentNote(mNote, mReplyText, new CommentActions.CommentActionListener() {
            @Override
            public void onActionResult(CommentActionResult result) {
                if (result != null && result.isSuccess()) {

                } else {
                    requestFailed();
                }
            }
        });

    }

}
