package org.wordpress.android.login;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.StringRes;
import android.support.v4.app.NotificationCompat;

import org.wordpress.android.util.AutoForeground;

class LoginNotification {
    private static Intent getResumeIntent(Context context) {
        // Let's get an Intent with the sole purpose of _resuming_ the app from the background
        Intent resumeIntent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());

        // getLaunchIntentForPackage() seems to set the Package Name but if we construct a launcher Intent manually
        //  the package name is not set so, let's null it out here to match the manual Intent.
        resumeIntent.setSelector(null);
        resumeIntent.setPackage(null);

        return resumeIntent;
    }

    private static NotificationCompat.Builder getNotificationBuilder(Context context, @StringRes int title,
            @StringRes int content) {
        NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();
        bigTextStyle.setBigContentTitle(context.getString(title));
        bigTextStyle.bigText(context.getString(content));

        return new NotificationCompat.Builder(context)
                .setStyle(bigTextStyle)
                .setContentTitle(context.getString(title))
                .setContentText(context.getString(content))
                .setSmallIcon(R.drawable.login_notification_icon)
                .setColor(context.getResources().getColor(R.color.login_notification_accent_color))
                .setAutoCancel(true);
    }

    static Notification progress(Context context, int progress, @StringRes int content) {
        return getNotificationBuilder(context, R.string.notification_login_title_in_progress, content)
                .setContentIntent(PendingIntent.getActivity(
                        context,
                        AutoForeground.NOTIFICATION_ID_PROGRESS,
                        getResumeIntent(context),
                        PendingIntent.FLAG_ONE_SHOT))
                .setProgress(100, progress, false)
                .build();
    }

    static Notification success(Context context, @StringRes int content) {
        return getNotificationBuilder(context, R.string.notification_login_title_success, content)
                .setContentIntent(PendingIntent.getActivity(
                        context,
                        AutoForeground.NOTIFICATION_ID_SUCCESS,
                        getResumeIntent(context),
                        PendingIntent.FLAG_ONE_SHOT))
                .build();
    }

    static Notification failure(Context context, @StringRes int content) {
        return getNotificationBuilder(context, R.string.notification_login_title_stopped, content)
                .setContentIntent(PendingIntent.getActivity(
                        context,
                        AutoForeground.NOTIFICATION_ID_FAILURE,
                        getResumeIntent(context),
                        PendingIntent.FLAG_ONE_SHOT))
                .build();
    }
}
