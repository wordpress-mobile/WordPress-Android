
package org.wordpress.android;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.util.ArrayMap;
import android.text.TextUtils;

import com.google.android.gms.gcm.GcmListenerService;

import org.apache.commons.lang.StringEscapeUtils;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.analytics.AnalyticsTracker.Stat;
import org.wordpress.android.analytics.AnalyticsTrackerMixpanel;
import org.wordpress.android.models.AccountHelper;
import org.wordpress.android.ui.main.WPMainActivity;
import org.wordpress.android.ui.notifications.NotificationDismissBroadcastReceiver;
import org.wordpress.android.ui.notifications.NotificationEvents;
import org.wordpress.android.ui.notifications.NotificationsListFragment;
import org.wordpress.android.ui.notifications.utils.NotificationsUtils;
import org.wordpress.android.ui.notifications.utils.SimperiumUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.HelpshiftHelper;
import org.wordpress.android.util.ImageUtils;
import org.wordpress.android.util.PhotonUtils;
import org.wordpress.android.util.StringUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import de.greenrobot.event.EventBus;

public class GCMMessageService extends GcmListenerService {
    private static final ArrayMap<Integer, Bundle> sActiveNotificationsMap = new ArrayMap<>();
    private static String sPreviousNoteId = null;
    private static long sPreviousNoteTime = 0L;

    private static final String NOTIFICATION_GROUP_KEY = "notification_group_key";
    private static final int PUSH_NOTIFICATION_ID = 10000;
    private static final int AUTH_PUSH_NOTIFICATION_ID = 20000;
    public static final int GROUP_NOTIFICATION_ID = 30000;
    private static final int MAX_INBOX_ITEMS = 5;

    private static final String PUSH_ARG_USER = "user";
    private static final String PUSH_ARG_TYPE = "type";
    private static final String PUSH_ARG_TITLE = "title";
    private static final String PUSH_ARG_MSG = "msg";
    private static final String PUSH_ARG_NOTE_ID = "note_id";

    private static final String PUSH_TYPE_COMMENT = "c";
    private static final String PUSH_TYPE_LIKE = "like";
    private static final String PUSH_TYPE_COMMENT_LIKE = "comment_like";
    private static final String PUSH_TYPE_AUTOMATTCHER = "automattcher";
    private static final String PUSH_TYPE_FOLLOW = "follow";
    private static final String PUSH_TYPE_REBLOG = "reblog";
    private static final String PUSH_TYPE_PUSH_AUTH = "push_auth";

    // Add to the analytics properties map a subset of the push notification payload.
    private static String[] propertiesToCopyIntoAnalytics = {PUSH_ARG_NOTE_ID, PUSH_ARG_TYPE, "blog_id", "post_id",
            "comment_id"};

    private void synchronizedHandleDefaultPush(String from, @NonNull Bundle data) {
        // sActiveNotificationsMap being static, we can't just synchronize the method
        synchronized (sActiveNotificationsMap) {
            handleDefaultPush(from, data);
        }
    }

