package org.wordpress.android.push;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.RemoteInput;
import android.text.TextUtils;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.models.CommentStatus;
import org.wordpress.android.models.Note;
import org.wordpress.android.ui.comments.CommentActionResult;
import org.wordpress.android.ui.comments.CommentActions;
import org.wordpress.android.ui.main.WPMainActivity;
import org.wordpress.android.ui.notifications.NotificationsListFragment;
import org.wordpress.android.util.AppLog;

import java.util.HashMap;

import static org.wordpress.android.push.GCMMessageService.ACTIONS_RESULT_NOTIFICATION_ID;
import static org.wordpress.android.push.GCMMessageService.EXTRA_VOICE_REPLY;
import static org.wordpress.android.ui.notifications.NotificationsListFragment.NOTE_INSTANT_REPLY_EXTRA;

/**
 * service which makes it possible to process Notifications quick actions in the background,
 * such as:
 * - like
 * - reply-to-comment
 * - approve
 */

public class NotificationsProcessingService extends Service {

    public static final String ARG_ACTION_TYPE = "action_type";
    public static final String ARG_ACTION_LIKE = "action_like";
    public static final String ARG_ACTION_REPLY = "action_reply";
    public static final String ARG_ACTION_APPROVE = "action_approve";
    public static final String ARG_ACTION_REPLY_TEXT = "action_reply_text";
    public static final String ARG_NOTE_ID = "note_id";
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

        if (TextUtils.isEmpty(mReplyText)) {
            //if voice reply is enabled in a wearable, it will come through the remoteInput
            //extra EXTRA_VOICE_REPLY
            Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
            if (remoteInput != null) {
                CharSequence replyText = remoteInput.getCharSequence(EXTRA_VOICE_REPLY);
                if (replyText != null) {
                    mReplyText = replyText.toString();
                }
            }
        }

        showIntermediateMessageToUser(getString(R.string.comment_q_action_updating));

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
                            requestFailed(mActionType);
                        }
                    };

            getNoteFromNoteId(mNoteId, listener, errorListener);

        } else {
            requestFailed(null);
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
                // add other actions here
                //no op
                requestFailed(null);
            }
        } else {
            requestFailed(null);
        }
    }

    /*
     * called when action has been completed successfully
     */
    private void requestCompleted(String actionType) {
        String successMessage = null;
        if (actionType != null) {
            if (actionType.equals(ARG_ACTION_LIKE)) {
                successMessage = getString(R.string.comment_liked);
            } else if (actionType.equals(ARG_ACTION_APPROVE)) {
                successMessage = getString(R.string.comment_moderated_approved);
            } else if (actionType.equals(ARG_ACTION_REPLY)) {
                successMessage = getString(R.string.note_reply_successful);
            }
        } else {
            //show generic success message here
            successMessage = getString(R.string.comment_q_action_done_generic);
        }

        //show temporal notification indicating the operation succeeded
        showFinalMessageToUser(successMessage);

        //remove the original notification from the system bar
        GCMMessageService.removeNotificationWithNoteIdFromSystemBar(this, mNoteId);

        //after 3 seconds, dismiss the temporal notification that indicated success
        Handler handler = new Handler();
        final NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        handler.postDelayed(new Runnable() {
            public void run() {
                notificationManager.cancel(ACTIONS_RESULT_NOTIFICATION_ID);
            }}, 3000); // show the success message for 3 seconds, then dismiss
    }

    /*
     * called when action failed
     */
    private void requestFailed(String actionType) {
        String errorMessage = null;
        if (actionType != null) {
            if (actionType.equals(ARG_ACTION_LIKE)) {
                errorMessage = getString(R.string.error_generic);
            } else if (actionType.equals(ARG_ACTION_APPROVE)) {
                errorMessage = getString(R.string.error_notif_q_action_approve);
            } else if (actionType.equals(ARG_ACTION_REPLY)) {
                errorMessage = getString(R.string.error_notif_q_action_reply);
            }
        } else {
            //show generic error here
            errorMessage = getString(R.string.error_generic);
        }
        showFinalMessageToUser(errorMessage);
    }

    private void showIntermediateMessageToUser(String message) {
        showMessageToUser(message, true);
    }

    private void showFinalMessageToUser(String message) {
        showMessageToUser(message, false);
    }

    private void showMessageToUser(String message, boolean intermediateMessage) {
        NotificationCompat.Builder builder = getBuilder().setContentText(message).setTicker(message);
        builder.setProgress(0, 0, intermediateMessage);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(ACTIONS_RESULT_NOTIFICATION_ID, builder.build());
    }

    private NotificationCompat.Builder getBuilder() {
        String title = getString(R.string.app_name);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.notification_icon)
                .setColor(getResources().getColor(R.color.blue_wordpress))
                .setContentTitle(title)
                .setAutoCancel(true);
                //.setStyle(new NotificationCompat.BigTextStyle().bigText(message));
        return builder;
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
                        if (response != null && response.optBoolean("success")) {
                            requestCompleted(ARG_ACTION_LIKE);
                        }
                    }
                }, new RestRequest.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        requestFailed(ARG_ACTION_LIKE);
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
                    requestCompleted(ARG_ACTION_APPROVE);
                } else {
                    requestFailed(ARG_ACTION_APPROVE);
                }
            }
        });

    }

    private void replyToComment(){
        if (mNote == null) return;

        // Bump analytics
        AnalyticsTracker.track(AnalyticsTracker.Stat.NOTIFICATION_REPLIED_TO);


        if (!TextUtils.isEmpty(mReplyText)) {
            CommentActions.submitReplyToCommentNote(mNote, mReplyText, new CommentActions.CommentActionListener() {
                @Override
                public void onActionResult(CommentActionResult result) {
                    if (result != null && result.isSuccess()) {
                        requestCompleted(ARG_ACTION_REPLY);
                    } else {
                        requestFailed(ARG_ACTION_REPLY);
                    }
                }
            });
        } else {
            // just trigger the Activity to allow the user to write a reply
            startReplyToCommentActivity();
        }
    }

    private void startReplyToCommentActivity() {
        Intent intent = new Intent(this, WPMainActivity.class);
        intent.putExtra(WPMainActivity.ARG_OPENED_FROM_PUSH, true);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.setAction("android.intent.action.MAIN");
        intent.addCategory("android.intent.category.LAUNCHER");
        intent.putExtra(NotificationsListFragment.NOTE_ID_EXTRA, mNoteId);
        intent.putExtra(NOTE_INSTANT_REPLY_EXTRA, true);
        startActivity(intent);
    }

}
