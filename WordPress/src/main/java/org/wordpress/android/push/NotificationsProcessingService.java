package org.wordpress.android.push;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
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
import org.wordpress.android.ui.notifications.utils.NotificationsActions;
import org.wordpress.android.ui.notifications.utils.NotificationsUtils;
import org.wordpress.android.util.AppLog;

import java.util.HashMap;

import static org.wordpress.android.push.GCMMessageService.ACTIONS_PROGRESS_NOTIFICATION_ID;
import static org.wordpress.android.push.GCMMessageService.ACTIONS_RESULT_NOTIFICATION_ID;
import static org.wordpress.android.push.GCMMessageService.AUTH_PUSH_NOTIFICATION_ID;
import static org.wordpress.android.push.GCMMessageService.EXTRA_VOICE_OR_INLINE_REPLY;
import static org.wordpress.android.push.GCMMessageService.GROUP_NOTIFICATION_ID;
import static org.wordpress.android.ui.notifications.NotificationsListFragment.NOTE_INSTANT_REPLY_EXTRA;

/**
 * service which makes it possible to process Notifications quick actions in the background,
 * such as:
 * - like
 * - reply-to-comment
 * - approve
 * - 2fa approve & ignore
 */

public class NotificationsProcessingService extends Service {

    public static final String ARG_ACTION_TYPE = "action_type";
    public static final String ARG_ACTION_LIKE = "action_like";
    public static final String ARG_ACTION_REPLY = "action_reply";
    public static final String ARG_ACTION_APPROVE = "action_approve";
    public static final String ARG_ACTION_AUTH_APPROVE = "action_auth_aprove";
    public static final String ARG_ACTION_AUTH_IGNORE = "action_auth_ignore";
    public static final String ARG_ACTION_REPLY_TEXT = "action_reply_text";
    public static final String ARG_NOTE_ID = "note_id";


    //bundle and push ID, as they are held in the system dashboard
    public static final String ARG_NOTE_BUNDLE = "note_bundle";

    /*
    * Use this if you want the service to handle a background note Like.
    * */
    public static void startServiceForLike(Context context, String noteId) {
        Intent intent = new Intent(context, NotificationsProcessingService.class);
        intent.putExtra(ARG_ACTION_TYPE, ARG_ACTION_LIKE);
        intent.putExtra(ARG_NOTE_ID, noteId);
        context.startService(intent);
    }

    /*
    * Use this if you want the service to handle a background note Approve.
    * */
    public static void startServiceForApprove(Context context, String noteId) {
        Intent intent = new Intent(context, NotificationsProcessingService.class);
        intent.putExtra(ARG_ACTION_TYPE, ARG_ACTION_APPROVE);
        intent.putExtra(ARG_NOTE_ID, noteId);
        context.startService(intent);
    }

    /*
    * Use this if you want the service to handle a background note reply.
    * */
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

        // Offload to a separate thread.
        final QuickActionProcessor proc = new QuickActionProcessor(this, intent, startId);
        new Thread(new Runnable() {
            public void run() {
                proc.process();
            }
        }).start();