    private void handleDefaultPush(String from, @NonNull Bundle data) {
        // Ensure Simperium is running so that notes sync
        SimperiumUtils.configureSimperium(this, AccountHelper.getDefaultAccount().getAccessToken());

        long wpcomUserId = AccountHelper.getDefaultAccount().getUserId();
        String pushUserId = data.getString(PUSH_ARG_USER);
        // pushUserId is always set server side, but better to double check it here.
        if (!String.valueOf(wpcomUserId).equals(pushUserId)) {
            AppLog.e(T.NOTIFS, "wpcom userId found in the app doesn't match with the ID in the PN. Aborting.");
            return;
        }

        String noteType = StringUtils.notNullStr(data.getString(PUSH_ARG_TYPE));

        // Check for wpcom auth push, if so we will process this push differently
        if (noteType.equals(PUSH_TYPE_PUSH_AUTH)) {
            handlePushAuth(from, data);
            return;
        }


        String title = StringEscapeUtils.unescapeHtml(data.getString(PUSH_ARG_TITLE));
        if (title == null) {
            title = getString(R.string.app_name);
        }
        String message = StringEscapeUtils.unescapeHtml(data.getString(PUSH_ARG_MSG));
        String noteId = data.getString(PUSH_ARG_NOTE_ID, "");

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
        if (sPreviousNoteId != null && sPreviousNoteId.equals(noteId)) {
            long seconds = TimeUnit.MILLISECONDS.toSeconds(thisTime - sPreviousNoteTime);
            if (seconds <= 1) {
                AppLog.w(T.NOTIFS, "skipped potential duplicate notification");
                return;
            }
        }

        sPreviousNoteId = noteId;
        sPreviousNoteTime = thisTime;

        // Update notification content for the same noteId if it is already showing
        int pushId = 0;
        for (Integer id : sActiveNotificationsMap.keySet()) {
            if (id == null) {
                continue;
            }
            Bundle noteBundle = sActiveNotificationsMap.get(id);
            if (noteBundle != null && noteBundle.getString(PUSH_ARG_NOTE_ID, "").equals(noteId)) {
                pushId = id;
                sActiveNotificationsMap.put(pushId, data);
                break;
            }
        }

        if (pushId == 0) {
            pushId = PUSH_NOTIFICATION_ID + sActiveNotificationsMap.size();
            sActiveNotificationsMap.put(pushId, data);
        }

        String iconUrl = data.getString("icon");
        Bitmap largeIconBitmap = null;
        if (iconUrl != null) {
            try {
                iconUrl = URLDecoder.decode(iconUrl, "UTF-8");
                int largeIconSize = getResources().getDimensionPixelSize(
                        android.R.dimen.notification_large_icon_height);
                String resizedUrl = PhotonUtils.getPhotonImageUrl(iconUrl, largeIconSize, largeIconSize);
                largeIconBitmap = ImageUtils.downloadBitmap(resizedUrl);
                if (largeIconBitmap != null && shouldCircularizeNoteIcon(noteType)) {
                    largeIconBitmap = ImageUtils.getCircularBitmap(largeIconBitmap);
                }
            } catch (UnsupportedEncodingException e) {
                AppLog.e(T.NOTIFS, e);
            }
        }

        // Bump Analytics for PNs if "Show notifications" setting is checked (default). Skip otherwise.
        if (NotificationsUtils.isNotificationsEnabled(this)) {
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

        NotificationCompat.Builder builder;

        // Build the new notification, add group to support wearable stacking
        builder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.notification_icon)
                .setColor(getResources().getColor(R.color.blue_wordpress))
                .setContentTitle(title)
                .setContentText(message)
                .setTicker(message)
                .setAutoCancel(true)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setGroup(NOTIFICATION_GROUP_KEY);

        // Add some actions if this is a comment notification
        if (noteType.equals(PUSH_TYPE_COMMENT)) {
            Intent commentReplyIntent = new Intent(this, WPMainActivity.class);
            commentReplyIntent.putExtra(WPMainActivity.ARG_OPENED_FROM_PUSH, true);
            commentReplyIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            commentReplyIntent.setAction("android.intent.action.MAIN");
            commentReplyIntent.addCategory("android.intent.category.LAUNCHER");
            commentReplyIntent.addCategory("comment-reply");
            commentReplyIntent.putExtra(NotificationsListFragment.NOTE_INSTANT_REPLY_EXTRA, true);
            if (noteId != null) {
                commentReplyIntent.putExtra(NotificationsListFragment.NOTE_ID_EXTRA, noteId);
            }
            PendingIntent commentReplyPendingIntent = PendingIntent.getActivity(this, 0, commentReplyIntent,
                    PendingIntent.FLAG_CANCEL_CURRENT);
            builder.addAction(R.drawable.ic_reply_white_24dp, getText(R.string.reply),
                    commentReplyPendingIntent);
        }

        if (largeIconBitmap != null) {
            builder.setLargeIcon(largeIconBitmap);
        }

        showNotificationForBuilder(builder, this, pushId);

        // Also add a group summary notification, which is required for non-wearable devices
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
                    String pnTitle = StringEscapeUtils.unescapeHtml((pushBundle.getString(PUSH_ARG_TITLE)));
                    String pnMessage = StringEscapeUtils.unescapeHtml((pushBundle.getString(PUSH_ARG_MSG)));
                    inboxStyle.addLine(pnTitle + ": " + pnMessage);
                } else {
                    String pnMessage = StringEscapeUtils.unescapeHtml((pushBundle.getString(PUSH_ARG_MSG)));
                    inboxStyle.addLine(pnMessage);
                }

