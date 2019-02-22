package org.wordpress.android.push;

import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import org.wordpress.android.R;

import static org.wordpress.android.push.GCMMessageService.ACTIONS_PROGRESS_NOTIFICATION_ID;

public class NativeNotificationsUtils {
    public static void showIntermediateMessageToUser(String message, Context context) {
        showMessageToUser(message, true, ACTIONS_PROGRESS_NOTIFICATION_ID, context);
    }

    public static void showFinalMessageToUser(String message, int pushId, Context context) {
        showMessageToUser(message, false, pushId, context);
    }

    private static void showMessageToUser(String message, boolean intermediateMessage, int pushId, Context context) {
        NotificationCompat.Builder builder = getBuilder(context,
                context.getString(R.string.notification_channel_transient_id))
                .setContentText(message).setTicker(message)
                .setOnlyAlertOnce(true);
        showMessageToUserWithBuilder(builder, message, intermediateMessage, pushId, context);
    }

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
                .setSmallIcon(R.drawable.ic_my_sites_white_24dp)
                .setColor(context.getResources().getColor(R.color.blue_wordpress))
                .setContentTitle(context.getString(R.string.app_name))
                .setAutoCancel(true);
    }

    public static void dismissNotification(int pushId, Context context) {
        if (context != null) {
            final NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.cancel(pushId);
        }
    }

    public static void hideStatusBar(Context context) {
        Intent closeIntent = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        context.sendBroadcast(closeIntent);
    }
}
