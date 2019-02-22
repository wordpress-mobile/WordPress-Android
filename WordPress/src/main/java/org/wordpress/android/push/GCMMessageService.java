package org.wordpress.android.push;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.RemoteInput;
import android.support.v4.util.ArrayMap;
import android.text.TextUtils;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.apache.commons.text.StringEscapeUtils;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.analytics.AnalyticsTracker.Stat;
import org.wordpress.android.datasets.NotificationsTable;
import org.wordpress.android.fluxc.model.CommentStatus;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.models.Note;
import org.wordpress.android.support.ZendeskHelper;
import org.wordpress.android.ui.main.WPMainActivity;
import org.wordpress.android.ui.notifications.NotificationEvents;
import org.wordpress.android.ui.notifications.NotificationsListFragment;
import org.wordpress.android.ui.notifications.utils.NotificationsActions;
import org.wordpress.android.ui.notifications.utils.NotificationsUtils;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.ImageUtils;
import org.wordpress.android.util.PhotonUtils;
import org.wordpress.android.util.StringUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

import static org.wordpress.android.ui.notifications.services.NotificationsUpdateServiceStarter.IS_TAPPED_ON_NOTIFICATION;

public class GCMMessageService extends FirebaseMessagingService {
    private static final ArrayMap<Integer, Bundle> ACTIVE_NOTIFICATIONS_MAP = new ArrayMap<>();
    private static final NotificationHelper NOTIFICATION_HELPER = new NotificationHelper();

    private static final String NOTIFICATION_GROUP_KEY = "notification_group_key";
    private static final int PUSH_NOTIFICATION_ID = 10000;
    public static final int AUTH_PUSH_NOTIFICATION_ID = 20000;
    public static final int GROUP_NOTIFICATION_ID = 30000;
    public static final int ACTIONS_RESULT_NOTIFICATION_ID = 40000;
    public static final int ACTIONS_PROGRESS_NOTIFICATION_ID = 50000;
    public static final int GENERIC_LOCAL_NOTIFICATION_ID = 60000;
    private static final int AUTH_PUSH_REQUEST_CODE_APPROVE = 0;
    private static final int AUTH_PUSH_REQUEST_CODE_IGNORE = 1;
    private static final int AUTH_PUSH_REQUEST_CODE_OPEN_DIALOG = 2;
    public static final String EXTRA_VOICE_OR_INLINE_REPLY = "extra_voice_or_inline_reply";
    private static final int MAX_INBOX_ITEMS = 5;

    private static final String PUSH_ARG_USER = "user";
    private static final String PUSH_ARG_TYPE = "type";
    private static final String PUSH_ARG_TITLE = "title";
    private static final String PUSH_ARG_MSG = "msg";
    public static final String PUSH_ARG_NOTE_ID = "note_id";
    public static final String PUSH_ARG_NOTE_FULL_DATA = "note_full_data";
    private static final String PUSH_ARG_ZENDESK_REQUEST_ID = "zendesk_sdk_request_id";

    private static final String PUSH_TYPE_COMMENT = "c";
    private static final String PUSH_TYPE_LIKE = "like";
    private static final String PUSH_TYPE_COMMENT_LIKE = "comment_like";
    private static final String PUSH_TYPE_AUTOMATTCHER = "automattcher";
    private static final String PUSH_TYPE_FOLLOW = "follow";
    private static final String PUSH_TYPE_REBLOG = "reblog";
    private static final String PUSH_TYPE_PUSH_AUTH = "push_auth";
    private static final String PUSH_TYPE_BADGE_RESET = "badge-reset";
    private static final String PUSH_TYPE_NOTE_DELETE = "note-delete";
    private static final String PUSH_TYPE_TEST_NOTE = "push_test";
    private static final String PUSH_TYPE_ZENDESK = "zendesk";

    // All Zendesk push notifications will show the same notification, so hopefully this will be a unique ID
    private static final int ZENDESK_PUSH_NOTIFICATION_ID = 1999999999;

    @Inject AccountStore mAccountStore;
    @Inject SiteStore mSiteStore;
    @Inject ZendeskHelper mZendeskHelper;

    private static final String KEY_CATEGORY_COMMENT_LIKE = "comment-like";
    private static final String KEY_CATEGORY_COMMENT_REPLY = "comment-reply";
    private static final String KEY_CATEGORY_COMMENT_MODERATE = "comment-moderate";

    @Override
    public void onCreate() {
        super.onCreate();
        ((WordPress) getApplication()).component().inject(this);
    }

    // Add to the analytics properties map a subset of the push notification payload.
    private static final String[] PROPERTIES_TO_COPY_INTO_ANALYTICS =
            {PUSH_ARG_NOTE_ID, PUSH_ARG_TYPE, "blog_id", "post_id", "comment_id"};

    private void synchronizedHandleDefaultPush(@NonNull Map<String, String> data) {
        // ACTIVE_NOTIFICATIONS_MAP being static, we can't just synchronize the method
        AnalyticsTracker.track(AnalyticsTracker.Stat.NOTIFICATION_RECEIVED_PROCESSING_START);
        synchronized (GCMMessageService.class) {
            NOTIFICATION_HELPER.handleDefaultPush(
                    this, convertMapToBundle(data), mAccountStore.getAccount().getUserId());
        }
        AnalyticsTracker.track(AnalyticsTracker.Stat.NOTIFICATION_RECEIVED_PROCESSING_END);
    }

    // convert FCM RemoteMessage's Map into legacy GCM Bundle to keep code changes to a minimum
    private Bundle convertMapToBundle(@NonNull Map<String, String> data) {
        Bundle bundle = new Bundle();
        for (Map.Entry<String, String> entry : data.entrySet()) {
            bundle.putString(entry.getKey(), entry.getValue());
        }
        return bundle;
    }