                noteCtr++;
            }

            if (sActiveNotificationsMap.size() > MAX_INBOX_ITEMS) {
                inboxStyle.setSummaryText(String.format(getString(R.string.more_notifications),
                        sActiveNotificationsMap.size() - MAX_INBOX_ITEMS));
            }

            String subject = String.format(getString(R.string.new_notifications), sActiveNotificationsMap.size());
            NotificationCompat.Builder groupBuilder = new NotificationCompat.Builder(this)
                    .setSmallIcon(R.drawable.notification_icon)
                    .setColor(getResources().getColor(R.color.blue_wordpress))
                    .setGroup(NOTIFICATION_GROUP_KEY)
                    .setGroupSummary(true)
                    .setAutoCancel(true)
                    .setTicker(message)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText(subject)
                    .setStyle(inboxStyle);

            showNotificationForBuilder(groupBuilder, this, GROUP_NOTIFICATION_ID);
        } else {
            // Set the individual notification we've already built as the group summary
            builder.setGroupSummary(true);
            showNotificationForBuilder(builder, this, GROUP_NOTIFICATION_ID);
        }

        EventBus.getDefault().post(new NotificationEvents.NotificationsChanged());
    }

    // Displays a notification to the user
    private void showNotificationForBuilder(NotificationCompat.Builder builder, Context context, int notificationId) {
        if (builder == null || context == null) {
            return;
        }

        Intent resultIntent = new Intent(this, WPMainActivity.class);
        resultIntent.putExtra(WPMainActivity.ARG_OPENED_FROM_PUSH, true);
        resultIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        resultIntent.setAction("android.intent.action.MAIN");
        resultIntent.addCategory("android.intent.category.LAUNCHER");
        if (sPreviousNoteId != null) {
            resultIntent.putExtra(NotificationsListFragment.NOTE_ID_EXTRA, sPreviousNoteId);
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        boolean shouldPlaySound = prefs.getBoolean("wp_pref_notification_sound", false);
        boolean shouldVibrate = prefs.getBoolean("wp_pref_notification_vibrate", false);
        boolean shouldBlinkLight = prefs.getBoolean("wp_pref_notification_light", false);
        String notificationSound = prefs.getString("wp_pref_custom_notification_sound", null); //"" if None is selected


        // use default sound if the legacy sound preference was ON but the custom sound was not selected (null)
        if (shouldPlaySound && notificationSound == null) {
            builder.setSound(Uri.parse("content://settings/system/notification_sound"));
        } else if (!TextUtils.isEmpty(notificationSound)) {
            builder.setSound(Uri.parse(notificationSound));
        }

        if (shouldVibrate) {
            builder.setVibrate(new long[]{500, 500, 500});
        }
        if (shouldBlinkLight) {
            builder.setLights(0xff0000ff, 1000, 5000);
        }

        // Call broadcast receiver when notification is dismissed
        Intent notificationDeletedIntent = new Intent(this, NotificationDismissBroadcastReceiver.class);
        notificationDeletedIntent.putExtra("notificationId", notificationId);
        notificationDeletedIntent.setAction(String.valueOf(notificationId));
        PendingIntent pendingDeleteIntent =
                PendingIntent.getBroadcast(context, notificationId, notificationDeletedIntent, 0);
        builder.setDeleteIntent(pendingDeleteIntent);

        builder.setCategory(NotificationCompat.CATEGORY_SOCIAL);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, notificationId, resultIntent,
                PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pendingIntent);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(notificationId, builder.build());
    }

    // Show a notification for two-step auth users who sign in from a web browser
    private void handlePushAuth(String from, Bundle data) {
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
        Intent pushAuthIntent = new Intent(this, WPMainActivity.class);
        pushAuthIntent.putExtra(WPMainActivity.ARG_OPENED_FROM_PUSH, true);
        pushAuthIntent.putExtra(NotificationsUtils.ARG_PUSH_AUTH_TOKEN, pushAuthToken);
        pushAuthIntent.putExtra(NotificationsUtils.ARG_PUSH_AUTH_TITLE, title);
        pushAuthIntent.putExtra(NotificationsUtils.ARG_PUSH_AUTH_MESSAGE, message);
        pushAuthIntent.putExtra(NotificationsUtils.ARG_PUSH_AUTH_EXPIRES, expirationTimestamp);
        pushAuthIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        pushAuthIntent.setAction("android.intent.action.MAIN");
        pushAuthIntent.addCategory("android.intent.category.LAUNCHER");

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.notification_icon)
                .setColor(getResources().getColor(R.color.blue_wordpress))
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_MAX);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 1, pushAuthIntent,
                PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pendingIntent);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(AUTH_PUSH_NOTIFICATION_ID, builder.build());
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

        // Handle mixpanel PNs
        if (data.containsKey("mp_message")) {
            String mpMessage = data.getString("mp_message");
            String title = getString(R.string.app_name);
            Intent resultIntent = new Intent(this, WPMainActivity.class);
            resultIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, resultIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            AnalyticsTrackerMixpanel.showNotification(this, pendingIntent,
                    R.drawable.notification_icon, title, mpMessage);
            return;
        }

        if (!AccountHelper.isSignedInWordPressDotCom()) {
            return;
        }

        synchronizedHandleDefaultPush(from, data);
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

    public static synchronized void clearNotifications() {
        sActiveNotificationsMap.clear();
    }

    public static synchronized int getNotificationsCount() {
        return sActiveNotificationsMap.size();
    }

    public static synchronized boolean hasNotifications() {
        return !sActiveNotificationsMap.isEmpty();
    }

    public static synchronized void removeNotification(int notificationId) {
        sActiveNotificationsMap.remove(notificationId);
    }

    // Removes all app notifications from the system bar
    public static synchronized void removeAllNotifications(Context context) {
        if (context == null || !hasNotifications()) {
            return;
        }

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        for (Integer pushId : sActiveNotificationsMap.keySet()) {
            notificationManager.cancel(pushId);
        }
        notificationManager.cancel(GCMMessageService.GROUP_NOTIFICATION_ID);

        clearNotifications();
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
}
