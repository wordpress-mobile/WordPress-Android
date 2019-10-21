package org.wordpress.android.push;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.action.CommentAction;
import org.wordpress.android.fluxc.model.CommentModel;
import org.wordpress.android.fluxc.store.CommentStore;
import org.wordpress.android.fluxc.store.CommentStore.OnCommentChanged;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.util.AppLog;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

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
    public static final String ARG_ACTION_REPLY_TEXT = "action_reply_text";
    public static final String ARG_NOTE_ID = "note_id";
    public static final String ARG_PUSH_ID = "notificationId";

    // bundle and push ID, as they are held in the system dashboard
    public static final String ARG_NOTE_BUNDLE = "note_bundle";

    private QuickActionProcessor mQuickActionProcessor;

    private List<Long> mActionedCommentsRemoteIds = new ArrayList<>();

    @Inject Dispatcher mDispatcher;
    @Inject SiteStore mSiteStore;
    @Inject CommentStore mCommentStore;

    /*
    * Use this if you want the service to handle a background note Like.
    * */
    public static void startServiceForLike(Context context, String noteId) {
        Intent intent = new Intent(context, NotificationsProcessingService.class);
        intent.putExtra(ARG_ACTION_TYPE, NotificationActionType.ARG_ACTION_LIKE);
        intent.putExtra(ARG_NOTE_ID, noteId);
        context.startService(intent);
    }

    /*
    * Use this if you want the service to handle a background note Approve.
    * */
    public static void startServiceForApprove(Context context, String noteId) {
        Intent intent = new Intent(context, NotificationsProcessingService.class);
        intent.putExtra(ARG_ACTION_TYPE, NotificationActionType.ARG_ACTION_APPROVE);
        intent.putExtra(ARG_NOTE_ID, noteId);
        context.startService(intent);
    }

    /*
    * Use this if you want the service to handle a background note reply.
    * */
    public static void startServiceForReply(Context context, String noteId, String replyToComment) {
        Intent intent = new Intent(context, NotificationsProcessingService.class);
        intent.putExtra(ARG_ACTION_TYPE, NotificationActionType.ARG_ACTION_REPLY);
        intent.putExtra(ARG_NOTE_ID, noteId);
        intent.putExtra(ARG_ACTION_REPLY_TEXT, replyToComment);
        context.startService(intent);
    }

    public static PendingIntent getPendingIntentForNotificationDismiss(Context context, int pushId) {
        Intent intent = new Intent(context, NotificationsProcessingService.class);
        intent.putExtra(ARG_ACTION_TYPE, NotificationActionType.ARG_ACTION_NOTIFICATION_DISMISS);
        intent.putExtra(ARG_PUSH_ID, pushId);
        intent.addCategory(NotificationActionType.ARG_ACTION_NOTIFICATION_DISMISS.getValue());
        return PendingIntent.getService(context, pushId, intent, PendingIntent.FLAG_CANCEL_CURRENT);
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
        mDispatcher.register(this);
        AppLog.i(AppLog.T.NOTIFS, "notifications action processing service > created");
    }

    @Override
    public void onDestroy() {
        AppLog.i(AppLog.T.NOTIFS, "notifications action processing service > destroyed");
        mDispatcher.unregister(this);
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Offload to a separate thread.
        mQuickActionProcessor = new QuickActionProcessor(this, mDispatcher, mSiteStore, this, intent, startId);
        new Thread(new Runnable() {
            public void run() {
                mQuickActionProcessor.process();
            }
        }).start();

        return START_NOT_STICKY;
    }

    public void addActionedCommentsRemoteId(long remoteCommendId) {
        if (!mActionedCommentsRemoteIds.contains(remoteCommendId)) {
            mActionedCommentsRemoteIds.add(remoteCommendId);
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
                    CommentModel localComment = mCommentStore.getCommentByLocalId(commentLocalId);
                    if (localComment != null) {
                        eventChangedCommentsRemoteIds.add(localComment.getRemoteCommentId());
                    }
                }

                // here we need to check ids: is an event corresponding to an action triggered from this Service?
                for (Long oneEventCommentRemoteId : eventChangedCommentsRemoteIds) {
                    if (mActionedCommentsRemoteIds.contains(oneEventCommentRemoteId)) {
                        if (event.isError()) {
                            mQuickActionProcessor.requestFailed(NotificationActionType.ARG_ACTION_APPROVE);
                        } else {
                            mQuickActionProcessor.requestCompleted(NotificationActionType.ARG_ACTION_APPROVE);
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
                mQuickActionProcessor.requestFailed(NotificationActionType.ARG_ACTION_LIKE);
            } else {
                mQuickActionProcessor.requestCompleted(NotificationActionType.ARG_ACTION_LIKE);
            }
        } else if (event.causeOfChange == CommentAction.CREATE_NEW_COMMENT) {
            if (event.isError()) {
                mQuickActionProcessor.requestFailed(NotificationActionType.ARG_ACTION_REPLY);
            } else {
                mQuickActionProcessor.requestCompleted(NotificationActionType.ARG_ACTION_REPLY);
            }
        }
    }
}