    @Override
    public void onMessageReceived(RemoteMessage message) {
        Map data = message.getData();
        AppLog.v(T.NOTIFS, "Received Message");

        if (data == null) {
            AppLog.v(T.NOTIFS, "No notification message content received. Aborting.");
            return;
        }

        if (!mAccountStore.hasAccessToken()) {
            return;
        }

        if (PUSH_TYPE_ZENDESK.equals(String.valueOf(data.get("type")))) {
            String zendeskRequestId = String.valueOf(data.get(PUSH_ARG_ZENDESK_REQUEST_ID));

            // Try to refresh the Zendesk request page if it's currently being displayed; otherwise show a notification
            if (!mZendeskHelper.refreshRequest(this, zendeskRequestId)) {
                NOTIFICATION_HELPER.handleZendeskNotification(this);
            }
        }

        synchronizedHandleDefaultPush(data);
    }

    public static synchronized void rebuildAndUpdateNotificationsOnSystemBarForThisNote(Context context,
                                                                                        String noteId) {
        if (ACTIVE_NOTIFICATIONS_MAP.size() > 0) {
            // get the corresponding bundle for this noteId
            // using a copy of the ArrayMap to iterate over on, as we might need to modify the original array
            ArrayMap<Integer, Bundle> tmpMap = new ArrayMap(ACTIVE_NOTIFICATIONS_MAP);
            for (Map.Entry<Integer, Bundle> row : tmpMap.entrySet()) {
                Bundle noteBundle = row.getValue();
                if (noteBundle.getString(PUSH_ARG_NOTE_ID, "").equals(noteId)) {
                    NOTIFICATION_HELPER.rebuildAndUpdateNotificationsOnSystemBar(context, noteBundle);
                    return;
                }
            }
        }
    }

    public static synchronized void rebuildAndUpdateNotifsOnSystemBarForRemainingNote(Context context) {
        if (ACTIVE_NOTIFICATIONS_MAP.size() > 0) {
            Bundle remainingNote = ACTIVE_NOTIFICATIONS_MAP.values().iterator().next();
            NOTIFICATION_HELPER.rebuildAndUpdateNotificationsOnSystemBar(context, remainingNote);
        }
    }

    public static synchronized Bundle getCurrentNoteBundleForNoteId(String noteId) {
        if (ACTIVE_NOTIFICATIONS_MAP.size() > 0) {
            // get the corresponding bundle for this noteId
            for (Iterator<Map.Entry<Integer, Bundle>> it = ACTIVE_NOTIFICATIONS_MAP.entrySet().iterator();
                 it.hasNext();) {
                Map.Entry<Integer, Bundle> row = it.next();
                Bundle noteBundle = row.getValue();
                if (noteBundle.getString(PUSH_ARG_NOTE_ID, "").equals(noteId)) {
                    return noteBundle;
                }
            }
        }
        return null;
    }

    public static synchronized void clearNotifications() {
        for (Iterator<Map.Entry<Integer, Bundle>> it = ACTIVE_NOTIFICATIONS_MAP.entrySet().iterator(); it.hasNext();) {
            Map.Entry<Integer, Bundle> row = it.next();
            Integer pushId = row.getKey();
            // don't cancel or remove the AUTH notification if it exists
            if (!pushId.equals(AUTH_PUSH_NOTIFICATION_ID)) {
                it.remove();
            }
        }
    }

    public static synchronized int getNotificationsCount() {
        return ACTIVE_NOTIFICATIONS_MAP.size();
    }

    public static synchronized boolean hasNotifications() {
        return !ACTIVE_NOTIFICATIONS_MAP.isEmpty();
    }

    // Removes a specific notification from the internal map - only use this when we know
    // the user has dismissed the app by swiping it off the screen
    public static synchronized void removeNotification(int notificationId) {
        ACTIVE_NOTIFICATIONS_MAP.remove(notificationId);
    }

    // Removes a specific notification from the system bar
    public static synchronized void removeNotificationWithNoteIdFromSystemBar(Context context, String noteID) {
        if (context == null || TextUtils.isEmpty(noteID) || !hasNotifications()) {
            return;
        }

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        // here we loop with an Iterator as there might be several Notifications with the same Note ID
        // (i.e. likes on the same Note) so we need to keep cancelling them and removing them from our
        // ACTIVE_NOTIFICATIONS_MAP as we find it suitable
        for (Iterator<Map.Entry<Integer, Bundle>> it = ACTIVE_NOTIFICATIONS_MAP.entrySet().iterator(); it.hasNext();) {
            Map.Entry<Integer, Bundle> row = it.next();
            Integer pushId = row.getKey();
            Bundle noteBundle = row.getValue();
            if (noteBundle.getString(PUSH_ARG_NOTE_ID, "").equals(noteID)) {
                notificationManager.cancel(pushId);
                it.remove();
            }
        }

        if (ACTIVE_NOTIFICATIONS_MAP.size() == 0) {
            notificationManager.cancel(GCMMessageService.GROUP_NOTIFICATION_ID);
        }
    }

    // Removes all app notifications from the system bar
    public static synchronized void removeAllNotifications(Context context) {
        if (context == null || !hasNotifications()) {
            return;
        }

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        for (Iterator<Map.Entry<Integer, Bundle>> it = ACTIVE_NOTIFICATIONS_MAP.entrySet().iterator(); it.hasNext();) {
            Map.Entry<Integer, Bundle> row = it.next();
            Integer pushId = row.getKey();
            // don't cancel or remove the AUTH notification if it exists
            if (!pushId.equals(AUTH_PUSH_NOTIFICATION_ID)) {
                notificationManager.cancel(pushId);
                it.remove();
            }
        }
        notificationManager.cancel(GCMMessageService.GROUP_NOTIFICATION_ID);
    }

    public static synchronized void remove2FANotification(Context context) {
        if (context == null || !hasNotifications()) {
            return;
        }

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.cancel(AUTH_PUSH_NOTIFICATION_ID);
        ACTIVE_NOTIFICATIONS_MAP.remove(AUTH_PUSH_NOTIFICATION_ID);
    }