        return START_NOT_STICKY;
    }

    private class QuickActionProcessor {
        private String mNoteId;
        private String mReplyText;
        private String mActionType;
        private int mPushId;
        private Note mNote;
        private final int mTaskId;
        private final Context mContext;
        private final Intent mIntent;

        public QuickActionProcessor(Context ctx, Intent intent, int taskId) {
            mContext = ctx;
            mIntent = intent;
            mTaskId = taskId;
        }

        public void process() {

            getDataFromIntent();

            //now handle each action
            if (mActionType != null) {

                // check special cases for authorization push
                if (mActionType.equals(ARG_ACTION_AUTH_IGNORE)) {
                    //dismiss notifs
                    dismissNotification(ACTIONS_RESULT_NOTIFICATION_ID);
                    dismissNotification(AUTH_PUSH_NOTIFICATION_ID);
                    dismissNotification(ACTIONS_PROGRESS_NOTIFICATION_ID);
                    GCMMessageService.removeNotification(AUTH_PUSH_NOTIFICATION_ID);

                    AnalyticsTracker.track(AnalyticsTracker.Stat.PUSH_AUTHENTICATION_IGNORED);
                    return;
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && mActionType.equals(ARG_ACTION_REPLY)) {
                    //we don't need showing the infinite progress bar in case of REPLY on Android N,
                    //because we've got inline-reply there with its own spinner to show progress
                    // no op
                } else {
                    showIntermediateMessageToUser(getProcessingTitleForAction(mActionType));
                }

                //if we still don't have a Note, go get it from the REST API
                if (mNote == null) {
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
                    //we have a Note! just go ahead and perform the requested action
                    performRequestedAction();
                }
            } else {
                requestFailed(null);
            }

        }

        private void getNoteFromBundleIfExists() {
            if (mIntent.hasExtra(ARG_NOTE_BUNDLE)) {
                Bundle payload = mIntent.getBundleExtra(ARG_NOTE_BUNDLE);
                mNote = NotificationsUtils.buildNoteObjectFromBundle(payload);
            }
        }

        private void getDataFromIntent() {
            // get all needed data from intent
            mNoteId = mIntent.getStringExtra(ARG_NOTE_ID);
            mActionType = mIntent.getStringExtra(ARG_ACTION_TYPE);
            //default value for push notification ID is likely GROUP_NOTIFICATION_ID for the only
            // notif in active notifs map (there is only one notif if quick actions are available)
            mPushId = GROUP_NOTIFICATION_ID;
            if (mIntent.hasExtra(ARG_ACTION_REPLY_TEXT)) {
                mReplyText = mIntent.getStringExtra(ARG_ACTION_REPLY_TEXT);
            }

            if (TextUtils.isEmpty(mReplyText)) {
                //if voice reply is enabled in a wearable, it will come through the remoteInput
                //extra EXTRA_VOICE_OR_INLINE_REPLY
                //same thing with direct-reply in Android 7
                Bundle remoteInput = RemoteInput.getResultsFromIntent(mIntent);
                if (remoteInput != null) {
                    obtainReplyTextFromRemoteInputBundle(remoteInput);
                }
            }

            //we probably have the note in the PN payload and such it's passed in the intent extras
            // bundle. If we have it, no need to go fetch it through REST API.
            getNoteFromBundleIfExists();
        }

        private String getProcessingTitleForAction(String actionType) {
            if (actionType != null) {
                switch (actionType) {
                    case ARG_ACTION_LIKE:
                        return getString(R.string.comment_q_action_liking);
                    case ARG_ACTION_APPROVE:
                        return getString(R.string.comment_q_action_approving);
                    case ARG_ACTION_REPLY:
                        return getString(R.string.comment_q_action_replying);
                    default:
                        //default, generic  "processing"
                        return getString(R.string.comment_q_action_processing);
                }
            } else {
                //default, generic  "processing"
                return getString(R.string.comment_q_action_processing);
            }
        }

        private void obtainReplyTextFromRemoteInputBundle(Bundle bundle) {
            CharSequence replyText = bundle.getCharSequence(EXTRA_VOICE_OR_INLINE_REPLY);
            if (replyText != null) {
                mReplyText = replyText.toString();
            }
        }

        private void buildNoteFromJSONObject(JSONObject jsonObject) {
            try {
                if (jsonObject.has("notes")) {
                    JSONArray jsonArray = jsonObject.getJSONArray("notes");
                    if (jsonArray != null && jsonArray.length() == 1) {
                        jsonObject = jsonArray.getJSONObject(0);
                    }
                }
                mNote = new Note(mNoteId, jsonObject);

            } catch (JSONException e) {
                AppLog.e(AppLog.T.NOTIFS, e.getMessage());
            }
        }

        private void performRequestedAction(){
            /*********************************************************/
            /* possible actions are Comment REPLY, APPROVE, and LIKE */
            /*********************************************************/
            if (mNote != null) {
                if (mActionType != null) {
                    switch (mActionType) {
                        case ARG_ACTION_LIKE:
                            likeComment();
                            break;
                        case ARG_ACTION_APPROVE:
                            approveComment();
                            break;
                        case ARG_ACTION_REPLY:
                            replyToComment();
                            break;
                        default:
                            //no op
                            requestFailed(null);
                            break;
                    }
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

            NotificationsActions.markNoteAsRead(mNote);

            //dismiss any other pending result notification
            dismissNotification(ACTIONS_RESULT_NOTIFICATION_ID);
            //update notification indicating the operation succeeded
            showFinalMessageToUser(successMessage, ACTIONS_PROGRESS_NOTIFICATION_ID);
            //remove the original notification from the system bar
            GCMMessageService.removeNotificationWithNoteIdFromSystemBar(mContext, mNoteId);

            //after 3 seconds, dismiss the notification that indicated success
            Handler handler = new Handler(getMainLooper());
            handler.postDelayed(new Runnable() {
                public void run() {
                    dismissNotification(ACTIONS_PROGRESS_NOTIFICATION_ID);
                }}, 3000); // show the success message for 3 seconds, then dismiss

            stopSelf(mTaskId);
        }

        /*
         * called when action failed
         */
        private void requestFailed(String actionType) {
            String errorMessage = null;
            if (actionType != null) {
                if (actionType.equals(ARG_ACTION_LIKE)) {
                    errorMessage = getString(R.string.error_notif_q_action_like);
                } else if (actionType.equals(ARG_ACTION_APPROVE)) {
                    errorMessage = getString(R.string.error_notif_q_action_approve);
                } else if (actionType.equals(ARG_ACTION_REPLY)) {
                    errorMessage = getString(R.string.error_notif_q_action_reply);
                }
            } else {
                //show generic error here
                errorMessage = getString(R.string.error_generic);
            }
            resetOriginalNotification();
            dismissNotification(ACTIONS_PROGRESS_NOTIFICATION_ID);
            showFinalMessageToUser(errorMessage, ACTIONS_RESULT_NOTIFICATION_ID);

            //after 3 seconds, dismiss the error message notification
            Handler handler = new Handler(getMainLooper());
            handler.postDelayed(new Runnable() {
                public void run() {
                    //remove the error notification from the system bar
                    dismissNotification(ACTIONS_RESULT_NOTIFICATION_ID);
                }}, 3000); // show the success message for 3 seconds, then dismiss

            stopSelf(mTaskId);
        }

        private void requestFailedWithMessage(String errorMessage, boolean autoDismiss) {
            if (errorMessage == null) {
                //show generic error here
                errorMessage = getString(R.string.error_generic);
            }
            resetOriginalNotification();
            showFinalMessageToUser(errorMessage, ACTIONS_RESULT_NOTIFICATION_ID);

            if (autoDismiss) {
                //after 3 seconds, dismiss the error message notification
                Handler handler = new Handler(getMainLooper());
                handler.postDelayed(new Runnable() {
                    public void run() {
                        //remove the error notification from the system bar
                        dismissNotification(ACTIONS_RESULT_NOTIFICATION_ID);
                    }}, 3000); // show the success message for 3 seconds, then dismiss
            }

            stopSelf(mTaskId);
        }

        private void showIntermediateMessageToUser(String message) {
            showMessageToUser(message, true, ACTIONS_PROGRESS_NOTIFICATION_ID);
        }

        private void showFinalMessageToUser(String message, int pushId) {
            showMessageToUser(message, false, pushId);
        }

        private void showMessageToUser(String message, boolean intermediateMessage, int pushId) {
            NotificationCompat.Builder builder = getBuilder().setContentText(message).setTicker(message);
            if (!intermediateMessage) {
                builder.setStyle(new NotificationCompat.BigTextStyle().bigText(message));
            }
            builder.setProgress(0, 0, intermediateMessage);

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(mContext);
            notificationManager.notify(pushId, builder.build());
        }

        private NotificationCompat.Builder getBuilder() {
            return new NotificationCompat.Builder(mContext)
                    .setSmallIcon(R.drawable.notification_icon)
                    .setColor(getResources().getColor(R.color.blue_wordpress))
                    .setContentTitle(getString(R.string.app_name))
                    .setAutoCancel(true);
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
            AnalyticsTracker.track(AnalyticsTracker.Stat.NOTIFICATION_QUICK_ACTIONS_LIKED);

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
            AnalyticsTracker.track(AnalyticsTracker.Stat.NOTIFICATION_QUICK_ACTIONS_APPROVED);

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
            AnalyticsTracker.track(AnalyticsTracker.Stat.NOTIFICATION_QUICK_ACTIONS_REPLIED_TO);


            if (!TextUtils.isEmpty(mReplyText)) {
                CommentActions.submitReplyToCommentNote(mNote, mReplyText, new CommentActions.CommentActionListener() {
                    @Override
                    public void onActionResult(CommentActionResult result) {
                        if (result != null && result.isSuccess()) {
                            requestCompleted(ARG_ACTION_REPLY);
                        } else {
                            if (result != null && !TextUtils.isEmpty(result.getMessage())) {
                                requestFailedWithMessage(result.getMessage(), true);
                            } else {
                                requestFailed(ARG_ACTION_REPLY);
                            }
                        }
                    }
                });
            } else {
                //cancel the current notification
                dismissNotification(mPushId);
                hideStatusBar();
                //and just trigger the Activity to allow the user to write a reply
                startReplyToCommentActivity();
            }
        }

        private void dismissNotification(int pushId) {
            final NotificationManagerCompat notificationManager = NotificationManagerCompat.from(mContext);
            notificationManager.cancel(pushId);
        }

        private void startReplyToCommentActivity() {
            Intent intent = new Intent(mContext, WPMainActivity.class);
            intent.putExtra(WPMainActivity.ARG_OPENED_FROM_PUSH, true);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            intent.setAction("android.intent.action.MAIN");
            intent.addCategory("android.intent.category.LAUNCHER");
            intent.putExtra(NotificationsListFragment.NOTE_ID_EXTRA, mNoteId);
            intent.putExtra(NOTE_INSTANT_REPLY_EXTRA, true);
            startActivity(intent);
        }

        private void hideStatusBar() {
            Intent closeIntent = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
            sendBroadcast(closeIntent);
        }

        private void resetOriginalNotification(){
            GCMMessageService.rebuildAndUpdateNotificationsOnSystemBarForThisNote(mContext, mNoteId);
        }
    }
}
