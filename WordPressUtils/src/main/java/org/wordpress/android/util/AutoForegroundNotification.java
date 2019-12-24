package org.wordpress.android.util;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;
import android.support.v4.app.NotificationCompat;

import static org.wordpress.android.util.AutoForeground.NOTIFICATION_ID_FAILURE;
import static org.wordpress.android.util.AutoForeground.NOTIFICATION_ID_PROGRESS;
import static org.wordpress.android.util.AutoForeground.NOTIFICATION_ID_SUCCESS;

public class AutoForegroundNotification {
    private static Intent getResumeIntent(Context context) {
        // Let's get an Intent with the sole purpose of _resuming_ the app from the background
        Intent resumeIntent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());

        // getLaunchIntentForPackage() seems to set the Package Name but if we construct a launcher Intent manually
        // the package name is not set so, let's null it out here to match the manual Intent.
        resumeIntent.setSelector(null);
        resumeIntent.setPackage(null);

        return resumeIntent;
    }

    private static NotificationCompat.Builder getNotificationBuilder(Context context, String channelId, int requestCode,
                                                                     @StringRes int title, @StringRes int content,
                                                                     @DrawableRes int icon, @ColorRes int accentColor) {
        NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();
        bigTextStyle.setBigContentTitle(context.getString(title));
        bigTextStyle.bigText(context.getString(content));

        return new NotificationCompat.Builder(context, channelId)
                .setStyle(bigTextStyle)
                .setContentTitle(context.getString(title))
                .setContentText(context.getString(content))
                .setSmallIcon(icon)
                .setColor(context.getResources().getColor(accentColor))
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(PendingIntent.getActivity(
                        context,
                        requestCode,
                        getResumeIntent(context),
                        PendingIntent.FLAG_ONE_SHOT));
    }

    public static Notification progress(Context context, String channelId, int progress, @StringRes int title,
                                        @StringRes int content,
                                        @DrawableRes int icon, @ColorRes int accentColor) {
        return getNotificationBuilder(context, channelId, NOTIFICATION_ID_PROGRESS, title, content, icon, accentColor)
                .setProgress(100, progress, false)
                .build();
    }

    public static Notification progressIndeterminate(Context context, String channelId, @StringRes int title,
                                                     @StringRes int content, @DrawableRes int icon,
                                                     @ColorRes int accentColor) {
        return getNotificationBuilder(context, channelId, NOTIFICATION_ID_PROGRESS, title, content, icon, accentColor)
                .setProgress(0, 0, true)
                .build();
    }

    public static Notification success(Context context, String channelId, @StringRes int title, @StringRes int content,
                                       @DrawableRes int icon, @ColorRes int accentColor) {
        return getNotificationBuilder(context, channelId, NOTIFICATION_ID_SUCCESS, title, content, icon, accentColor)
                .build();
    }

    public static Notification failure(Context context, String channelId, @StringRes int title, @StringRes int content,
                                       @DrawableRes int icon, @ColorRes int accentColor) {
        return getNotificationBuilder(context, channelId, NOTIFICATION_ID_FAILURE, title, content, icon, accentColor)
                .build();
    }
}
