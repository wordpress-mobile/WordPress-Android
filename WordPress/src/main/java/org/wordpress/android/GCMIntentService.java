
package org.wordpress.android;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;

import com.google.android.gcm.GCMBaseIntentService;

import org.apache.commons.lang.StringEscapeUtils;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.analytics.AnalyticsTracker.Stat;
import org.wordpress.android.analytics.AnalyticsTrackerMixpanel;
import org.wordpress.android.ui.main.WPMainActivity;
import org.wordpress.android.ui.notifications.NotificationDismissBroadcastReceiver;
import org.wordpress.android.ui.notifications.NotificationEvents;
import org.wordpress.android.ui.notifications.NotificationsListFragment;
import org.wordpress.android.ui.notifications.utils.NotificationsUtils;
import org.wordpress.android.util.ABTestingUtils;
import org.wordpress.android.util.ABTestingUtils.Feature;
import org.wordpress.android.util.AccountHelper;
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
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import de.greenrobot.event.EventBus;

public class GCMIntentService extends GCMBaseIntentService {
    public static final int PUSH_NOTIFICATION_ID = 1337;
    private static final int AUTH_PUSH_NOTIFICATION_ID = 1338;

    private static final Map<String, Bundle> mActiveNotificationsMap = new HashMap<>();
    private static String mPreviousNoteId = null;
    private static long mPreviousNoteTime = 0L;
    private static final int mMaxInboxItems = 5;

    private static final String PUSH_TYPE_COMMENT = "c";
    private static final String PUSH_TYPE_LIKE = "like";
    private static final String PUSH_TYPE_COMMENT_LIKE = "comment_like";
    private static final String PUSH_TYPE_AUTOMATTCHER = "automattcher";
    private static final String PUSH_TYPE_FOLLOW = "follow";
    private static final String PUSH_TYPE_REBLOG = "reblog";
    private static final String PUSH_TYPE_PUSH_AUTH = "push_auth";

    @Override
    protected String[] getSenderIds(Context context) {
        String[] senderIds = new String[1];
        senderIds[0] = BuildConfig.GCM_ID;
        return senderIds;
    }

    @Override
    protected void onError(Context context, String errorId) {
        AppLog.e(T.NOTIFS, "GCM Error: " + errorId);
    }

    private void handleDefaultPush(Context context, Bundle extras) {
        long wpcomUserID = AccountHelper.getDefaultAccount().getUserId();
        String userIDFromPN = extras.getString("user");
        // userIDFromPN is always set server side, but better to double check it here.
        if (userIDFromPN != null) {
            if (!String.valueOf(wpcomUserID).equals(userIDFromPN)) {
                AppLog.e(T.NOTIFS, "wpcom userId found in the app doesn't match with the ID in the PN. Aborting.");
                return;
            }
        }

        String noteType = StringUtils.notNullStr(extras.getString("type"));

        // Check for wpcom auth push, if so we will process this push differently
        if (noteType.equals(PUSH_TYPE_PUSH_AUTH)) {
            handlePushAuth(context, extras);
            return;
        }

        String title = StringEscapeUtils.unescapeHtml(extras.getString("title"));
        if (title == null) {
            title = getString(R.string.app_name);
        }
        String message = StringEscapeUtils.unescapeHtml(extras.getString("msg"));
        String note_id = extras.getString("note_id");

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
        if (mPreviousNoteId != null && mPreviousNoteId.equals(note_id)) {
            long seconds = TimeUnit.MILLISECONDS.toSeconds(thisTime - mPreviousNoteTime);
            if (seconds <= 1) {
                AppLog.w(T.NOTIFS, "skipped potential duplicate notification");
                return;
            }
        }

        mPreviousNoteId = note_id;
        mPreviousNoteTime = thisTime;

        if (note_id != null && !mActiveNotificationsMap.containsKey(note_id)) {
            mActiveNotificationsMap.put(note_id, extras);
        }

        String iconUrl = extras.getString("icon");
        Bitmap largeIconBitmap = null;
        if (iconUrl != null) {
            try {
                iconUrl = URLDecoder.decode(iconUrl, "UTF-8");
                int largeIconSize = context.getResources().getDimensionPixelSize(
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

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean sound, vibrate, light;

        sound = prefs.getBoolean("wp_pref_notification_sound", false);
        vibrate = prefs.getBoolean("wp_pref_notification_vibrate", false);
        light = prefs.getBoolean("wp_pref_notification_light", false);

        NotificationCompat.Builder mBuilder;

        Intent resultIntent = new Intent(this, WPMainActivity.class);
        resultIntent.putExtra(WPMainActivity.ARG_OPENED_FROM_PUSH, true);
        resultIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        resultIntent.setAction("android.intent.action.MAIN");
        resultIntent.addCategory("android.intent.category.LAUNCHER");

        if (mActiveNotificationsMap.size() <= 1) {
            mBuilder = new NotificationCompat.Builder(this)
                    .setSmallIcon(R.drawable.notification_icon)
                    .setColor(getResources().getColor(R.color.blue_wordpress))
                    .setContentTitle(title)
                    .setContentText(message)
                    .setTicker(message)
                    .setAutoCancel(true)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(message));

            if (note_id != null) {
                resultIntent.putExtra(NotificationsListFragment.NOTE_ID_EXTRA, note_id);
            }

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
                if (note_id != null) {
                    commentReplyIntent.putExtra(NotificationsListFragment.NOTE_ID_EXTRA, note_id);
                }
                PendingIntent commentReplyPendingIntent = PendingIntent.getActivity(context, 0, commentReplyIntent,
                        PendingIntent.FLAG_CANCEL_CURRENT);
                mBuilder.addAction(R.drawable.ic_reply_white_24dp, context.getText(R.string.reply),
                        commentReplyPendingIntent);
            }

            if (largeIconBitmap != null) {
                mBuilder.setLargeIcon(largeIconBitmap);
            }
        } else {
            NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();

            int noteCtr = 1;
            for (Bundle wpPN : mActiveNotificationsMap.values()) {
                if (noteCtr > mMaxInboxItems) // InboxStyle notification is limited to 5 lines
                    break;
                if (wpPN.getString("msg") == null)
                    continue;
                if (wpPN.getString("type") != null && wpPN.getString("type").equals(PUSH_TYPE_COMMENT)) {
                    String pnTitle = StringEscapeUtils.unescapeHtml((wpPN.getString("title")));
                    String pnMessage = StringEscapeUtils.unescapeHtml((wpPN.getString("msg")));
                    inboxStyle.addLine(pnTitle + ": " + pnMessage);
                } else {
                    String pnMessage = StringEscapeUtils.unescapeHtml((wpPN.getString("msg")));
                    inboxStyle.addLine(pnMessage);
                }

                noteCtr++;
            }

            if (mActiveNotificationsMap.size() > mMaxInboxItems) {
                inboxStyle.setSummaryText(String.format(getString(R.string.more_notifications),
                        mActiveNotificationsMap.size() - mMaxInboxItems));
            }

            String subject = String.format(getString(R.string.new_notifications), mActiveNotificationsMap.size());

            mBuilder = new NotificationCompat.Builder(this)
                    .setSmallIcon(R.drawable.notification_icon)
                    .setColor(getResources().getColor(R.color.blue_wordpress))
                    .setContentTitle("WordPress")
                    .setContentText(subject)
                    .setTicker(message)
                    .setAutoCancel(true)
                    .setStyle(inboxStyle);
        }

        // Call broadcast receiver when notification is dismissed
        Intent notificationDeletedIntent = new Intent(this, NotificationDismissBroadcastReceiver.class);
        PendingIntent pendingDeleteIntent = PendingIntent.getBroadcast(context, 0, notificationDeletedIntent, 0);
        mBuilder.setDeleteIntent(pendingDeleteIntent);

        if (sound) {
            mBuilder.setSound(Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.notification));
        }
        if (vibrate) {
            mBuilder.setVibrate(new long[]{500, 500, 500});
        }
        if (light) {
            mBuilder.setLights(0xff0000ff, 1000, 5000);
        }

        mBuilder.setCategory(NotificationCompat.CATEGORY_SOCIAL);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, resultIntent,
                PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(pendingIntent);
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(PUSH_NOTIFICATION_ID, mBuilder.build());

        EventBus.getDefault().post(new NotificationEvents.NotificationsChanged());
    }

