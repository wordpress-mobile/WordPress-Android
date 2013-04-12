
package org.wordpress.android;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gcm.GCMBaseIntentService;

import org.wordpress.android.ui.comments.CommentsActivity;

public class GCMIntentService extends GCMBaseIntentService {

    @Override
    protected void onError(Context context, String errorId) {
        // TODO Auto-generated method stub
        Log.v("WORDPRESS", "GCM Error: " + errorId);
    }

    @Override
    protected void onMessage(Context context, Intent intent) {
        // TODO Auto-generated method stub
        Log.v("WORDPRESS", "Received Message");
        
        Bundle extras = intent.getExtras();
        
        if (extras == null)
            return;
        
        String message = extras.getString("message");

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean sound, vibrate, light;

        sound = prefs.getBoolean("wp_pref_notification_sound", false);
        vibrate = prefs.getBoolean("wp_pref_notification_vibrate", false);
        light = prefs.getBoolean("wp_pref_notification_light", false);
        
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.notification_icon)
                .setContentTitle("New Notification")
                .setContentText(message)
                .setAutoCancel(true);
        
        
        if (sound)
            mBuilder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
        if (vibrate)
            mBuilder.setVibrate(new long[]{ 500, 500, 500 });
        if (light)
            mBuilder.setLights(0xff0000ff, 1000, 5000);
        
        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(this, CommentsActivity.class);


        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, resultIntent,
                Intent.FLAG_ACTIVITY_CLEAR_TOP);
        mBuilder.setContentIntent(pendingIntent);
        NotificationManager mNotificationManager =
            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // mId allows you to update the notification later on.
        // TODO: .getNotification() is deprecated but .build() is not available on 2.3.3
        mNotificationManager.notify(0, mBuilder.getNotification());

    }

    @Override
    protected void onRegistered(Context context, String regId) {
        // Send id to WP.com
        Log.v("WORDPRESS", "GCM Registered ID: " + regId);

    }

    @Override
    protected void onUnregistered(Context context, String regId) {
        // Remove id from WP.com
        Log.v("WORDPRESS", "GCM Unregistered ID: " + regId);

    }

}
