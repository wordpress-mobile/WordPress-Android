package org.wordpress.android.ui.accounts.login;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;

import org.wordpress.android.R;
import org.wordpress.android.ui.main.WPMainActivity;
import org.wordpress.android.util.AutoForeground;

class LoginNotification {
    private static Intent getPendingIntent(Context context) {
        return new Intent(context, WPMainActivity.class);
    }

    private static NotificationCompat.Builder getNotificationBuilder(Context context, String content) {
        return new NotificationCompat.Builder(context)
                .setContentTitle(content)
                .setSmallIcon(R.drawable.ic_my_sites_24dp)
                .setColor(context.getResources().getColor(R.color.blue_wordpress))
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.app_icon))
                .setAutoCancel(true);
    }

    static Notification progress(Context context, int progress, String content) {
        return getNotificationBuilder(context, content)
                .setContentIntent(PendingIntent.getActivity(
                        context,
                        AutoForeground.NOTIFICATION_ID_PROGRESS,
                        getPendingIntent(context),
                        PendingIntent.FLAG_ONE_SHOT))
                .setProgress(100, progress, false)
                .build();
    }

    static Notification success(Context context, String content) {
        return getNotificationBuilder(context, content)
                .setContentIntent(PendingIntent.getActivity(
                        context,
                        AutoForeground.NOTIFICATION_ID_SUCCESS,
                        getPendingIntent(context),
                        PendingIntent.FLAG_ONE_SHOT))
                .build();
    }

    static Notification failure(Context context, String content) {
        return getNotificationBuilder(context, content)
                .setContentIntent(PendingIntent.getActivity(
                        context,
                        AutoForeground.NOTIFICATION_ID_FAILURE,
                        getPendingIntent(context),
                        PendingIntent.FLAG_ONE_SHOT))
                .build();
    }
}
