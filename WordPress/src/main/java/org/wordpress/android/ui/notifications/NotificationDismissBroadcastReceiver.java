package org.wordpress.android.ui.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationManagerCompat;

import org.wordpress.android.GCMMessageService;

/*
 * Clears the notification map when a user dismisses a notification
 */
public class NotificationDismissBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        int notificationId = intent.getIntExtra("notificationId", 0);
        if (notificationId == GCMMessageService.GROUP_NOTIFICATION_ID) {
            GCMMessageService.clearNotifications();
        } else {
            GCMMessageService.removeNotification(notificationId);
            // Dismiss the grouped notification if a user dismisses all notifications from a wear device
            if (!GCMMessageService.hasNotifications()) {
                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                notificationManager.cancel(GCMMessageService.GROUP_NOTIFICATION_ID);
            }
        }
    }
}
