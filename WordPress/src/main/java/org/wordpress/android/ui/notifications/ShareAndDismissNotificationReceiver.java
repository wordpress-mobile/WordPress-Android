package org.wordpress.android.ui.notifications;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.wordpress.android.util.SystemServiceFactory;

public class ShareAndDismissNotificationReceiver extends BroadcastReceiver {
    public static final String NOTIFICATION_ID_KEY = "NOTIFICATION_ID_KEY";

    public void onReceive(Context context, Intent receivedIntent) {
        // Cancel (dismiss) the notification
        int notificationId = receivedIntent.getIntExtra(NOTIFICATION_ID_KEY, 0);
        NotificationManager notificationManager = (NotificationManager) SystemServiceFactory.get(context,
                Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(notificationId);

        // Close system notification tray
        context.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));

        // Start the Share action
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, receivedIntent.getStringExtra(Intent.EXTRA_TEXT));
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, receivedIntent.getStringExtra(Intent.EXTRA_SUBJECT));
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(shareIntent);
    }
}
