package org.wordpress.android.push;

import android.annotation.SuppressLint;
import android.content.Context;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import org.wordpress.android.R;

import static org.wordpress.android.push.NotificationPushIds.ACTIONS_PROGRESS_NOTIFICATION_ID;

public class NativeNotificationsUtils {
    public static void showIntermediateMessageToUser(String message, Context context,
                                                     NotificationType notificationType) {
        showMessageToUser(message, true, ACTIONS_PROGRESS_NOTIFICATION_ID, context, notificationType);
    }

    public static void showFinalMessageToUser(String message, int pushId, Context context,
                                              NotificationType notificationType) {
        showMessageToUser(message, false, pushId, context, notificationType);
    }

    private static void showMessageToUser(String message, boolean intermediateMessage, int pushId,
                                          Context context, NotificationType notificationType) {
        NotificationCompat.Builder builder = getBuilder(context,
                context.getString(R.string.notification_channel_transient_id))
                .setContentText(message).setTicker(message)
                .setOnlyAlertOnce(true);
        if (notificationType != null) {
            builder = builder.setDeleteIntent(
                    NotificationsProcessingService.getPendingIntentForNotificationDismiss(
                            context,
                            pushId,
                            notificationType
                    )
            );
        }
        showMessageToUserWithBuilder(builder, message, intermediateMessage, pushId, context);
    }

    @SuppressLint("MissingPermission")
    public static void showMessageToUserWithBuilder(NotificationCompat.Builder builder, String message,
                                                    boolean intermediateMessage, int pushId, Context context) {
        if (!intermediateMessage) {
            builder.setStyle(new NotificationCompat.BigTextStyle().bigText(message));
        }
        builder.setProgress(0, 0, intermediateMessage);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(pushId, builder.build());
    }

    public static NotificationCompat.Builder getBuilder(Context context, String channelId) {
        return new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_app_white_24dp)
                .setColor(context.getResources().getColor(R.color.primary_50))
                .setContentTitle(context.getString(R.string.app_name))
                .setAutoCancel(true);
    }

    public static void dismissNotification(int pushId, Context context) {
        if (context != null) {
            final NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.cancel(pushId);
        }
    }
}
