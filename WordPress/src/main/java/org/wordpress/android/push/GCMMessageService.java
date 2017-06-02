
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

import com.google.android.gms.gcm.GcmListenerService;

import org.apache.commons.lang3.StringEscapeUtils;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.analytics.AnalyticsTracker.Stat;
import org.wordpress.android.datasets.NotificationsTable;
import org.wordpress.android.fluxc.model.CommentStatus;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.models.Note;
import org.wordpress.android.ui.main.WPMainActivity;
import org.wordpress.android.ui.notifications.NotificationDismissBroadcastReceiver;
import org.wordpress.android.ui.notifications.NotificationEvents;
import org.wordpress.android.ui.notifications.NotificationsListFragment;
import org.wordpress.android.ui.notifications.utils.NotificationsActions;
import org.wordpress.android.ui.notifications.utils.NotificationsUtils;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.HelpshiftHelper;
import org.wordpress.android.util.ImageUtils;
import org.wordpress.android.util.PhotonUtils;
import org.wordpress.android.util.StringUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

public class GCMMessageService extends GcmListenerService {
    private static final ArrayMap<Integer, Bundle> sActiveNotificationsMap = new ArrayMap<>();
    private static final NotificationHelper sNotificationHelper = new NotificationHelper();

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

    private static final String PUSH_TYPE_COMMENT = "c";
    private static final String PUSH_TYPE_LIKE = "like";
    private static final String PUSH_TYPE_COMMENT_LIKE = "comment_like";
    private static final String PUSH_TYPE_AUTOMATTCHER = "automattcher";
    private static final String PUSH_TYPE_FOLLOW = "follow";
    private static final String PUSH_TYPE_REBLOG = "reblog";
    private static final String PUSH_TYPE_PUSH_AUTH = "push_auth";
    private static final String PUSH_TYPE_BADGE_RESET = "badge-reset";
    private static final String PUSH_TYPE_NOTE_DELETE = "note-delete";

    @Inject AccountStore mAccountStore;
    @Inject SiteStore mSiteStore;

    private static final String KEY_CATEGORY_COMMENT_LIKE = "comment-like";
    private static final String KEY_CATEGORY_COMMENT_REPLY = "comment-reply";
    private static final String KEY_CATEGORY_COMMENT_MODERATE = "comment-moderate";

    @Override
    public void onCreate() {
        super.onCreate();
        ((WordPress) getApplication()).component().inject(this);
    }

    // Add to the analytics properties map a subset of the push notification payload.
    private static final String[] propertiesToCopyIntoAnalytics = {PUSH_ARG_NOTE_ID, PUSH_ARG_TYPE, "blog_id", "post_id",
            "comment_id"};

    private void synchronizedHandleDefaultPush(@NonNull Bundle data) {
        // sActiveNotificationsMap being static, we can't just synchronize the method
        synchronized (GCMMessageService.class) {
            sNotificationHelper.handleDefaultPush(this, data, mAccountStore.getAccount().getUserId());
        }
    }

    @Override
    public void onMessageReceived(String from, Bundle data) {
        AppLog.v(T.NOTIFS, "Received Message");

        if (data == null) {
            AppLog.v(T.NOTIFS, "No notification message content received. Aborting.");
            return;
        }

        // Handle helpshift PNs
        if (TextUtils.equals(data.getString("origin"), "helpshift")) {
            HelpshiftHelper.getInstance().handlePush(this, new Intent().putExtras(data));
            return;
        }
        
        if (!mAccountStore.hasAccessToken()) {
            return;
        }

        synchronizedHandleDefaultPush(data);
    }

    public static synchronized void rebuildAndUpdateNotificationsOnSystemBarForThisNote(Context context,
                                                                                        String noteId) {
        if (sActiveNotificationsMap.size() > 0) {
            //get the corresponding bundle for this noteId
            for (Map.Entry<Integer, Bundle> row : sActiveNotificationsMap.entrySet()) {
                Bundle noteBundle = row.getValue();
                if (noteBundle.getString(PUSH_ARG_NOTE_ID, "").equals(noteId)) {
                    sNotificationHelper.rebuildAndUpdateNotificationsOnSystemBar(context, noteBundle);
                    return;
                }
            }
        }
    }

