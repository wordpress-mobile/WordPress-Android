package org.wordpress.android.push;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import com.android.volley.VolleyError;
import com.simperium.client.BucketObjectMissingException;
import com.wordpress.rest.RestRequest;

import org.json.JSONObject;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.models.Note;
import org.wordpress.android.ui.notifications.utils.SimperiumUtils;
import org.wordpress.android.util.AppLog;

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
            mNote = obtainNoteFromNoteId(mNoteId);
            if (mNote != null) {
                if (mActionType.equals(ARG_ACTION_LIKE)) {
                    likeComment();
                }
            } else {
                showErrorToUserAndFinish();
            }
        }

        return START_NOT_STICKY;
    }

    /*
     * called when action has been completed successfully
     */
    private void requestCompleted() {
        //EventBus.getDefault().post(new PlanEvents.PlansUpdated(mLocalBlogId, mSitePlans));
    }

    /*
     * called when action failed
     */
    private void requestFailed() {
        //EventBus.getDefault().post(new PlanEvents.PlansUpdateFailed());
    }

    private void showErrorToUserAndFinish() {
        //TODO implement a note to tell the user how this went
    }


    private Note obtainNoteFromNoteId(String noteId) {
        Note note = null;

        if (noteId == null) return null;

        if (SimperiumUtils.getNotesBucket() != null) {
            try {
                note = SimperiumUtils.getNotesBucket().get(noteId);
            } catch (BucketObjectMissingException e) {
                AppLog.e(AppLog.T.NOTIFS, e.getMessage());
                SimperiumUtils.trackBucketObjectMissingWarning(e.getMessage(), noteId);
                showErrorToUserAndFinish();
            }
        }

        return note;
    }


    // Like or unlike a comment via the REST API
    private void likeComment() {
        if (mNote == null) return;

        // Bump analytics
        AnalyticsTracker.track(true ? AnalyticsTracker.Stat.NOTIFICATION_LIKED : AnalyticsTracker.Stat.NOTIFICATION_UNLIKED);

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

}
