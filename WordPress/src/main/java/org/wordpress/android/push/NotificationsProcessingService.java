package org.wordpress.android.push;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.text.TextUtils;

import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.RemoteInput;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.analytics.AnalyticsTracker.Stat;
import org.wordpress.android.fluxc.action.CommentAction;
import org.wordpress.android.fluxc.generated.CommentActionBuilder;
import org.wordpress.android.fluxc.model.CommentModel;
import org.wordpress.android.fluxc.model.CommentStatus;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.CommentStore.OnCommentChanged;
import org.wordpress.android.fluxc.store.CommentStore.RemoteCommentPayload;
import org.wordpress.android.fluxc.store.CommentStore.RemoteCreateCommentPayload;
import org.wordpress.android.fluxc.store.CommentStore.RemoteLikeCommentPayload;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.models.Note;
import org.wordpress.android.ui.comments.unified.CommentsStoreAdapter;
import org.wordpress.android.ui.notifications.SystemNotificationsTracker;
import org.wordpress.android.ui.notifications.receivers.NotificationsPendingDraftsReceiver;
import org.wordpress.android.ui.notifications.utils.NotificationsActions;
import org.wordpress.android.ui.notifications.utils.NotificationsUtils;
import org.wordpress.android.ui.notifications.utils.PendingDraftsNotificationsUtils;
import org.wordpress.android.ui.quickstart.QuickStartTracker;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.LocaleManager;
import org.wordpress.android.util.analytics.AnalyticsUtils;
import org.wordpress.android.util.analytics.AnalyticsUtils.AnalyticsCommentActionSource;
import org.wordpress.android.util.analytics.AnalyticsUtils.QuickActionTrackPropertyValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;

import static org.wordpress.android.push.NotificationPushIds.GROUP_NOTIFICATION_ID;
import static org.wordpress.android.push.NotificationPushIds.QUICK_START_REMINDER_NOTIFICATION_ID;

/**
 * service which makes it possible to process Notifications quick actions in the background,
 * such as:
 * - like
 * - reply-to-comment
 * - approve
 * - 2fa approve & ignore
 * - pending draft notification ignore & dismissal
 */

public class NotificationsProcessingService extends Service {
    public static final String ARG_ACTION_TYPE = "action_type";
    public static final String ARG_ACTION_LIKE = "action_like";
    public static final String ARG_ACTION_REPLY = "action_reply";
    public static final String ARG_ACTION_APPROVE = "action_approve";
    public static final String ARG_ACTION_AUTH_APPROVE = "action_auth_aprove";
    public static final String ARG_ACTION_AUTH_IGNORE = "action_auth_ignore";
    public static final String ARG_ACTION_DRAFT_PENDING_DISMISS = "action_draft_pending_dismiss";
    public static final String ARG_ACTION_DRAFT_PENDING_IGNORE = "action_draft_pending_ignore";
    public static final String ARG_ACTION_REPLY_TEXT = "action_reply_text";
    public static final String ARG_ACTION_NOTIFICATION_DISMISS = "action_dismiss";
    public static final String ARG_NOTE_ID = "note_id";
    public static final String ARG_PUSH_ID = "notificationId";
    public static final String ARG_NOTIFICATION_TYPE = "notificationType";

    // bundle and push ID, as they are held in the system dashboard
    public static final String ARG_NOTE_BUNDLE = "note_bundle";

    private QuickActionProcessor mQuickActionProcessor;
    private List<Long> mActionedCommentsRemoteIds = new ArrayList<>();

    @Inject CommentsStoreAdapter mCommentsStoreAdapter;
    @Inject SiteStore mSiteStore;
    @Inject SystemNotificationsTracker mSystemNotificationsTracker;
    @Inject GCMMessageHandler mGCMMessageHandler;
    @Inject QuickStartTracker mQuickStartTracker;

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