    public static synchronized void rebuildAndUpdateNotifsOnSystemBarForRemainingNote(Context context) {
        if (sActiveNotificationsMap.size() > 0) {
            Bundle remainingNote = sActiveNotificationsMap.values().iterator().next();
            sNotificationHelper.rebuildAndUpdateNotificationsOnSystemBar(context, remainingNote);
        }
    }

    public static synchronized Bundle getCurrentNoteBundleForNoteId(String noteId){
        if (sActiveNotificationsMap.size() > 0) {
            //get the corresponding bundle for this noteId
            for(Iterator<Map.Entry<Integer, Bundle>> it = sActiveNotificationsMap.entrySet().iterator(); it.hasNext(); ) {
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
        Bundle authPNBundle = sActiveNotificationsMap.remove(AUTH_PUSH_NOTIFICATION_ID);

        sActiveNotificationsMap.clear();

        //reinsert 2fa bundle if it was present
        if (authPNBundle != null) {
            sActiveNotificationsMap.put(AUTH_PUSH_NOTIFICATION_ID, authPNBundle);
        }
    }

    public static synchronized int getNotificationsCount() {
        return sActiveNotificationsMap.size();
    }

    public static synchronized boolean hasNotifications() {
        return !sActiveNotificationsMap.isEmpty();
    }

    // Removes a specific notification from the internal map - only use this when we know
    // the user has dismissed the app by swiping it off the screen
    public static synchronized void removeNotification(int notificationId) {
        sActiveNotificationsMap.remove(notificationId);
    }

    // Removes a specific notification from the system bar
    public static synchronized void removeNotificationWithNoteIdFromSystemBar(Context context, String noteID) {
        if (context == null || TextUtils.isEmpty(noteID) || !hasNotifications()) {
            return;
        }

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        // here we loop with an Iterator as there might be several Notifications with the same Note ID (i.e. likes on the same Note)
        // so we need to keep cancelling them and removing them from our activeNotificationsMap as we find it suitable
        for(Iterator<Map.Entry<Integer, Bundle>> it = sActiveNotificationsMap.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Integer, Bundle> row = it.next();
            Integer pushId = row.getKey();
            Bundle noteBundle = row.getValue();
            if (noteBundle.getString(PUSH_ARG_NOTE_ID, "").equals(noteID)) {
                notificationManager.cancel(pushId);
                it.remove();
            }
        }

        if (sActiveNotificationsMap.size() == 0) {
            notificationManager.cancel(GCMMessageService.GROUP_NOTIFICATION_ID);
        }
    }

    // Removes all app notifications from the system bar
    public static synchronized void removeAllNotifications(Context context) {
        if (context == null || !hasNotifications()) {
            return;
        }

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        Bundle authPNBundle = sActiveNotificationsMap.remove(AUTH_PUSH_NOTIFICATION_ID);
        for (Integer pushId : sActiveNotificationsMap.keySet()) {
            notificationManager.cancel(pushId);
        }
        notificationManager.cancel(GCMMessageService.GROUP_NOTIFICATION_ID);

        //reinsert 2fa bundle if it was present
        if (authPNBundle != null) {
            sActiveNotificationsMap.put(AUTH_PUSH_NOTIFICATION_ID, authPNBundle);
        }

        clearNotifications();
    }

    public static synchronized void remove2FANotification(Context context) {
        if (context == null || !hasNotifications()) {
            return;
        }

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.cancel(AUTH_PUSH_NOTIFICATION_ID);
        sActiveNotificationsMap.remove(AUTH_PUSH_NOTIFICATION_ID);
    }

    // NoteID is the ID if the note in WordPress
    public static synchronized void bumpPushNotificationsTappedAnalytics(String noteID) {
        for (int id : sActiveNotificationsMap.keySet()) {
            Bundle noteBundle = sActiveNotificationsMap.get(id);
            if (noteBundle.getString(PUSH_ARG_NOTE_ID, "").equals(noteID)) {
                bumpPushNotificationsAnalytics(Stat.PUSH_NOTIFICATION_TAPPED, noteBundle, null);
                AnalyticsTracker.flush();
                return;
            }
        }
    }

    // Mark all notifications as tapped
    public static synchronized void bumpPushNotificationsTappedAllAnalytics() {
        for (int id : sActiveNotificationsMap.keySet()) {
            Bundle noteBundle = sActiveNotificationsMap.get(id);
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
            for (String currentPropertyToCopy : propertiesToCopyIntoAnalytics) {
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
        sActiveNotificationsMap.put(AUTH_PUSH_NOTIFICATION_ID, data);
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

            buildAndShowNotificationFromNoteData(context, data);
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
            for (Integer id : sActiveNotificationsMap.keySet()) {
                if (id == null) {
                    continue;
                }
                Bundle noteBundle = sActiveNotificationsMap.get(id);
                if (noteBundle != null && noteBundle.getString(PUSH_ARG_NOTE_ID, "").equals(wpcomNoteID)) {
                    pushId = id;
                    sActiveNotificationsMap.put(pushId, data);
                    break;
                }
            }

            if (pushId == 0) {
                pushId = PUSH_NOTIFICATION_ID + sActiveNotificationsMap.size();
                sActiveNotificationsMap.put(pushId, data);
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
            Bitmap largeIconBitmap = getLargeIconBitmap(context, data.getString("icon"), shouldCircularizeNoteIcon(noteType));
            if (largeIconBitmap != null) {
                builder.setLargeIcon(largeIconBitmap);
            }

            showSingleNotificationForBuilder(context, builder, noteType, wpcomNoteID, pushId, true);

            // Also add a group summary notification, which is required for non-wearable devices
            // Do not need to play the sound again. We've already played it in the individual builder.
            showGroupNotificationForBuilder(context, builder, wpcomNoteID, message);
        }

        private void addActionsForCommentNotification(Context context, NotificationCompat.Builder builder, String noteId) {
            // Add some actions if this is a comment notification
            boolean areActionsSet = false;
            Note note = NotificationsTable.getNoteById(noteId);
            if (note != null) {
                //if note can be replied to, we'll always add this action first
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

        private void addCommentReplyActionForCommentNotification(Context context, NotificationCompat.Builder builder, String noteId) {
            // adding comment reply action
            Intent commentReplyIntent = getCommentActionReplyIntent(context, noteId);
            commentReplyIntent.addCategory(KEY_CATEGORY_COMMENT_REPLY);
            commentReplyIntent.putExtra(NotificationsProcessingService.ARG_ACTION_TYPE, NotificationsProcessingService.ARG_ACTION_REPLY);
            if (noteId != null) {
                commentReplyIntent.putExtra(NotificationsProcessingService.ARG_NOTE_ID, noteId);
            }
            commentReplyIntent.putExtra(NotificationsProcessingService.ARG_NOTE_BUNDLE, getCurrentNoteBundleForNoteId(noteId));


            PendingIntent commentReplyPendingIntent = getCommentActionPendingIntent(context, commentReplyIntent);

            // The following code adds the behavior for Direct reply, available on Android N (7.0) and on.
            // Using backward compatibility with NotificationCompat.
            String replyLabel = context.getString(R.string.reply);
            RemoteInput remoteInput = new RemoteInput.Builder(EXTRA_VOICE_OR_INLINE_REPLY)
                    .setLabel(replyLabel)
                    .build();
            NotificationCompat.Action action =
                    new NotificationCompat.Action.Builder(R.drawable.ic_reply_grey_32dp,
                            context.getString(R.string.reply), commentReplyPendingIntent)
                            .addRemoteInput(remoteInput)
                            .build();
            // now add the action corresponding to direct-reply
            builder.addAction(action);
        }

        private void addCommentLikeActionForCommentNotification(Context context, NotificationCompat.Builder builder, String noteId) {
            // adding comment like action
            Intent commentLikeIntent = getCommentActionIntent(context);
            commentLikeIntent.addCategory(KEY_CATEGORY_COMMENT_LIKE);
            commentLikeIntent.putExtra(NotificationsProcessingService.ARG_ACTION_TYPE, NotificationsProcessingService.ARG_ACTION_LIKE);
            if (noteId != null) {
                commentLikeIntent.putExtra(NotificationsProcessingService.ARG_NOTE_ID, noteId);
            }
            commentLikeIntent.putExtra(NotificationsProcessingService.ARG_NOTE_BUNDLE, getCurrentNoteBundleForNoteId(noteId));

            PendingIntent commentLikePendingIntent =  getCommentActionPendingIntentForService(context,
                    commentLikeIntent);
            builder.addAction(R.drawable.ic_star_grey_32dp, context.getText(R.string.like), commentLikePendingIntent);
        }

        private void addCommentApproveActionForCommentNotification(Context context, NotificationCompat.Builder builder, String noteId) {
            // adding comment approve action
            Intent commentApproveIntent = getCommentActionIntent(context);
            commentApproveIntent.addCategory(KEY_CATEGORY_COMMENT_MODERATE);
            commentApproveIntent.putExtra(NotificationsProcessingService.ARG_ACTION_TYPE, NotificationsProcessingService.ARG_ACTION_APPROVE);
            if (noteId != null) {
                commentApproveIntent.putExtra(NotificationsProcessingService.ARG_NOTE_ID, noteId);
            }
            commentApproveIntent.putExtra(NotificationsProcessingService.ARG_NOTE_BUNDLE, getCurrentNoteBundleForNoteId(noteId));

            PendingIntent commentApprovePendingIntent =  getCommentActionPendingIntentForService(context,
                    commentApproveIntent);
            builder.addAction(R.drawable.ic_checkmark_grey_32dp, context.getText(R.string.approve),
                    commentApprovePendingIntent);
        }

        private PendingIntent getCommentActionPendingIntent(Context context, Intent intent){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                return getCommentActionPendingIntentForService(context, intent);
            } else {
                return getCommentActionPendingIntentForActivity(context, intent);
            }
        }

        private PendingIntent getCommentActionPendingIntentForService(Context context, Intent intent){
            return PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        }

        private PendingIntent getCommentActionPendingIntentForActivity(Context context, Intent intent){
            return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        }

        private Intent getCommentActionReplyIntent(Context context, String noteId){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                return getCommentActionIntentForService(context);
            } else {
                return getCommentActionIntentForActivity(context, noteId);
            }
        }

        private Intent getCommentActionIntent(Context context){
            return getCommentActionIntentForService(context);
        }

        private Intent getCommentActionIntentForService(Context context){
            return new Intent(context, NotificationsProcessingService.class);
        }

        private Intent getCommentActionIntentForActivity(Context context, String noteId){
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

        private Bitmap getLargeIconBitmap(Context context, String iconUrl, boolean shouldCircularizeIcon){
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

        private NotificationCompat.Builder getNotificationBuilder(Context context, String title, String message){
            // Build the new notification, add group to support wearable stacking
           return new NotificationCompat.Builder(context)
                    .setSmallIcon(R.drawable.ic_my_sites_24dp)
                    .setColor(context.getResources().getColor(R.color.blue_wordpress))
                    .setContentTitle(title)
                    .setContentText(message)
                    .setTicker(message)
                    .setAutoCancel(true)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                    .setGroup(NOTIFICATION_GROUP_KEY);
        }

        private void showGroupNotificationForBuilder(Context context, NotificationCompat.Builder builder,
                                                     String wpcomNoteID, String message) {

            if (builder == null || context == null) {
                return;
            }

            //first remove 2fa push from the map, then reinsert it, so it's not shown in the inbox style group notif
            Bundle authPNBundle = sActiveNotificationsMap.remove(AUTH_PUSH_NOTIFICATION_ID);
            if (sActiveNotificationsMap.size() > 1) {

                NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
                int noteCtr = 1;
                for (Bundle pushBundle : sActiveNotificationsMap.values()) {
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

                if (sActiveNotificationsMap.size() > MAX_INBOX_ITEMS) {
                    inboxStyle.setSummaryText(String.format(context.getString(R.string.more_notifications),
                            sActiveNotificationsMap.size() - MAX_INBOX_ITEMS));
                }

                String subject = String.format(context.getString(R.string.new_notifications), sActiveNotificationsMap.size());
                NotificationCompat.Builder groupBuilder = new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.ic_my_sites_24dp)
                        .setColor(context.getResources().getColor(R.color.blue_wordpress))
                        .setGroup(NOTIFICATION_GROUP_KEY)
                        .setGroupSummary(true)
                        .setAutoCancel(true)
                        .setTicker(message)
                        .setContentTitle(context.getString(R.string.app_name))
                        .setContentText(subject)
                        .setStyle(inboxStyle);

                showNotificationForBuilder(groupBuilder, context, wpcomNoteID, GROUP_NOTIFICATION_ID, false);

            } else {
                // Set the individual notification we've already built as the group summary
                builder.setGroupSummary(true);
                showNotificationForBuilder(builder, context, wpcomNoteID, GROUP_NOTIFICATION_ID, false);
            }
            //reinsert 2fa bundle if it was present
            if (authPNBundle != null) {
                sActiveNotificationsMap.put(AUTH_PUSH_NOTIFICATION_ID, authPNBundle);
            }

        }

        private void showSingleNotificationForBuilder(Context context, NotificationCompat.Builder builder,
                                                      String noteType, String wpcomNoteID, int pushId, boolean notifyUser) {
            if (builder == null || context == null) {
                return;
            }

            if (noteType.equals(PUSH_TYPE_COMMENT)) {
                addActionsForCommentNotification(context, builder, wpcomNoteID);
            }

            showNotificationForBuilder(builder, context, wpcomNoteID, pushId, notifyUser);
        }

        // Displays a notification to the user
        private void showNotificationForBuilder(NotificationCompat.Builder builder, Context context,
                                                String wpcomNoteID, int pushId, boolean notifyUser) {
            if (builder == null || context == null) {
                return;
            }

            Intent resultIntent = new Intent(context, WPMainActivity.class);
            resultIntent.putExtra(WPMainActivity.ARG_OPENED_FROM_PUSH, true);
            resultIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            resultIntent.setAction("android.intent.action.MAIN");
            resultIntent.addCategory("android.intent.category.LAUNCHER");
            resultIntent.putExtra(NotificationsListFragment.NOTE_ID_EXTRA, wpcomNoteID);

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

            if (notifyUser) {
                boolean shouldVibrate = prefs.getBoolean("wp_pref_notification_vibrate", false);
                boolean shouldBlinkLight = prefs.getBoolean("wp_pref_notification_light", true);
                String notificationSound = prefs.getString("wp_pref_custom_notification_sound", "content://settings/system/notification_sound"); //"" if None is selected

                if (!TextUtils.isEmpty(notificationSound)) {
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
                // Do not turn the led off otherwise the previous (single) notification led is not shown. We're re-using the same builder for single and group.
            }

            // Call broadcast receiver when notification is dismissed
            Intent notificationDeletedIntent = new Intent(context, NotificationDismissBroadcastReceiver.class);
            notificationDeletedIntent.putExtra("notificationId", pushId);
            notificationDeletedIntent.setAction(String.valueOf(pushId));
            PendingIntent pendingDeleteIntent =
                    PendingIntent.getBroadcast(context, pushId, notificationDeletedIntent, 0);
            builder.setDeleteIntent(pendingDeleteIntent);

            builder.setCategory(NotificationCompat.CATEGORY_SOCIAL);

            PendingIntent pendingIntent = PendingIntent.getActivity(context, pushId, resultIntent,
                    PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_UPDATE_CURRENT);
            builder.setContentIntent(pendingIntent);
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.notify(pushId, builder.build());
        }

        private void rebuildAndUpdateNotificationsOnSystemBar(Context context, Bundle data) {
            String noteType = StringUtils.notNullStr(data.getString(PUSH_ARG_TYPE));

            // Check for wpcom auth push, if so we will process this push differently
            // and we'll remove the auth special notif out of the map while we re-build the remaining notifs
            Bundle authPNBundle = sActiveNotificationsMap.remove(AUTH_PUSH_NOTIFICATION_ID);
            if (authPNBundle != null) {
                handlePushAuth(context, authPNBundle);
                if (sActiveNotificationsMap.size() > 0 && noteType.equals(PUSH_TYPE_PUSH_AUTH)) {
                    //get the data for the next notification in map for re-build
                    //because otherwise we would be keeping the PUSH_AUTH type note in `data`
                    data = sActiveNotificationsMap.values().iterator().next();
                } else if (noteType.equals(PUSH_TYPE_PUSH_AUTH)) {
                    //only note is the 2fa note, just reinsert it in the map and return
                    sActiveNotificationsMap.put(AUTH_PUSH_NOTIFICATION_ID, authPNBundle);
                    return;
                }
            }


            Bitmap largeIconBitmap = null;
            // here notify the existing group notification by eliminating the line that is now gone
            String title = getNotificationTitleOrAppNameFromBundle(context, data);
            String message = StringEscapeUtils.unescapeHtml4(data.getString(PUSH_ARG_MSG));

            NotificationCompat.Builder builder = null;
            String wpcomNoteID = null;

            if (sActiveNotificationsMap.size() == 1) {
                //only one notification remains, so get the proper message for it and re-instate in the system dashboard
                Bundle remainingNote = sActiveNotificationsMap.values().iterator().next();
                if (remainingNote != null) {
                    String remainingNoteTitle = StringEscapeUtils.unescapeHtml4(remainingNote.getString(PUSH_ARG_TITLE));
                    if (!TextUtils.isEmpty(remainingNoteTitle)) {
                        title = remainingNoteTitle;
                    }
                    String remainingNoteMessage = StringEscapeUtils.unescapeHtml4(remainingNote.getString(PUSH_ARG_MSG));
                    if (!TextUtils.isEmpty(remainingNoteMessage)) {
                        message = remainingNoteMessage;
                    }
                    largeIconBitmap = getLargeIconBitmap(context, remainingNote.getString("icon"),
                            shouldCircularizeNoteIcon(remainingNote.getString(PUSH_ARG_TYPE)));

                    builder = getNotificationBuilder(context, title, message);

                    // set timestamp for note: first try with the notification timestamp, then try google's sent time
                    // if not available; finally just set the system's current time if everything else fails (not likely)
                    long timeStampToShow =
                            DateTimeUtils.timestampFromIso8601Millis(remainingNote.getString("note_timestamp"));
                    timeStampToShow = timeStampToShow != 0 ? timeStampToShow :
                            remainingNote.getLong("google.sent_time", System.currentTimeMillis());
                    builder.setWhen(timeStampToShow);

                    noteType = StringUtils.notNullStr(remainingNote.getString(PUSH_ARG_TYPE));
                    wpcomNoteID = remainingNote.getString(PUSH_ARG_NOTE_ID, "");
                    if (!sActiveNotificationsMap.isEmpty()) {
                        showSingleNotificationForBuilder(context, builder, noteType, wpcomNoteID,
                                sActiveNotificationsMap.keyAt(0), false);
                    }
                }
            }

            if (builder == null) {
                builder = getNotificationBuilder(context, title, message);
            }

            if (largeIconBitmap == null) {
                largeIconBitmap = getLargeIconBitmap(context, data.getString("icon"), shouldCircularizeNoteIcon(PUSH_TYPE_BADGE_RESET));
            }

            if (wpcomNoteID == null) {
                wpcomNoteID = AppPrefs.getLastPushNotificationWpcomNoteId();
            }

            if (largeIconBitmap != null) {
                builder.setLargeIcon(largeIconBitmap);
            }

            showGroupNotificationForBuilder(context, builder,  wpcomNoteID, message);

            //reinsert 2fa bundle if it was present
            if (authPNBundle != null) {
                sActiveNotificationsMap.put(AUTH_PUSH_NOTIFICATION_ID, authPNBundle);
            }
        }

        private String getNotificationTitleOrAppNameFromBundle(Context context, Bundle data){
            String title = StringEscapeUtils.unescapeHtml4(data.getString(PUSH_ARG_TITLE));
            if (title == null) {
                title = context.getString(R.string.app_name);
            }
            return title;
        }

        // Clear all notifications
        private void handleBadgeResetPN(Context context, Bundle data) {
            if (data == null || !data.containsKey(PUSH_ARG_NOTE_ID))  {
                // ignore the reset-badge PN if it's a global one
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
            }

            removeNotificationWithNoteIdFromSystemBar(context, noteID);
            //now that we cleared the specific notif, we can check and make any visual updates
            if (sActiveNotificationsMap.size() > 0) {
                rebuildAndUpdateNotificationsOnSystemBar(context, data);
            }

            EventBus.getDefault().post(new NotificationEvents.NotificationsChanged(sActiveNotificationsMap.size() > 0));
        }

        private void handleNoteDeletePN(Context context, Bundle data) {
            if (data == null || !data.containsKey(PUSH_ARG_NOTE_ID))  {
                return;
            }

            String noteID = data.getString(PUSH_ARG_NOTE_ID, "");
            if (!TextUtils.isEmpty(noteID)) {
                NotificationsTable.deleteNoteById(noteID);
            }

            removeNotificationWithNoteIdFromSystemBar(context, noteID);
            //now that we cleared the specific notif, we can check and make any visual updates
            if (sActiveNotificationsMap.size() > 0) {
                rebuildAndUpdateNotificationsOnSystemBar(context, data);
            }

            EventBus.getDefault().post(new NotificationEvents.NotificationsChanged(sActiveNotificationsMap.size() > 0));
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

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                    .setSmallIcon(R.drawable.ic_my_sites_24dp)
                    .setColor(context.getResources().getColor(R.color.blue_wordpress))
                    .setContentTitle(title)
                    .setContentText(message)
                    .setAutoCancel(true)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                    .setPriority(NotificationCompat.PRIORITY_MAX);

            PendingIntent pendingIntent = PendingIntent.getActivity(context, AUTH_PUSH_REQUEST_CODE_OPEN_DIALOG, pushAuthIntent,
                    PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_UPDATE_CURRENT);
            builder.setContentIntent(pendingIntent);


            // adding ignore / approve quick actions
            Intent authApproveIntent = new Intent(context, WPMainActivity.class);
            authApproveIntent.putExtra(WPMainActivity.ARG_OPENED_FROM_PUSH, true);
            authApproveIntent.putExtra(NotificationsProcessingService.ARG_ACTION_TYPE, NotificationsProcessingService.ARG_ACTION_AUTH_APPROVE);
            authApproveIntent.putExtra(NotificationsUtils.ARG_PUSH_AUTH_TOKEN, pushAuthToken);
            authApproveIntent.putExtra(NotificationsUtils.ARG_PUSH_AUTH_TITLE, title);
            authApproveIntent.putExtra(NotificationsUtils.ARG_PUSH_AUTH_MESSAGE, message);
            authApproveIntent.putExtra(NotificationsUtils.ARG_PUSH_AUTH_EXPIRES, expirationTimestamp);

            authApproveIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            authApproveIntent.setAction("android.intent.action.MAIN");
            authApproveIntent.addCategory("android.intent.category.LAUNCHER");

            PendingIntent authApprovePendingIntent = PendingIntent.getActivity(context, AUTH_PUSH_REQUEST_CODE_APPROVE, authApproveIntent,
                    PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_UPDATE_CURRENT);

            builder.addAction(R.drawable.ic_checkmark_grey_32dp, context.getText(R.string.approve), authApprovePendingIntent);


            Intent authIgnoreIntent = new Intent(context, NotificationsProcessingService.class);
            authIgnoreIntent.putExtra(NotificationsProcessingService.ARG_ACTION_TYPE, NotificationsProcessingService.ARG_ACTION_AUTH_IGNORE);
            PendingIntent authIgnorePendingIntent =  PendingIntent.getService(context,
                    AUTH_PUSH_REQUEST_CODE_IGNORE, authIgnoreIntent, PendingIntent.FLAG_CANCEL_CURRENT);
            builder.addAction(R.drawable.ic_close_white_24dp, context.getText(R.string.ignore), authIgnorePendingIntent);


            // Call broadcast receiver when notification is dismissed
            Intent notificationDeletedIntent = new Intent(context, NotificationDismissBroadcastReceiver.class);
            notificationDeletedIntent.putExtra("notificationId", AUTH_PUSH_NOTIFICATION_ID);
            notificationDeletedIntent.setAction(String.valueOf(AUTH_PUSH_NOTIFICATION_ID));
            PendingIntent pendingDeleteIntent =
                    PendingIntent.getBroadcast(context, AUTH_PUSH_NOTIFICATION_ID, notificationDeletedIntent, 0);
            builder.setDeleteIntent(pendingDeleteIntent);

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.notify(AUTH_PUSH_NOTIFICATION_ID, builder.build());
        }

        // Returns true if the note type is known to have a gravatar
        public boolean shouldCircularizeNoteIcon(String noteType) {
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
    }
}