    // Show a notification for two-step auth users who sign in from a web browser
    private void handlePushAuth(Context context, Bundle extras) {
        if (context == null || extras == null) return;

        String pushAuthToken = extras.getString("push_auth_token", "");
        String title = extras.getString("title", "");
        String message = extras.getString("msg", "");
        long expirationTimestamp = Long.valueOf(extras.getString("expires", "0"));

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

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 1, pushAuthIntent,
                PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(AUTH_PUSH_NOTIFICATION_ID, builder.build());
    }

    @Override
    protected void onMessage(Context context, Intent intent) {
        AppLog.v(T.NOTIFS, "Received Message");
        AnalyticsTracker.track(Stat.PUSH_NOTIFICATION_RECEIVED);
        Bundle extras = intent.getExtras();

        if (extras == null) {
            AppLog.v(T.NOTIFS, "No notification message content received. Aborting.");
            return;
        }

        // Handle helpshift PNs
        if (TextUtils.equals(extras.getString("origin"), "helpshift")) {
            HelpshiftHelper.getInstance().handlePush(context, intent);
            return;
        }

        // Handle mixpanel PNs
        if (extras.containsKey("mp_message")) {
            String mpMessage = intent.getExtras().getString("mp_message");
            String title = getString(R.string.app_name);
            Intent resultIntent = new Intent(this, WPMainActivity.class);
            resultIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, resultIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            AnalyticsTrackerMixpanel.showNotification(context, pendingIntent, R.drawable.notification_icon, title,
                    mpMessage);
            return;
        }

        if (!AccountHelper.getDefaultAccount().hasAccessToken()) {
            return;
        }

        handleDefaultPush(context, extras);
    }

    @Override
    protected void onRegistered(Context context, String regId) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        if (!TextUtils.isEmpty(regId)) {
            // Get or create UUID for WP.com notes api
            String uuid = settings.getString(NotificationsUtils.WPCOM_PUSH_DEVICE_UUID, null);
            if (uuid == null) {
                uuid = UUID.randomUUID().toString();
                SharedPreferences.Editor editor = settings.edit();
                editor.putString(NotificationsUtils.WPCOM_PUSH_DEVICE_UUID, uuid);
                editor.apply();
            }

            NotificationsUtils.registerDeviceForPushNotifications(context, regId);

            if (ABTestingUtils.isFeatureEnabled(Feature.HELPSHIFT)) {
                HelpshiftHelper.getInstance().registerDeviceToken(context, regId);
            }
            AnalyticsTracker.registerPushNotificationToken(regId);
        }
    }

    @Override
    protected void onUnregistered(Context context, String regId) {
        AppLog.v(T.NOTIFS, "GCM Unregistered ID: " + regId);
    }

    // Returns true if the note type is known to have a gravatar
    public boolean shouldCircularizeNoteIcon(String noteType) {
        if (TextUtils.isEmpty(noteType)) return false;

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

    public static void clearNotificationsMap() {
        mActiveNotificationsMap.clear();
    }
}