    public static PendingIntent getPendingIntentForNotificationDismiss(Context context, int pushId,
                                                                       NotificationType notificationType) {
        Intent intent = new Intent(context, NotificationsProcessingService.class);
        intent.putExtra(ARG_ACTION_TYPE, ARG_ACTION_NOTIFICATION_DISMISS);
        intent.putExtra(ARG_PUSH_ID, pushId);
        intent.putExtra(ARG_NOTIFICATION_TYPE, notificationType);
        intent.addCategory(ARG_ACTION_NOTIFICATION_DISMISS);

        return PendingIntent
                .getService(context, pushId, intent, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    public static void stopService(Context context) {
        if (context == null) {
            return;
        }

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
        ((WordPress) getApplication()).component().inject(this);
        mCommentsStoreAdapter.register(this);
        AppLog.i(AppLog.T.NOTIFS, "notifications action processing service > created");
    }

    @Override
    public void onDestroy() {
        AppLog.i(AppLog.T.NOTIFS, "notifications action processing service > destroyed");
        mCommentsStoreAdapter.unregister(this);
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Offload to a separate thread.

        mQuickActionProcessor =
                new QuickActionProcessor(this, mSystemNotificationsTracker, mGCMMessageHandler, intent, startId);
        new Thread(() -> mQuickActionProcessor.process()).start();

        return START_NOT_STICKY;
    }

    private class QuickActionProcessor {
        private SystemNotificationsTracker mSystemNotificationsTracker;
        private GCMMessageHandler mGCMMessageHandler;
        private String mNoteId;
        private String mReplyText;
        private String mActionType;
        private NotificationType mNotificationType;
        private int mPushId;
        private Note mNote;
        private final int mTaskId;
        private final Context mContext;
        private final Intent mIntent;

        QuickActionProcessor(Context ctx, SystemNotificationsTracker notificationsTracker,
                             GCMMessageHandler gcmMessageHandler, Intent intent, int taskId) {
            mContext = ctx;
            mSystemNotificationsTracker = notificationsTracker;
            mIntent = intent;
            mTaskId = taskId;
            this.mGCMMessageHandler = gcmMessageHandler;
        }

        public void process() {
            getDataFromIntent();

            // now handle each action
            if (mActionType != null) {
                // check special cases for authorization push
                if (mActionType.equals(ARG_ACTION_AUTH_IGNORE)) {
                    // dismiss notifs
                    NativeNotificationsUtils.dismissNotification(
                            NotificationPushIds.ACTIONS_RESULT_NOTIFICATION_ID, mContext);
                    NativeNotificationsUtils.dismissNotification(
                            NotificationPushIds.AUTH_PUSH_NOTIFICATION_ID, mContext);
                    NativeNotificationsUtils.dismissNotification(
                            NotificationPushIds.ACTIONS_PROGRESS_NOTIFICATION_ID, mContext);
                    mGCMMessageHandler.removeNotification(NotificationPushIds.AUTH_PUSH_NOTIFICATION_ID);

                    AnalyticsTracker.track(AnalyticsTracker.Stat.PUSH_AUTHENTICATION_IGNORED);
                    return;
                }

                // check notification dismissed pending intent
                if (mActionType.equals(ARG_ACTION_NOTIFICATION_DISMISS)) {
                    if (mNotificationType != null) {
                        mSystemNotificationsTracker.trackDismissedNotification(mNotificationType);
                    }
                    int notificationId = mIntent.getIntExtra(ARG_PUSH_ID, 0);
                    if (notificationId == GROUP_NOTIFICATION_ID) {
                        mGCMMessageHandler.clearNotifications();
                    } else if (notificationId == QUICK_START_REMINDER_NOTIFICATION_ID) {
                        mQuickStartTracker.track(Stat.QUICK_START_NOTIFICATION_DISMISSED);
                    } else {
                        mGCMMessageHandler.removeNotification(notificationId);
                        // Dismiss the grouped notification if a user dismisses all notifications from a wear device
                        if (!mGCMMessageHandler.hasNotifications()) {
                            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(mContext);
                            notificationManager.cancel(GROUP_NOTIFICATION_ID);
                        }
                    }
                    return;
                }

                // check special cases for pending draft notifications - ignore
                if (mActionType.equals(ARG_ACTION_DRAFT_PENDING_IGNORE)) {
                    // dismiss notif
                    int postId = mIntent.getIntExtra(NotificationsPendingDraftsReceiver.POST_ID_EXTRA, 0);
                    if (postId != 0) {
                        NativeNotificationsUtils.dismissNotification(
                                PendingDraftsNotificationsUtils.makePendingDraftNotificationId(postId),
                                mContext
                        );
                    }
                    AnalyticsTracker.track(AnalyticsTracker.Stat.NOTIFICATION_PENDING_DRAFTS_IGNORED);
                    return;
                }

                // check special cases for pending draft notifications - dismiss
                if (mActionType.equals(ARG_ACTION_DRAFT_PENDING_DISMISS)) {
                    AnalyticsTracker.track(AnalyticsTracker.Stat.NOTIFICATION_PENDING_DRAFTS_DISMISSED);
                    return;
                }

                if (mActionType.equals(ARG_ACTION_REPLY)) {
                    // we don't need showing the infinite progress bar in case of REPLY on Android N,
                    // because we've got inline-reply there with its own spinner to show progress
                    // no op
                } else {
                    NativeNotificationsUtils
                            .showIntermediateMessageToUser(getProcessingTitleForAction(mActionType), mContext,
                                    mNotificationType);
                }

                // if we still don't have a Note, go get it from the REST API
                if (mNote == null) {
                    RestRequest.Listener listener =
                            new RestRequest.Listener() {
                                @Override
                                public void onResponse(JSONObject response) {
                                    if (response != null && !response.optBoolean("success")) {
                                        // build the Note object here
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
                    // we have a Note! just go ahead and perform the requested action
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
            if (mIntent == null) {
                return;
            }
            // get all needed data from intent
            mNoteId = mIntent.getStringExtra(ARG_NOTE_ID);
            mActionType = mIntent.getStringExtra(ARG_ACTION_TYPE);
            // default value for push notification ID is likely GROUP_NOTIFICATION_ID for the only
            // notif in active notifs map (there is only one notif if quick actions are available)
            mPushId = GROUP_NOTIFICATION_ID;
            if (mIntent.hasExtra(ARG_ACTION_REPLY_TEXT)) {
                mReplyText = mIntent.getStringExtra(ARG_ACTION_REPLY_TEXT);
            }
            if (mIntent.hasExtra(ARG_NOTIFICATION_TYPE)) {
                mNotificationType = (NotificationType) mIntent.getSerializableExtra(ARG_NOTIFICATION_TYPE);
            }

            if (TextUtils.isEmpty(mReplyText)) {
                // if voice reply is enabled in a wearable, it will come through the remoteInput
                // extra EXTRA_VOICE_OR_INLINE_REPLY
                // same thing with direct-reply in Android 7
                Bundle remoteInput = RemoteInput.getResultsFromIntent(mIntent);
                if (remoteInput != null) {
                    obtainReplyTextFromRemoteInputBundle(remoteInput);
                }
            }

            // we probably have the note in the PN payload and such it's passed in the intent extras
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
                        // default, generic "processing"
                        return getString(R.string.comment_q_action_processing);
                }
            } else {
                // default, generic "processing"
                return getString(R.string.comment_q_action_processing);
            }
        }

        private void obtainReplyTextFromRemoteInputBundle(Bundle bundle) {
            CharSequence replyText = bundle.getCharSequence(GCMMessageService.EXTRA_VOICE_OR_INLINE_REPLY);
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

        private void performRequestedAction() {
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
                            // no op
                            requestFailed(null);
                            break;
                    }
                } else {
                    // add other actions here
                    // no op
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
                // show generic success message here
                successMessage = getString(R.string.comment_q_action_done_generic);
            }

            NotificationsActions.markNoteAsRead(mNote);

            // dismiss any other pending result notification
            NativeNotificationsUtils.dismissNotification(
                    NotificationPushIds.ACTIONS_RESULT_NOTIFICATION_ID, mContext);
            // update notification indicating the operation succeeded
            NativeNotificationsUtils.showFinalMessageToUser(successMessage,
                    NotificationPushIds.ACTIONS_PROGRESS_NOTIFICATION_ID,
                    mContext,
                    NotificationType.ACTIONS_PROGRESS);
            // remove the original notification from the system bar
            mGCMMessageHandler.removeNotificationWithNoteIdFromSystemBar(mContext, mNoteId);

            // after 3 seconds, dismiss the notification that indicated success
            Handler handler = new Handler(getMainLooper());
            handler.postDelayed(new Runnable() {
                public void run() {
                    NativeNotificationsUtils.dismissNotification(
                            NotificationPushIds.ACTIONS_PROGRESS_NOTIFICATION_ID, mContext);
                }
            }, 3000); // show the success message for 3 seconds, then dismiss

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
                // show generic error here
                errorMessage = getString(R.string.error_generic);
            }
            resetOriginalNotification();
            NativeNotificationsUtils.dismissNotification(
                    NotificationPushIds.ACTIONS_PROGRESS_NOTIFICATION_ID, mContext);
            NativeNotificationsUtils
                    .showFinalMessageToUser(errorMessage, NotificationPushIds.ACTIONS_RESULT_NOTIFICATION_ID, mContext,
                            NotificationType.ACTIONS_RESULT);

            // after 3 seconds, dismiss the error message notification
            Handler handler = new Handler(getMainLooper());
            handler.postDelayed(new Runnable() {
                public void run() {
                    // remove the error notification from the system bar
                    NativeNotificationsUtils.dismissNotification(
                            NotificationPushIds.ACTIONS_RESULT_NOTIFICATION_ID, mContext);
                }
            }, 3000); // show the success message for 3 seconds, then dismiss

            stopSelf(mTaskId);
        }

        private void requestFailedWithMessage(String errorMessage, boolean autoDismiss) {
            if (errorMessage == null) {
                // show generic error here
                errorMessage = getString(R.string.error_generic);
            }
            resetOriginalNotification();
            NativeNotificationsUtils
                    .showFinalMessageToUser(errorMessage, NotificationPushIds.ACTIONS_RESULT_NOTIFICATION_ID, mContext,
                            NotificationType.ACTIONS_RESULT);

            if (autoDismiss) {
                // after 3 seconds, dismiss the error message notification
                Handler handler = new Handler(getMainLooper());
                handler.postDelayed(new Runnable() {
                    public void run() {
                        // remove the error notification from the system bar
                        NativeNotificationsUtils.dismissNotification(
                                NotificationPushIds.ACTIONS_RESULT_NOTIFICATION_ID, mContext);
                    }
                }, 3000); // show the success message for 3 seconds, then dismiss
            }

            stopSelf(mTaskId);
        }

        private void keepRemoteCommentIdForPostProcessing(long remoteCommendId) {
            if (!mActionedCommentsRemoteIds.contains(remoteCommendId)) {
                mActionedCommentsRemoteIds.add(remoteCommendId);
            }
        }

        private void getNoteFromNoteId(String noteId, RestRequest.Listener listener,
                                       RestRequest.ErrorListener errorListener) {
            if (noteId == null) {
                return;
            }

            HashMap<String, String> params = new HashMap<>();
            params.put("locale", LocaleManager.getLanguage(mContext));
            WordPress.getRestClientUtils().getNotification(params, noteId, listener, errorListener);
        }

        // Like or unlike a comment via the REST API
        private void likeComment() {
            if (mNote == null) {
                requestFailed(ARG_ACTION_LIKE);
                return;
            }

            SiteModel site = mSiteStore.getSiteBySiteId(mNote.getSiteId());

            // Bump analytics
            // TODO klymyam remove legacy comment tracking after new comments are shipped and new funnels are made
            AnalyticsUtils.trackWithBlogPostDetails(
                    AnalyticsTracker.Stat.NOTIFICATION_QUICK_ACTIONS_LIKED, mNote.getSiteId(), mNote.getPostId());
            AnalyticsUtils.trackCommentActionWithSiteDetails(Stat.COMMENT_QUICK_ACTION_LIKED,
                    AnalyticsCommentActionSource.NOTIFICATIONS, site);
            AnalyticsUtils.trackQuickActionTouched(
                    QuickActionTrackPropertyValue.LIKE,
                    site,
                    mNote.buildComment());

            if (site != null) {
                mCommentsStoreAdapter.dispatch(CommentActionBuilder.newLikeCommentAction(
                        new RemoteLikeCommentPayload(site, mNote.getCommentId(), true)));
            } else {
                requestFailed(ARG_ACTION_LIKE);
                AppLog.e(T.NOTIFS, "Site with id: " + mNote.getSiteId() + " doesn't exist in the Site store");
            }
        }

        private void approveComment() {
            if (mNote == null) {
                requestFailed(ARG_ACTION_APPROVE);
                return;
            }

            SiteModel site = mSiteStore.getSiteBySiteId(mNote.getSiteId());

            // Bump analytics
            // TODO klymyam remove legacy comment tracking after new comments are shipped and new funnels are made
            AnalyticsUtils.trackWithBlogPostDetails(
                    AnalyticsTracker.Stat.NOTIFICATION_QUICK_ACTIONS_APPROVED, mNote.getSiteId(), mNote.getPostId());
            AnalyticsUtils.trackCommentActionWithSiteDetails(Stat.COMMENT_QUICK_ACTION_APPROVED,
                    AnalyticsCommentActionSource.NOTIFICATIONS, site);

            AnalyticsUtils.trackQuickActionTouched(
                    QuickActionTrackPropertyValue.APPROVE,
                    site,
                    mNote.buildComment());

            // Update pseudo comment (built from the note)
            CommentModel comment = mNote.buildComment();
            comment.setStatus(CommentStatus.APPROVED.toString());

            if (site == null) {
                AppLog.e(T.NOTIFS, "Impossible to approve a comment on a site that is not in the App. SiteId: "
                                   + mNote.getSiteId());
                requestFailed(ARG_ACTION_APPROVE);
                return;
            }

            // keep the CommentId, we'll use it later to know whether to trigger the end processing notification
            // or not
            keepRemoteCommentIdForPostProcessing(comment.getRemoteCommentId());

            // Push the comment
            mCommentsStoreAdapter.dispatch(CommentActionBuilder
                    .newPushCommentAction(new RemoteCommentPayload(site, comment)));
        }

        private void replyToComment() {
            if (mNote == null) {
                requestFailed(ARG_ACTION_APPROVE);
                return;
            }

            if (TextUtils.isEmpty(mReplyText)) return;

            SiteModel site = mSiteStore.getSiteBySiteId(mNote.getSiteId());
            if (site == null) {
                AppLog.e(T.NOTIFS, "Impossible to reply to a comment on a site that is not in the App."
                                   + " SiteId: " + mNote.getSiteId());
                requestFailed(ARG_ACTION_APPROVE);
                return;
            }

            // Pseudo comment (built from the note)
            CommentModel comment = mNote.buildComment();

            // Pseudo comment reply
            CommentModel reply = new CommentModel();
            reply.setContent(mReplyText);

            // Push the reply
            RemoteCreateCommentPayload payload = new RemoteCreateCommentPayload(site, comment, reply);
            mCommentsStoreAdapter.dispatch(CommentActionBuilder.newCreateNewCommentAction(payload));

            // Bump analytics
            AnalyticsUtils.trackCommentReplyWithDetails(true,
                    site, comment, AnalyticsCommentActionSource.NOTIFICATIONS);
            AnalyticsUtils.trackQuickActionTouched(QuickActionTrackPropertyValue.REPLY_TO, site, comment);
        }

        private void resetOriginalNotification() {
            mGCMMessageHandler.rebuildAndUpdateNotificationsOnSystemBarForThisNote(mContext, mNoteId);
        }
    }

    // OnChanged events

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onCommentChanged(OnCommentChanged event) {
        if (mQuickActionProcessor == null) {
            return;
        }

        // LIKE_COMMENT events do not hold event.changedCommentsLocalIds info so, just process them
        // also CREATE_NEW_COMMENT, which are received for REPLY quick actions won't have a matching id as FluxC
        // only notifies us of _new_ comments (the actual reply) so, no way to connect one another in this place.
        // Therefore, we only care and match for `PUSH_COMMENT` to make sure there's a corresponding initial APPROVE
        // quick action that corresponds to the resulting FluxC event.
        if (event.causeOfChange == CommentAction.PUSH_COMMENT) {
            List<Long> eventChangedCommentsRemoteIds = new ArrayList<>();
            if (mActionedCommentsRemoteIds.size() > 0) {
                // prepare a comparable list of Ids
                for (Integer commentLocalId : event.changedCommentsLocalIds) {
                    CommentModel localComment = mCommentsStoreAdapter.getCommentByLocalId(commentLocalId);
                    if (localComment != null) {
                        eventChangedCommentsRemoteIds.add(localComment.getRemoteCommentId());
                    }
                }

                // here we need to check ids: is an event corresponding to an action triggered from this Service?
                for (Long oneEventCommentRemoteId : eventChangedCommentsRemoteIds) {
                    if (mActionedCommentsRemoteIds.contains(oneEventCommentRemoteId)) {
                        if (event.isError()) {
                            mQuickActionProcessor.requestFailed(ARG_ACTION_APPROVE);
                        } else {
                            mQuickActionProcessor.requestCompleted(ARG_ACTION_APPROVE);
                        }
                    }
                }
            }

            // now remove any remoteIds for already processed actions
            for (Long remoteId : eventChangedCommentsRemoteIds) {
                mActionedCommentsRemoteIds.remove(remoteId);
            }
        } else if (event.causeOfChange == CommentAction.LIKE_COMMENT) {
            if (event.isError()) {
                mQuickActionProcessor.requestFailed(ARG_ACTION_LIKE);
            } else {
                mQuickActionProcessor.requestCompleted(ARG_ACTION_LIKE);
            }
        } else if (event.causeOfChange == CommentAction.CREATE_NEW_COMMENT) {
            if (event.isError()) {
                mQuickActionProcessor.requestFailed(ARG_ACTION_REPLY);
            } else {
                mQuickActionProcessor.requestCompleted(ARG_ACTION_REPLY);
            }
        }
    }
}
