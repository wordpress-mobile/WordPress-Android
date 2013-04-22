
package org.wordpress.android;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
import android.util.Log;

import com.google.android.gcm.GCMBaseIntentService;

import org.xmlrpc.android.WPComXMLRPCApi;
import org.xmlrpc.android.XMLRPCCallback;
import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;

import org.wordpress.android.ui.notifications.NotificationsActivity;
import org.wordpress.android.util.ImageHelper;

public class GCMIntentService extends GCMBaseIntentService {
    
    private static int noteCounter = 0;
    private static Map<String, Integer> activeNotificationsMap = new HashMap<String, Integer>();

    @Override
    protected void onError(Context context, String errorId) {
        // TODO Auto-generated method stub
        Log.v("WORDPRESS", "GCM Error: " + errorId);
    }

    @Override
    protected void onMessage(Context context, Intent intent) {
        Log.v("WORDPRESS", "Received Message");
        
        Bundle extras = intent.getExtras();
        
        if (extras == null) {
            Log.v("WORDPRESS", "Hrm. No notification message content received. Aborting.");
            return;
        }
        
        String title = extras.getString("title");
        if (title == null)
            title = "WordPress";
        String message = extras.getString("msg");
        
        String note_id = extras.getString("from");
        int notificationId = 0;
        
        if (note_id != null) {
            if (activeNotificationsMap.containsKey(note_id)) {
                notificationId = activeNotificationsMap.get(note_id);
            } else {
                notificationId = noteCounter;
                activeNotificationsMap.put(note_id, notificationId);
                noteCounter++;
            }
        }
        
        String iconURL = extras.getString("icon");
        Bitmap largeIconBitmap = null;
        if (iconURL != null) {
            float screenDensity = getResources().getDisplayMetrics().densityDpi;
            int size = Math.round(64 * (screenDensity / 160));
            String resizedURL = iconURL.replaceAll("(?<=[?&;])s=[0-9]*", "s=" + size);
            largeIconBitmap = ImageHelper.downloadBitmap(resizedURL);
        }
        
        // It'd be cool to set the small icon, but it looks like it doesn't accept a bitmap. 
        // We could add noticons to the app resources though (like in WPiOS)
        /*String noticonURL = extras.getString("noticon");
        Bitmap smallIconBitmap = null;
        if (noticonURL != null) {
            int size = 16 * (screenDensity / 160);
            smallIconBitmap = ImageHelper.downloadBitmap(String.format("http://i0.wp.com/%s?w=%d", noticonURL, size));
        }*/
        
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean sound, vibrate, light;

        sound = prefs.getBoolean("wp_pref_notification_sound", false);
        vibrate = prefs.getBoolean("wp_pref_notification_vibrate", false);
        light = prefs.getBoolean("wp_pref_notification_light", false);
        
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.notification_icon)
                .setContentTitle(title)
                .setContentText(message)
                .setTicker(message)
                .setAutoCancel(true)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message));
        
        // Add some actions if this is a comment notification
        String noteType = extras.getString("type");
        if (noteType != null && noteType.equals("c")) {
            // TODO pass whatever's needed to go directly to comment reply
            // TODO use intent service for handling actions http://udinic.wordpress.com/2012/07/24/adding-more-actions-to-jellybean-notifications/
            Intent commentReplyIntent = new Intent(this, NotificationsActivity.class);
            PendingIntent commentReplyPendingIntent = PendingIntent.getActivity(context, 0, commentReplyIntent,
                    Intent.FLAG_ACTIVITY_NEW_TASK);
            mBuilder.addAction(R.drawable.ab_icon_reply, getResources().getText(R.string.reply), commentReplyPendingIntent);
        }
        
        if (largeIconBitmap != null) {
            mBuilder.setLargeIcon(largeIconBitmap);
        }
        
        if (sound)
            mBuilder.setSound(Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.notification));
        if (vibrate)
            mBuilder.setVibrate(new long[]{ 500, 500, 500 });
        if (light)
            mBuilder.setLights(0xff0000ff, 1000, 5000);
        
        Intent resultIntent = new Intent(this, NotificationsActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, resultIntent,
                Intent.FLAG_ACTIVITY_NEW_TASK);
        mBuilder.setContentIntent(pendingIntent);
        NotificationManager mNotificationManager =
            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // mId allows you to update the notification later on.
        // TODO: .getNotification() is deprecated but .build() is not available on 2.3.3
        mNotificationManager.notify(notificationId, mBuilder.build());

    }

    @Override
    protected void onRegistered(Context context, String regId) {
        
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context); 
        if (regId != null && regId.length() > 0) {
            // Get or create UUID for WP.com notes api
            String uuid = settings.getString("wp_pref_notifications_uuid", null);
            if (uuid == null) {
                uuid = UUID.randomUUID().toString();
                SharedPreferences.Editor editor = settings.edit();
                editor.putString("wp_pref_notifications_uuid", uuid);
                editor.commit();
            }

            Object[] params = {
                    settings.getString("wp_pref_wpcom_username", ""),
                    WordPressDB.decryptPassword(settings.getString("wp_pref_wpcom_password", "")),
                    regId,
                    uuid,
                    "android",
                    false
            };

            XMLRPCClient client = new XMLRPCClient(URI.create(Constants.wpcomXMLRPCURL), "", "");
            client.callAsync(new XMLRPCCallback() {
                public void onSuccess(long id, Object result) {
                    Log.v("WORDPRESS", "Succesfully registered device on WP.com");
                }

                public void onFailure(long id, XMLRPCException error) {
                    Log.v("WORDPRESS", error.getMessage());
                }
            }, "wpcom.mobile_push_register_token", params);
            
            new WPComXMLRPCApi().getNotificationSettings(null, context);
        }
    }

    @Override
    protected void onUnregistered(Context context, String regId) {
        // TODO Remove id from WP.com?
        Log.v("WORDPRESS", "GCM Unregistered ID: " + regId);

    }

}
