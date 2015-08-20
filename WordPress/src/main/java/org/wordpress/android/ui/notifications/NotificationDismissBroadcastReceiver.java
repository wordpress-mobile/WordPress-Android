package org.wordpress.android.ui.notifications;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.wordpress.android.GCMIntentService;

/*
 * Clears the notification map when a user dismisses a notification
 */
public class NotificationDismissBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        int notificationId = intent.getIntExtra("notificationId", 0);
        if (notificationId == GCMIntentService.GROUP_NOTIFICATION_ID) {
            GCMIntentService.clearNotificationsMap();
        } else {
            GCMIntentService.getNotificationsMap().remove(notificationId);

            // Dismiss the grouped notification if a user dismisses all notifications from a wear device
            if (GCMIntentService.getNotificationsMap().isEmpty()) {
                NotificationManager notificationManager = (NotificationManager) context
                        .getSystemService(GCMIntentService.NOTIFICATION_SERVICE);
                notificationManager.cancel(GCMIntentService.GROUP_NOTIFICATION_ID);
            }
        }
    }
}