    // NoteID is the ID if the note in WordPress
    public static synchronized void bumpPushNotificationsTappedAnalytics(String noteID) {
        for (Iterator<Map.Entry<Integer, Bundle>> it = ACTIVE_NOTIFICATIONS_MAP.entrySet().iterator();
             it.hasNext();) {
            Map.Entry<Integer, Bundle> row = it.next();
            Bundle noteBundle = row.getValue();
            if (noteBundle.getString(PUSH_ARG_NOTE_ID, "").equals(noteID)) {
                bumpPushNotificationsAnalytics(Stat.PUSH_NOTIFICATION_TAPPED, noteBundle, null);
                AnalyticsTracker.flush();
                return;
            }
        }
    }

    // Mark all notifications as tapped
    public static synchronized void bumpPushNotificationsTappedAllAnalytics() {
        for (Iterator<Map.Entry<Integer, Bundle>> it = ACTIVE_NOTIFICATIONS_MAP.entrySet().iterator();
             it.hasNext();) {
            Map.Entry<Integer, Bundle> row = it.next();
            Bundle noteBundle = row.getValue();
            bumpPushNotificationsAnalytics(Stat.PUSH_NOTIFICATION_TAPPED, noteBundle, null);
        }
        AnalyticsTracker.flush();
    }

    private static void bumpPushNotificationsAnalytics(Stat stat, Bundle noteBundle,
                                                       Map<String, Object> properties) {
        // Bump Analytics for PNs if "Show notifications" setting is checked (default). Skip otherwise.
        if (!NotificationsUtils.isNotificationsEnabled(WordPress.getContext())) {
            return;
        }
        if (properties == null) {
            properties = new HashMap<>();
        }

        String notificationID = noteBundle.getString(PUSH_ARG_NOTE_ID, "");
        if (!TextUtils.isEmpty(notificationID)) {
            for (String currentPropertyToCopy : PROPERTIES_TO_COPY_INTO_ANALYTICS) {
                if (noteBundle.containsKey(currentPropertyToCopy)) {
                    properties.put("push_notification_" + currentPropertyToCopy, noteBundle.get(currentPropertyToCopy));
                }
            }
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(WordPress.getContext());
            String lastRegisteredGCMToken = preferences.getString(NotificationsUtils.WPCOM_PUSH_DEVICE_TOKEN, null);
            properties.put("push_notification_token", lastRegisteredGCMToken);
            AnalyticsTracker.track(stat, properties);
        }
    }

    private static void addAuthPushNotificationToNotificationMap(Bundle data) {
        ACTIVE_NOTIFICATIONS_MAP.put(AUTH_PUSH_NOTIFICATION_ID, data);
    }

    private static class NotificationHelper {
        private void handleDefaultPush(Context context, @NonNull Bundle data, long wpcomUserId) {
            String pushUserId = data.getString(PUSH_ARG_USER);
            // pushUserId is always set server side, but better to double check it here.
            if (!String.valueOf(wpcomUserId).equals(pushUserId)) {
                AppLog.e(T.NOTIFS, "wpcom userId found in the app doesn't match with the ID in the PN. Aborting.");
                return;
            }

            String noteType = StringUtils.notNullStr(data.getString(PUSH_ARG_TYPE));

            // Check for wpcom auth push, if so we will process this push differently
            if (noteType.equals(PUSH_TYPE_PUSH_AUTH)) {
                addAuthPushNotificationToNotificationMap(data);
                handlePushAuth(context, data);
                return;
            }

            if (noteType.equals(PUSH_TYPE_BADGE_RESET)) {
                handleBadgeResetPN(context, data);
                return;
            }

            if (noteType.equals(PUSH_TYPE_NOTE_DELETE)) {
                handleNoteDeletePN(context, data);
                return;
            }

            if (noteType.equals(PUSH_TYPE_TEST_NOTE)) {
                buildAndShowNotificationFromTestPushData(context, data);
                return;
            }

            buildAndShowNotificationFromNoteData(context, data);
        }

        private void buildAndShowNotificationFromTestPushData(Context context, Bundle data) {
            if (data == null) {
                AppLog.e(T.NOTIFS, "Test push notification received without a valid Bundle!");
                return;
            }

            String title = context.getString(R.string.app_name);
            String message = StringEscapeUtils.unescapeHtml4(data.getString(PUSH_ARG_MSG));

            int pushId = PUSH_NOTIFICATION_ID + ACTIVE_NOTIFICATIONS_MAP.size();
            ACTIVE_NOTIFICATIONS_MAP.put(pushId, data);
            Intent resultIntent = new Intent(context, WPMainActivity.class);
            resultIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            showSimpleNotification(context, title, message, resultIntent, pushId);
        }

