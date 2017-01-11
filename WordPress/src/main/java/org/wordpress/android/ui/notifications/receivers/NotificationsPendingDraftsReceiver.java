package org.wordpress.android.ui.notifications.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.wordpress.android.ui.notifications.services.NotificationsPendingDraftsService;

public class NotificationsPendingDraftsReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        NotificationsPendingDraftsService.checkPrefsAndStartService(context);
    }
}
