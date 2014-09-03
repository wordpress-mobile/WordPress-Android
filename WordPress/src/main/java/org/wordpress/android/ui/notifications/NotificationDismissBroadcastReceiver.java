package org.wordpress.android.ui.notifications;

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
        GCMIntentService.clearNotificationsMap();
    }
}