        private void buildAndShowNotificationFromNoteData(Context context, Bundle data) {
            if (data == null) {
                AppLog.e(T.NOTIFS, "Push notification received without a valid Bundle!");
                return;
            }

            final String wpcomNoteID = data.getString(PUSH_ARG_NOTE_ID, "");
            if (TextUtils.isEmpty(wpcomNoteID)) {
                // At this point 'note_id' is always available in the notification bundle.
                AppLog.e(T.NOTIFS, "Push notification received without a valid note_id in in payload!");
                return;
            }

            // Try to build the note object from the PN payload, and save it to the DB.
            NotificationsUtils.buildNoteObjectFromBundleAndSaveIt(data);
            EventBus.getDefault().post(new NotificationEvents.NotificationsChanged(true));
            // Always do this, since a note can be updated on the server after a PN is sent
            NotificationsActions.downloadNoteAndUpdateDB(wpcomNoteID, null, null);

            String noteType = StringUtils.notNullStr(data.getString(PUSH_ARG_TYPE));

            String title = StringEscapeUtils.unescapeHtml4(data.getString(PUSH_ARG_TITLE));
            if (title == null) {
                title = context.getString(R.string.app_name);
            }
            String message = StringEscapeUtils.unescapeHtml4(data.getString(PUSH_ARG_MSG));

        /*
         * if this has the same note_id as the previous notification, and the previous notification
         * was received within the last second, then skip showing it - this handles duplicate
         * notifications being shown due to the device being registered multiple times with different tokens.
         * (still investigating how this could happen - 21-Oct-13)
         *
         * this also handles the (rare) case where the user receives rapid-fire sub-second like notifications
         * due to sudden popularity (post gets added to FP and is liked by many people all at once, etc.),
         * which we also want to avoid since it would drain the battery and annoy the user
         *
         * NOTE: different comments on the same post will have a different note_id, but different likes
         * on the same post will have the same note_id, so don't assume that the note_id is unique
         */
            long thisTime = System.currentTimeMillis();
            if (AppPrefs.getLastPushNotificationWpcomNoteId().equals(wpcomNoteID)) {
                long seconds = TimeUnit.MILLISECONDS.toSeconds(thisTime - AppPrefs.getLastPushNotificationTime());
                if (seconds <= 1) {
                    AppLog.w(T.NOTIFS, "skipped potential duplicate notification");
                    return;
                }
            }

            AppPrefs.setLastPushNotificationTime(thisTime);
            AppPrefs.setLastPushNotificationWpcomNoteId(wpcomNoteID);

            // Update notification content for the same noteId if it is already showing
            int pushId = 0;
            for (Integer id : ACTIVE_NOTIFICATIONS_MAP.keySet()) {
                if (id == null) {
                    continue;
                }
                Bundle noteBundle = ACTIVE_NOTIFICATIONS_MAP.get(id);
                if (noteBundle != null && noteBundle.getString(PUSH_ARG_NOTE_ID, "").equals(wpcomNoteID)) {
                    pushId = id;
                    ACTIVE_NOTIFICATIONS_MAP.put(pushId, data);
                    break;
                }
            }

            if (pushId == 0) {
                pushId = PUSH_NOTIFICATION_ID + ACTIVE_NOTIFICATIONS_MAP.size();
                ACTIVE_NOTIFICATIONS_MAP.put(pushId, data);
            }

            // Bump Analytics for PNs if "Show notifications" setting is checked (default). Skip otherwise.
            if (NotificationsUtils.isNotificationsEnabled(context)) {
                Map<String, Object> properties = new HashMap<>();
                if (!TextUtils.isEmpty(noteType)) {
                    // 'comment' and 'comment_pingback' types are sent in PN as type = "c"
                    if (noteType.equals(PUSH_TYPE_COMMENT)) {
                        properties.put("notification_type", "comment");
                    } else {
                        properties.put("notification_type", noteType);
                    }
                }

                bumpPushNotificationsAnalytics(Stat.PUSH_NOTIFICATION_RECEIVED, data, properties);
                AnalyticsTracker.flush();
            }

            // Build the new notification, add group to support wearable stacking
            NotificationCompat.Builder builder = getNotificationBuilder(context, title, message);
            Bitmap largeIconBitmap =
                    getLargeIconBitmap(context, data.getString("icon"), shouldCircularizeNoteIcon(noteType));
            if (largeIconBitmap != null) {
                builder.setLargeIcon(largeIconBitmap);
            }

            showSingleNotificationForBuilder(context, builder, noteType, wpcomNoteID, pushId, true);

            // Also add a group summary notification, which is required for non-wearable devices
            // Do not need to play the sound again. We've already played it in the individual builder.
            showGroupNotificationForBuilder(context, builder, wpcomNoteID, message);
        }

        private void showSimpleNotification(Context context, String title, String message, Intent resultIntent,
                                            int pushId) {
            NotificationCompat.Builder builder = getNotificationBuilder(context, title, message);
            showNotificationForBuilder(builder, context, resultIntent, pushId, true);
        }

        private void addActionsForCommentNotification(Context context, NotificationCompat.Builder builder,
                                                      String noteId) {
            // Add some actions if this is a comment notification
            boolean areActionsSet = false;
            Note note = NotificationsTable.getNoteById(noteId);
            if (note != null) {
                // if note can be replied to, we'll always add this action first
                if (note.canReply()) {
                    addCommentReplyActionForCommentNotification(context, builder, noteId);
                }

                // if the comment is lacking approval, offer moderation actions
                if (note.getCommentStatus() == CommentStatus.UNAPPROVED) {
                    if (note.canModerate()) {
                        addCommentApproveActionForCommentNotification(context, builder, noteId);
                    }
                } else {
                    // else offer REPLY / LIKE actions
                    // LIKE can only be enabled for wp.com sites, so if this is a Jetpack site don't enable LIKEs
                    if (note.canLike()) {
                        addCommentLikeActionForCommentNotification(context, builder, noteId);
                    }
                }
                areActionsSet = true;
            }

            // if we could not set the actions, set the default one REPLY as it's then only safe bet
            // we can make at this point
            if (!areActionsSet) {
                addCommentReplyActionForCommentNotification(context, builder, noteId);
            }
        }

        private void addCommentReplyActionForCommentNotification(Context context, NotificationCompat.Builder builder,
                                                                 String noteId) {
            // adding comment reply action
            Intent commentReplyIntent = getCommentActionReplyIntent(context, noteId);
            commentReplyIntent.addCategory(KEY_CATEGORY_COMMENT_REPLY);
            commentReplyIntent.putExtra(NotificationsProcessingService.ARG_ACTION_TYPE,
                                        NotificationsProcessingService.ARG_ACTION_REPLY);
            if (noteId != null) {
                commentReplyIntent.putExtra(NotificationsProcessingService.ARG_NOTE_ID, noteId);
            }
            commentReplyIntent
                    .putExtra(NotificationsProcessingService.ARG_NOTE_BUNDLE, getCurrentNoteBundleForNoteId(noteId));


            PendingIntent commentReplyPendingIntent = getCommentActionPendingIntent(context, commentReplyIntent);

            // The following code adds the behavior for Direct reply, available on Android N (7.0) and on.
            // Using backward compatibility with NotificationCompat.
            String replyLabel = context.getString(R.string.reply);
            RemoteInput remoteInput = new RemoteInput.Builder(EXTRA_VOICE_OR_INLINE_REPLY)
                    .setLabel(replyLabel)
                    .build();
            NotificationCompat.Action action = new NotificationCompat.Action.Builder(R.drawable.ic_reply_white_24dp,
                    context.getString(R.string.reply), commentReplyPendingIntent).addRemoteInput(remoteInput).build();
            // now add the action corresponding to direct-reply
            builder.addAction(action);
        }

