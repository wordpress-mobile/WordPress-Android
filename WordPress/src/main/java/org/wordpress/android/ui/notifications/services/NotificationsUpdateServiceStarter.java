package org.wordpress.android.ui.notifications.services;

import android.content.Context;
import android.content.Intent;

import org.wordpress.android.ui.notifications.NotificationsListFragment;

public class NotificationsUpdateServiceStarter {
    public static final String IS_TAPPED_ON_NOTIFICATION = "is-tapped-on-notification";

    public static void startService(Context context) {
        if (context == null) {
            return;
        }
        startService(context, null);
    }

    public static void startService(Context context, String noteId) {
        if (context == null) {
            return;
        }
        Intent intent = new Intent(context, NotificationsUpdateService.class);
        if (noteId != null) {
            intent.putExtra(NotificationsListFragment.NOTE_ID_EXTRA, noteId);
            intent.putExtra(IS_TAPPED_ON_NOTIFICATION, true);
        }
        context.startService(intent);
    }
}
