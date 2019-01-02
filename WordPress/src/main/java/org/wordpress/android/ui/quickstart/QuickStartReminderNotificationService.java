package org.wordpress.android.ui.quickstart;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import org.wordpress.android.R;
import org.wordpress.android.ui.reader.services.ServiceCompletionListener;
import org.wordpress.android.util.AppLog;

public class QuickStartReminderNotificationService extends Service implements ServiceCompletionListener {
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        AppLog.i(AppLog.T.READER, "reader post service > destroyed");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return START_NOT_STICKY;
        }
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification notification = new NotificationCompat.Builder(this,
                this.getString(R.string.notification_channel_normal_id)).setSmallIcon(android.R.drawable.stat_sys_upload).setContentText("test content").setOnlyAlertOnce(true)
                                                                        .setSubText("test subcontent").build();


        notificationManager.notify(111, notification);
//        Intent myIntent = new Intent(this, MyActivity.class);
//        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, 0);
//        notification.setLatestEventInfo(this, "Notify label", "Notify text", contentIntent);
//        mNM.notify(NOTIFICATION, notification);

        return START_NOT_STICKY;
    }

    @Override
    public void onCompleted(Object companion) {
        stopSelf();
    }
}