        private void addCommentLikeActionForCommentNotification(Context context, NotificationCompat.Builder builder,
                                                                String noteId) {
            // adding comment like action
            Intent commentLikeIntent = getCommentActionIntent(context);
            commentLikeIntent.addCategory(KEY_CATEGORY_COMMENT_LIKE);
            commentLikeIntent.putExtra(NotificationsProcessingService.ARG_ACTION_TYPE,
                                       NotificationsProcessingService.ARG_ACTION_LIKE);
            if (noteId != null) {
                commentLikeIntent.putExtra(NotificationsProcessingService.ARG_NOTE_ID, noteId);
            }
            commentLikeIntent
                    .putExtra(NotificationsProcessingService.ARG_NOTE_BUNDLE, getCurrentNoteBundleForNoteId(noteId));

            PendingIntent commentLikePendingIntent = getCommentActionPendingIntentForService(context,
                                                                                             commentLikeIntent);
            builder.addAction(R.drawable.ic_star_white_24dp, context.getText(R.string.like), commentLikePendingIntent);
        }

        private void addCommentApproveActionForCommentNotification(Context context, NotificationCompat.Builder builder,
                                                                   String noteId) {
            // adding comment approve action
            Intent commentApproveIntent = getCommentActionIntent(context);
            commentApproveIntent.addCategory(KEY_CATEGORY_COMMENT_MODERATE);
            commentApproveIntent.putExtra(NotificationsProcessingService.ARG_ACTION_TYPE,
                                          NotificationsProcessingService.ARG_ACTION_APPROVE);
            if (noteId != null) {
                commentApproveIntent.putExtra(NotificationsProcessingService.ARG_NOTE_ID, noteId);
            }
            commentApproveIntent
                    .putExtra(NotificationsProcessingService.ARG_NOTE_BUNDLE, getCurrentNoteBundleForNoteId(noteId));

            PendingIntent commentApprovePendingIntent = getCommentActionPendingIntentForService(context,
                                                                                                commentApproveIntent);
            builder.addAction(R.drawable.ic_checkmark_white_24dp, context.getText(R.string.approve),
                              commentApprovePendingIntent);
        }

