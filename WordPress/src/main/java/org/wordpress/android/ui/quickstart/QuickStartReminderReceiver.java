package org.wordpress.android.ui.quickstart;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import org.wordpress.android.R;
import org.wordpress.android.ui.main.MySiteFragment;
import org.wordpress.android.ui.main.WPMainActivity;
import org.wordpress.android.ui.reader.services.ServiceCompletionListener;
import org.wordpress.android.util.AppLog;

import static android.app.Service.START_NOT_STICKY;
import static android.content.Context.NOTIFICATION_SERVICE;

public class QuickStartReminderReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        QuickStartDetails quickStartDetails = (QuickStartDetails) intent.getBundleExtra("b")
                                                                        .getSerializable(QuickStartDetails.KEY);

        Intent resultIntent = new Intent(context, WPMainActivity.class);

        resultIntent.putExtra(MySiteFragment.ARG_QUICK_START_TASK, quickStartDetails.getTask());
        resultIntent.putExtra("a", "b");
        resultIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK
                              | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        resultIntent.setAction("android.intent.action.MAIN");
        resultIntent.addCategory("android.intent.category.LAUNCHER");
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addNextIntentWithParentStack(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);


        NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        Notification notification = new NotificationCompat.Builder(context,
                context.getString(R.string.notification_channel_normal_id))
                .setSmallIcon(R.drawable.ic_my_sites_24dp)
                .setContentTitle(context.getString(quickStartDetails.getTitleId()))
                .setContentText(context.getString(quickStartDetails.getSubtitleId()))
                .setOnlyAlertOnce(true)
                .setAutoCancel(true)
                .setContentIntent(resultPendingIntent)
                .build();


        notificationManager.notify(111, notification);
    }
}
