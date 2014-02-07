
package org.wordpress.android;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.IntentCompat;
import android.util.Base64;

import com.google.android.gcm.GCMBaseIntentService;
import com.wordpress.rest.RestRequest;

import org.apache.commons.lang.StringEscapeUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.models.Note;
import org.wordpress.android.ui.notifications.NotificationUtils;
import org.wordpress.android.ui.notifications.NotificationsActivity;
import org.wordpress.android.ui.posts.PostsActivity;
import org.wordpress.android.ui.prefs.UserPrefs;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.ImageHelper;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class GCMIntentService extends GCMBaseIntentService {

    public static Map<String, Bundle> activeNotificationsMap = new HashMap<String, Bundle>();
    private int notificationId = 1337;

    @Override
    protected String[] getSenderIds(Context context) {
        String[] senderIds = new String[1];
        senderIds[0] = Config.GCM_ID;
        return senderIds;
    }

    @Override
    protected void onError(Context context, String errorId) {
        AppLog.e(T.NOTIFS, "GCM Error: " + errorId);
    }

    private static String mPreviousNoteId = null;
    private static long mPreviousNoteTime = 0L;

    @Override
    protected void onMessage(Context context, Intent intent) {
        AppLog.v(T.NOTIFS, "Received Message");

        if (!WordPress.hasValidWPComCredentials(context))
            return;
        
        Bundle extras = intent.getExtras();

        if (extras == null) {
            AppLog.v(T.NOTIFS, "Hrm. No notification message content received. Aborting.");
            return;
        }

        long wpcomUserID = UserPrefs.getCurrentUserId();
        String userIDFromPN = extras.getString("user");
        if (userIDFromPN != null) { //It is always populated server side, but better to double check it here.
            if (wpcomUserID <= 0) {
                //TODO: Do not abort the execution here, at least for this release, since there might be an issue for users that update the app. 
                //If they have never used the Reader, then they won't have a userId.
                //Code for next release is below:
               /* AppLog.e(T.NOTIFS, "Hrm. No wpcom userId found in the app. Aborting.");
                return;*/
            } else {
                if (!String.valueOf(wpcomUserID).equals(userIDFromPN)) {
                    AppLog.e(T.NOTIFS, "Hrm. wpcom userId found in the app doesn't match with the ID in the PN. Aborting.");
                    return;
                }
            }
        }

        String title = StringEscapeUtils.unescapeHtml(extras.getString("title"));
        if (title == null)
            title = "WordPress";
        String message = StringEscapeUtils.unescapeHtml(extras.getString("msg"));
        String note_id = extras.getString("note_id");

        Note note = null;
        if (extras.getString("note_full_data") != null) {
            byte[] decode = Base64.decode(intent.getStringExtra("note_full_data"), Base64.DEFAULT);
            String unzippedString = NotificationUtils.unzipString(decode);
            JSONObject jsonObject = null;
            try {
                jsonObject = new JSONObject(unzippedString);
                JSONArray notesJSON = jsonObject.getJSONArray("notes");
                note = new Note(notesJSON.getJSONObject(0));
                WordPress.wpDB.addNote(note, false);
            } catch (JSONException e) {
                AppLog.e(T.NOTIFS, "Can't parse restRequest JSON response, notifications: ", e);
            }
        } else { // create a placeholder note
            note = new Note(extras);
            WordPress.wpDB.addNote(note, true);
            refreshNotes();
        }

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

        if (note_id != null) {
            if (!activeNotificationsMap.containsKey(note_id))
                activeNotificationsMap.put(note_id, extras);
        }

        String iconURL = extras.getString("icon");
        Bitmap largeIconBitmap = null;
        if (iconURL != null) {
            try {
                iconURL = URLDecoder.decode(iconURL, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            float screenDensity = getResources().getDisplayMetrics().densityDpi;
            int size = Math.round(64 * (screenDensity / 160));
            String resizedURL = iconURL.replaceAll("(?<=[?&;])s=[0-9]*", "s=" + size);
            largeIconBitmap = ImageHelper.downloadBitmap(resizedURL);
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean sound, vibrate, light;

        sound = prefs.getBoolean("wp_pref_notification_sound", false);
        vibrate = prefs.getBoolean("wp_pref_notification_vibrate", false);
        light = prefs.getBoolean("wp_pref_notification_light", false);

        NotificationCompat.Builder mBuilder;

        Intent resultIntent = new Intent(this, PostsActivity.class);
        resultIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK
                | IntentCompat.FLAG_ACTIVITY_CLEAR_TASK);
        resultIntent.setAction("android.intent.action.MAIN");
        resultIntent.addCategory("android.intent.category.LAUNCHER");
        resultIntent.putExtra(NotificationsActivity.FROM_NOTIFICATION_EXTRA, true);

        if (activeNotificationsMap.size() <= 1) {
            mBuilder =
                    new NotificationCompat.Builder(this)
                            .setSmallIcon(R.drawable.notification_icon)
                            .setContentTitle(title)
                            .setContentText(message)
                            .setTicker(message)
                            .setAutoCancel(true)
                            .setStyle(new NotificationCompat.BigTextStyle().bigText(message));

            if (note_id != null)
                resultIntent.putExtra(NotificationsActivity.NOTE_ID_EXTRA, note_id);

            // Add some actions if this is a comment notification
            String noteType = extras.getString("type");
            if (noteType != null && noteType.equals("c")) {
                Intent commentReplyIntent = new Intent(this, PostsActivity.class);
                commentReplyIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK
                        | IntentCompat.FLAG_ACTIVITY_CLEAR_TASK);
                commentReplyIntent.setAction("android.intent.action.MAIN");
                commentReplyIntent.addCategory("android.intent.category.LAUNCHER");
                commentReplyIntent.addCategory("comment-reply");
                commentReplyIntent.putExtra(NotificationsActivity.FROM_NOTIFICATION_EXTRA, true);
                commentReplyIntent.putExtra(NotificationsActivity.NOTE_INSTANT_REPLY_EXTRA, true);
                if (note_id != null)
                    commentReplyIntent.putExtra(NotificationsActivity.NOTE_ID_EXTRA, note_id);
                PendingIntent commentReplyPendingIntent = PendingIntent.getActivity(context, 0,
                        commentReplyIntent,
                        PendingIntent.FLAG_CANCEL_CURRENT);
                mBuilder.addAction(R.drawable.ab_icon_reply,
                        getResources().getText(R.string.reply), commentReplyPendingIntent);
            }

            if (largeIconBitmap != null) {
                mBuilder.setLargeIcon(largeIconBitmap);
            }
        } else {

            NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();

            int noteCtr = 1;
            for (Bundle wpPN : activeNotificationsMap.values()) {
                if (noteCtr > 5) // InboxStyle notification is limited to 5 lines
                    break;
                if (wpPN.getString("msg") == null)
                    continue;
                if (wpPN.getString("type") != null && wpPN.getString("type").equals("c")) {
                    String pnTitle = StringEscapeUtils.unescapeHtml((wpPN.getString("title")));
                    String pnMessage = StringEscapeUtils.unescapeHtml((wpPN.getString("msg")));
                    inboxStyle.addLine(pnTitle + ": " + pnMessage);
                } else {
                    String pnMessage = StringEscapeUtils.unescapeHtml((wpPN.getString("msg")));
                    inboxStyle.addLine(pnMessage);
                }
                noteCtr++;
            }

            if (activeNotificationsMap.size() > 5)
                inboxStyle.setSummaryText(String.format(getString(R.string.more_notifications),
                        activeNotificationsMap.size() - 5));

            String subject = String.format(getString(R.string.new_notifications),
                    activeNotificationsMap.size());

            mBuilder =
                    new NotificationCompat.Builder(this)
                            .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.notification_multi))
                            .setSmallIcon(R.drawable.notification_icon)
                            .setContentTitle("WordPress")
                            .setContentText(subject)
                            .setTicker(message)
                            .setAutoCancel(true)
                            .setStyle(inboxStyle);
        }

        if (sound)
            mBuilder.setSound(Uri.parse("android.resource://" + getPackageName() + "/"
                    + R.raw.notification));
        if (vibrate)
            mBuilder.setVibrate(new long[]{
                    500, 500, 500
            });
        if (light)
            mBuilder.setLights(0xff0000ff, 1000, 5000);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, resultIntent,
                PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(pendingIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(notificationId, mBuilder.build());
        broadcastNewNotification();
    }

    public void broadcastNewNotification() {
        Intent msgIntent = new Intent();
        msgIntent.setAction(NotificationsActivity.NOTIFICATION_ACTION);
        sendBroadcast(msgIntent);
    }

    public void refreshNotes() {
        NotificationUtils.refreshNotifications(new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                try {
                    List<Note> notes = NotificationUtils.parseNotes(jsonObject);
                    WordPress.wpDB.clearNotes();
                    WordPress.wpDB.saveNotes(notes);
                    broadcastNewNotification();
                } catch (JSONException e) {
                    AppLog.e(T.NOTIFS, "Can't parse restRequest JSON response, notifications: " + e);
                }
            }
        }, null);
    }

    @Override
    protected void onRegistered(Context context, String regId) {

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        if (regId != null && regId.length() > 0) {
            // Get or create UUID for WP.com notes api
            String uuid = settings.getString(NotificationUtils.WPCOM_PUSH_DEVICE_UUID, null);
            if (uuid == null) {
                uuid = UUID.randomUUID().toString();
                SharedPreferences.Editor editor = settings.edit();
                editor.putString(NotificationUtils.WPCOM_PUSH_DEVICE_UUID, uuid);
                editor.commit();
            }

            NotificationUtils.registerDeviceForPushNotifications(context, regId);
        }
    }

    @Override
    protected void onUnregistered(Context context, String regId) {
        AppLog.v(T.NOTIFS, "GCM Unregistered ID: " + regId);
    }

}