        private PendingIntent getCommentActionPendingIntent(Context context, Intent intent) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                return getCommentActionPendingIntentForService(context, intent);
            } else {
                return getCommentActionPendingIntentForActivity(context, intent);
            }
        }

        private PendingIntent getCommentActionPendingIntentForService(Context context, Intent intent) {
            return PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        }

        private PendingIntent getCommentActionPendingIntentForActivity(Context context, Intent intent) {
            return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        }

        private Intent getCommentActionReplyIntent(Context context, String noteId) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                return getCommentActionIntentForService(context);
            } else {
                return getCommentActionIntentForActivity(context, noteId);
            }
        }

        private Intent getCommentActionIntent(Context context) {
            return getCommentActionIntentForService(context);
        }

        private Intent getCommentActionIntentForService(Context context) {
            return new Intent(context, NotificationsProcessingService.class);
        }

        private Intent getCommentActionIntentForActivity(Context context, String noteId) {
            Intent intent = new Intent(context, WPMainActivity.class);
            intent.putExtra(WPMainActivity.ARG_OPENED_FROM_PUSH, true);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            intent.setAction("android.intent.action.MAIN");
            intent.addCategory("android.intent.category.LAUNCHER");
            intent.putExtra(NotificationsListFragment.NOTE_ID_EXTRA, noteId);
            intent.putExtra(NotificationsListFragment.NOTE_INSTANT_REPLY_EXTRA, true);
            return intent;
        }

        private Bitmap getLargeIconBitmap(Context context, String iconUrl, boolean shouldCircularizeIcon) {
            Bitmap largeIconBitmap = null;
            if (iconUrl != null) {
                try {
                    iconUrl = URLDecoder.decode(iconUrl, "UTF-8");
                    int largeIconSize = context.getResources().getDimensionPixelSize(
                            android.R.dimen.notification_large_icon_height);
                    String resizedUrl = PhotonUtils.getPhotonImageUrl(iconUrl, largeIconSize, largeIconSize);
                    largeIconBitmap = ImageUtils.downloadBitmap(resizedUrl);
                    if (largeIconBitmap != null && shouldCircularizeIcon) {
                        largeIconBitmap = ImageUtils.getCircularBitmap(largeIconBitmap);
                    }
                } catch (UnsupportedEncodingException e) {
                    AppLog.e(T.NOTIFS, e);
                }
            }
            return largeIconBitmap;
        }

        private NotificationCompat.Builder getNotificationBuilder(Context context, String title, String message) {
            // Build the new notification, add group to support wearable stacking
            return new NotificationCompat.Builder(context,
                    context.getString(R.string.notification_channel_normal_id))
                    .setSmallIcon(R.drawable.ic_my_sites_white_24dp)
                    .setColor(context.getResources().getColor(R.color.blue_wordpress))
                    .setContentTitle(title)
                    .setContentText(message)
                    .setTicker(message)
                    .setOnlyAlertOnce(true)
                    .setAutoCancel(true)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                    .setGroup(NOTIFICATION_GROUP_KEY);
        }

        private void showGroupNotificationForBuilder(Context context, NotificationCompat.Builder builder,
                                                     String wpcomNoteID, String message) {
            if (builder == null || context == null) {
                return;
            }

            // using a copy of the map to avoid concurrency problems
            ArrayMap<Integer, Bundle> tmpMap = new ArrayMap(ACTIVE_NOTIFICATIONS_MAP);
            // first remove 2fa push from the map, so it's not shown in the inbox style group notif
            tmpMap.remove(AUTH_PUSH_NOTIFICATION_ID);
            if (tmpMap.size() > 1) {
                NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
                int noteCtr = 1;
                for (Bundle pushBundle : tmpMap.values()) {
                    // InboxStyle notification is limited to 5 lines
                    if (noteCtr > MAX_INBOX_ITEMS) {
                        break;
                    }
                    if (pushBundle == null || pushBundle.getString(PUSH_ARG_MSG) == null) {
                        continue;
                    }

                    if (pushBundle.getString(PUSH_ARG_TYPE, "").equals(PUSH_TYPE_COMMENT)) {
                        String pnTitle = StringEscapeUtils.unescapeHtml4((pushBundle.getString(PUSH_ARG_TITLE)));
                        String pnMessage = StringEscapeUtils.unescapeHtml4((pushBundle.getString(PUSH_ARG_MSG)));
                        inboxStyle.addLine(pnTitle + ": " + pnMessage);
                    } else {
                        String pnMessage = StringEscapeUtils.unescapeHtml4((pushBundle.getString(PUSH_ARG_MSG)));
                        inboxStyle.addLine(pnMessage);
                    }

                    noteCtr++;
                }

                if (tmpMap.size() > MAX_INBOX_ITEMS) {
                    inboxStyle.setSummaryText(String.format(context.getString(R.string.more_notifications),
                            tmpMap.size() - MAX_INBOX_ITEMS));
                }

                String subject =
                        String.format(context.getString(R.string.new_notifications), tmpMap.size());
                NotificationCompat.Builder groupBuilder = new NotificationCompat.Builder(context,
                        context.getString(R.string.notification_channel_normal_id))
                        .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
                        .setSmallIcon(R.drawable.ic_my_sites_white_24dp)
                        .setColor(context.getResources().getColor(R.color.blue_wordpress))
                        .setGroup(NOTIFICATION_GROUP_KEY)
                        .setGroupSummary(true)
                        .setAutoCancel(true)
                        .setTicker(message)
                        .setContentTitle(context.getString(R.string.app_name))
                        .setContentText(subject)
                        .setStyle(inboxStyle);

                showWPComNotificationForBuilder(groupBuilder, context, wpcomNoteID, GROUP_NOTIFICATION_ID, false);
            } else {
                // Set the individual notification we've already built as the group summary
                builder.setGroupSummary(true)
                        .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN);
                showWPComNotificationForBuilder(builder, context, wpcomNoteID, GROUP_NOTIFICATION_ID, false);
            }
        }

        private void showSingleNotificationForBuilder(Context context, NotificationCompat.Builder builder,
                                                      String noteType, String wpcomNoteID, int pushId,
                                                      boolean notifyUser) {
            if (builder == null || context == null) {
                return;
            }

            if (noteType.equals(PUSH_TYPE_COMMENT)) {
                addActionsForCommentNotification(context, builder, wpcomNoteID);
            }

            showWPComNotificationForBuilder(builder, context, wpcomNoteID, pushId, notifyUser);
        }

        private void showWPComNotificationForBuilder(NotificationCompat.Builder builder, Context context,
                                                     String wpcomNoteID, int pushId, boolean notifyUser) {
            Intent resultIntent = new Intent(context, WPMainActivity.class);
            resultIntent.putExtra(WPMainActivity.ARG_OPENED_FROM_PUSH, true);
            resultIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK
                                  | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            resultIntent.setAction("android.intent.action.MAIN");
            resultIntent.addCategory("android.intent.category.LAUNCHER");
            resultIntent.putExtra(NotificationsListFragment.NOTE_ID_EXTRA, wpcomNoteID);
            resultIntent.putExtra(IS_TAPPED_ON_NOTIFICATION, true);

            showNotificationForBuilder(builder, context, resultIntent, pushId, notifyUser);
        }

        // Displays a notification to the user
        private void showNotificationForBuilder(NotificationCompat.Builder builder, Context context,
                                                Intent resultIntent, int pushId, boolean notifyUser) {
            if (builder == null || context == null || resultIntent == null) {
                return;
            }

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            boolean shouldReceiveNotifications =
                    prefs.getBoolean(context.getString(R.string.wp_pref_notifications_master), true);

            if (shouldReceiveNotifications) {
                if (notifyUser) {
                    boolean shouldVibrate = prefs.getBoolean(
                            context.getString(R.string.wp_pref_notification_vibrate), false);
                    boolean shouldBlinkLight = prefs.getBoolean(
                            context.getString(R.string.wp_pref_notification_light), true);
                    String notificationSound = prefs.getString(
                       context.getString(R.string.wp_pref_custom_notification_sound),
                       context.getString(R.string.notification_settings_item_sights_and_sounds_choose_sound_default));

                    if (!TextUtils.isEmpty(notificationSound)
                            && !notificationSound.trim().toLowerCase(Locale.ROOT).startsWith("file://")) {
                        builder.setSound(Uri.parse(notificationSound));
                    }

                    if (shouldVibrate) {
                        builder.setVibrate(new long[]{500, 500, 500});
                    }
                    if (shouldBlinkLight) {
                        builder.setLights(0xff0000ff, 1000, 5000);
                    }
                } else {
                    builder.setVibrate(null);
                    builder.setSound(null);
                    // Do not turn the led off otherwise the previous (single) notification led is not shown.
                    // We're re-using the same builder for single and group.
                }

                // Call processing service when notification is dismissed
                PendingIntent pendingDeleteIntent =
                        NotificationsProcessingService.getPendingIntentForNotificationDismiss(context, pushId);
                builder.setDeleteIntent(pendingDeleteIntent);

                builder.setCategory(NotificationCompat.CATEGORY_SOCIAL);

                PendingIntent pendingIntent = PendingIntent.getActivity(context, pushId, resultIntent,
                                                                        PendingIntent.FLAG_CANCEL_CURRENT
                                                                        | PendingIntent.FLAG_UPDATE_CURRENT);
                builder.setContentIntent(pendingIntent);
                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                notificationManager.notify(pushId, builder.build());
            }
        }

        private void rebuildAndUpdateNotificationsOnSystemBar(Context context, Bundle data) {
            String noteType = StringUtils.notNullStr(data.getString(PUSH_ARG_TYPE));

            // Check for wpcom auth push, if so we will process this push differently
            // and we'll remove the auth special notif out of the map while we re-build the remaining notifs
            ArrayMap<Integer, Bundle> tmpMap = new ArrayMap(ACTIVE_NOTIFICATIONS_MAP);
            Bundle authPNBundle = tmpMap.remove(AUTH_PUSH_NOTIFICATION_ID);
            if (authPNBundle != null) {
                handlePushAuth(context, authPNBundle);
                if (tmpMap.size() > 0 && noteType.equals(PUSH_TYPE_PUSH_AUTH)) {
                    // get the data for the next notification in map for re-build
                    // because otherwise we would be keeping the PUSH_AUTH type note in `data`
                    data = tmpMap.values().iterator().next();
                } else if (noteType.equals(PUSH_TYPE_PUSH_AUTH)) {
                    // only note is the 2fa note, just return
                    return;
                }
            }


            Bitmap largeIconBitmap = null;
            // here notify the existing group notification by eliminating the line that is now gone
            String title = getNotificationTitleOrAppNameFromBundle(context, data);
            String message = StringEscapeUtils.unescapeHtml4(data.getString(PUSH_ARG_MSG));

            NotificationCompat.Builder builder = null;
            String wpcomNoteID = null;

            if (tmpMap.size() == 1) {
                // only one notification remains, so get the proper message for it and re-instate
                // in the system dashboard
                Bundle remainingNote = tmpMap.values().iterator().next();
                if (remainingNote != null) {
                    String remainingNoteTitle =
                            StringEscapeUtils.unescapeHtml4(remainingNote.getString(PUSH_ARG_TITLE));
                    if (!TextUtils.isEmpty(remainingNoteTitle)) {
                        title = remainingNoteTitle;
                    }
                    String remainingNoteMessage =
                            StringEscapeUtils.unescapeHtml4(remainingNote.getString(PUSH_ARG_MSG));
                    if (!TextUtils.isEmpty(remainingNoteMessage)) {
                        message = remainingNoteMessage;
                    }
                    largeIconBitmap = getLargeIconBitmap(context, remainingNote.getString("icon"),
                                                         shouldCircularizeNoteIcon(
                                                                 remainingNote.getString(PUSH_ARG_TYPE)));

                    builder = getNotificationBuilder(context, title, message);

                    // set timestamp for note: first try with the notification timestamp, then try google's sent time
                    // if not available; finally just set the system's current time if everything
                    // else fails (not likely)
                    long timeStampToShow =
                            DateTimeUtils.timestampFromIso8601Millis(remainingNote.getString("note_timestamp"));
                    timeStampToShow = timeStampToShow != 0 ? timeStampToShow
                            : remainingNote.getLong("google.sent_time", System.currentTimeMillis());
                    builder.setWhen(timeStampToShow);

                    noteType = StringUtils.notNullStr(remainingNote.getString(PUSH_ARG_TYPE));
                    wpcomNoteID = remainingNote.getString(PUSH_ARG_NOTE_ID, "");
                    if (!tmpMap.isEmpty()) {
                        showSingleNotificationForBuilder(context, builder, noteType, wpcomNoteID,
                                tmpMap.keyAt(0), false);
                    }
                }
            }

            if (builder == null) {
                builder = getNotificationBuilder(context, title, message);
            }

            if (largeIconBitmap == null) {
                largeIconBitmap = getLargeIconBitmap(context, data.getString("icon"),
                                                     shouldCircularizeNoteIcon(PUSH_TYPE_BADGE_RESET));
            }

            if (wpcomNoteID == null) {
                wpcomNoteID = AppPrefs.getLastPushNotificationWpcomNoteId();
            }

            if (largeIconBitmap != null) {
                builder.setLargeIcon(largeIconBitmap);
            }

            showGroupNotificationForBuilder(context, builder, wpcomNoteID, message);
        }

        private String getNotificationTitleOrAppNameFromBundle(Context context, Bundle data) {
            String title = StringEscapeUtils.unescapeHtml4(data.getString(PUSH_ARG_TITLE));
            if (title == null) {
                title = context.getString(R.string.app_name);
            }
            return title;
        }

        // Clear all notifications
        private void handleBadgeResetPN(Context context, Bundle data) {
            if (data == null) {
                return;
            }

            String noteID = data.getString(PUSH_ARG_NOTE_ID, "");
            if (!TextUtils.isEmpty(noteID)) {
                Note note = NotificationsTable.getNoteById(noteID);
                // mark the note as read if it's unread and update the DB silently
                if (note != null && note.isUnread()) {
                    note.setRead();
                    NotificationsTable.saveNote(note);
                }


                removeNotificationWithNoteIdFromSystemBar(context, noteID);
                // now that we cleared the specific notif, we can check and make any visual updates
                if (ACTIVE_NOTIFICATIONS_MAP.size() > 0) {
                    rebuildAndUpdateNotificationsOnSystemBar(context, data);
                }
            } else {
                removeAllNotifications(context);
            }

            EventBus.getDefault().post(new NotificationEvents.NotificationsChanged(
                    ACTIVE_NOTIFICATIONS_MAP.size() > 0));
        }

        private void handleNoteDeletePN(Context context, Bundle data) {
            if (data == null || !data.containsKey(PUSH_ARG_NOTE_ID)) {
                return;
            }

            String noteID = data.getString(PUSH_ARG_NOTE_ID, "");
            if (!TextUtils.isEmpty(noteID)) {
                NotificationsTable.deleteNoteById(noteID);
            }

            removeNotificationWithNoteIdFromSystemBar(context, noteID);
            // now that we cleared the specific notif, we can check and make any visual updates
            if (ACTIVE_NOTIFICATIONS_MAP.size() > 0) {
                rebuildAndUpdateNotificationsOnSystemBar(context, data);
            }

            EventBus.getDefault().post(new NotificationEvents.NotificationsChanged(
                    ACTIVE_NOTIFICATIONS_MAP.size() > 0));
        }

        // Show a notification for two-step auth users who log in from a web browser
        private void handlePushAuth(Context context, Bundle data) {
            if (data == null) {
                return;
            }

            String pushAuthToken = data.getString("push_auth_token", "");
            String title = data.getString("title", "");
            String message = data.getString("msg", "");
            long expirationTimestamp = Long.valueOf(data.getString("expires", "0"));

            // No strings, no service
            if (TextUtils.isEmpty(pushAuthToken) || TextUtils.isEmpty(title) || TextUtils.isEmpty(message)) {
                return;
            }

            // Show authorization intent
            Intent pushAuthIntent = new Intent(context, WPMainActivity.class);
            pushAuthIntent.putExtra(WPMainActivity.ARG_OPENED_FROM_PUSH, true);
            pushAuthIntent.putExtra(NotificationsUtils.ARG_PUSH_AUTH_TOKEN, pushAuthToken);
            pushAuthIntent.putExtra(NotificationsUtils.ARG_PUSH_AUTH_TITLE, title);
            pushAuthIntent.putExtra(NotificationsUtils.ARG_PUSH_AUTH_MESSAGE, message);
            pushAuthIntent.putExtra(NotificationsUtils.ARG_PUSH_AUTH_EXPIRES, expirationTimestamp);
            pushAuthIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK
                                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            pushAuthIntent.setAction("android.intent.action.MAIN");
            pushAuthIntent.addCategory("android.intent.category.LAUNCHER");

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context,
                    context.getString(R.string.notification_channel_important_id))
                    .setSmallIcon(R.drawable.ic_my_sites_white_24dp)
                    .setColor(context.getResources().getColor(R.color.blue_wordpress))
                    .setContentTitle(title)
                    .setContentText(message)
                    .setAutoCancel(true)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                    .setOnlyAlertOnce(true)
                    .setPriority(NotificationCompat.PRIORITY_MAX);

            PendingIntent pendingIntent =
                    PendingIntent.getActivity(context, AUTH_PUSH_REQUEST_CODE_OPEN_DIALOG, pushAuthIntent,
                                              PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_UPDATE_CURRENT);
            builder.setContentIntent(pendingIntent);


            // adding ignore / approve quick actions
            Intent authApproveIntent = new Intent(context, WPMainActivity.class);
            authApproveIntent.putExtra(WPMainActivity.ARG_OPENED_FROM_PUSH, true);
            authApproveIntent.putExtra(NotificationsProcessingService.ARG_ACTION_TYPE,
                                       NotificationsProcessingService.ARG_ACTION_AUTH_APPROVE);
            authApproveIntent.putExtra(NotificationsUtils.ARG_PUSH_AUTH_TOKEN, pushAuthToken);
            authApproveIntent.putExtra(NotificationsUtils.ARG_PUSH_AUTH_TITLE, title);
            authApproveIntent.putExtra(NotificationsUtils.ARG_PUSH_AUTH_MESSAGE, message);
            authApproveIntent.putExtra(NotificationsUtils.ARG_PUSH_AUTH_EXPIRES, expirationTimestamp);

            authApproveIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK
                                       | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            authApproveIntent.setAction("android.intent.action.MAIN");
            authApproveIntent.addCategory("android.intent.category.LAUNCHER");

            PendingIntent authApprovePendingIntent =
                    PendingIntent.getActivity(context, AUTH_PUSH_REQUEST_CODE_APPROVE, authApproveIntent,
                                              PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_UPDATE_CURRENT);

            builder.addAction(R.drawable.ic_checkmark_white_24dp, context.getText(R.string.approve),
                              authApprovePendingIntent);


            Intent authIgnoreIntent = new Intent(context, NotificationsProcessingService.class);
            authIgnoreIntent.putExtra(NotificationsProcessingService.ARG_ACTION_TYPE,
                                      NotificationsProcessingService.ARG_ACTION_AUTH_IGNORE);
            PendingIntent authIgnorePendingIntent = PendingIntent.getService(context,
                                                                             AUTH_PUSH_REQUEST_CODE_IGNORE,
                                                                             authIgnoreIntent,
                                                                             PendingIntent.FLAG_CANCEL_CURRENT);
            builder.addAction(R.drawable.ic_close_white_24dp, context.getText(R.string.ignore),
                              authIgnorePendingIntent);


            // Call processing service when notification is dismissed
            PendingIntent pendingDeleteIntent =
                    NotificationsProcessingService.getPendingIntentForNotificationDismiss(
                            context, AUTH_PUSH_NOTIFICATION_ID);
            builder.setDeleteIntent(pendingDeleteIntent);

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.notify(AUTH_PUSH_NOTIFICATION_ID, builder.build());
        }

        // Returns true if the note type is known to have a gravatar
        private boolean shouldCircularizeNoteIcon(String noteType) {
            if (TextUtils.isEmpty(noteType)) {
                return false;
            }

            switch (noteType) {
                case PUSH_TYPE_COMMENT:
                case PUSH_TYPE_LIKE:
                case PUSH_TYPE_COMMENT_LIKE:
                case PUSH_TYPE_AUTOMATTCHER:
                case PUSH_TYPE_FOLLOW:
                case PUSH_TYPE_REBLOG:
                    return true;
                default:
                    return false;
            }
        }

        /**
         * Shows a notification stating that the user has a reply pending from Zendesk. Since Zendesk always sends a
         * notification with the same title and message, we use our own localized messaging. For the same reason,
         * we use a static push notification ID. Tapping on the notification will open the `ME` fragment.
         */
        private void handleZendeskNotification(Context context) {
            if (context == null) {
                return;
            }
            String title = context.getString(R.string.support_push_notification_title);
            String message = context.getString(R.string.support_push_notification_message);
            Intent resultIntent = new Intent(context, WPMainActivity.class);
            resultIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            resultIntent.putExtra(WPMainActivity.ARG_SHOW_ZENDESK_NOTIFICATIONS, true);
            showSimpleNotification(context, title, message, resultIntent, ZENDESK_PUSH_NOTIFICATION_ID);
        }
    }
}
