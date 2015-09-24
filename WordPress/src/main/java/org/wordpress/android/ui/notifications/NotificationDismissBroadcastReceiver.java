package org.wordpress.android.ui.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationManagerCompat;

import org.wordpress.android.GCMIntentService;

/*
 * Clears the notification map when a user dismisses a notification
 */
public class NotificationDismissBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        int notificationId = intent.getIntExtra("notificationId", 0);
        if (notificationId == GCMIntentService.GROUP_NOTIFICATION_ID) {
            GCMIntentService.clearNotifications();
        } else {
            GCMIntentService.removeNotification(notificationId);

            // Dismiss the grouped notification if a user dismisses all notifications from a wear device
            if (!GCMIntentService.hasNotifications()) {
                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                notificationManager.cancel(GCMIntentService.GROUP_NOTIFICATION_ID);
            }
        }
    }
}