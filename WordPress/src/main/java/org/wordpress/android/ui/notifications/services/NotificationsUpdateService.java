package org.wordpress.android.ui.notifications.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import org.wordpress.android.ui.notifications.NotificationsListFragment;
import org.wordpress.android.util.AppLog;

public class NotificationsUpdateService extends Service implements NotificationsUpdateLogic.ServiceCompletionListener {
    public static final String IS_TAPPED_ON_NOTIFICATION = "is-tapped-on-notification";

    private NotificationsUpdateLogic mNotificationsUpdateLogic;

    public static void startService(Context context) {
        if (context == null) {
            return;
        }
        Intent intent = new Intent(context, NotificationsUpdateService.class);
        context.startService(intent);
    }

    public static void startService(Context context, String noteId) {
        if (context == null) {
            return;
        }
        Intent intent = new Intent(context, NotificationsUpdateService.class);
        intent.putExtra(NotificationsListFragment.NOTE_ID_EXTRA, noteId);
        intent.putExtra(IS_TAPPED_ON_NOTIFICATION, true);
        context.startService(intent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        AppLog.i(AppLog.T.NOTIFS, "notifications update service > created");
        mNotificationsUpdateLogic = new NotificationsUpdateLogic(this);
    }

    @Override
    public void onDestroy() {
        AppLog.i(AppLog.T.NOTIFS, "notifications update service > destroyed");
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String noteId = intent.getStringExtra(NotificationsListFragment.NOTE_ID_EXTRA);
            boolean isStartedByTappingOnNotification = intent.getBooleanExtra(
                    IS_TAPPED_ON_NOTIFICATION, false);
            mNotificationsUpdateLogic.performRefresh(noteId, isStartedByTappingOnNotification, null);
        }
        return START_NOT_STICKY;
    }

    @Override public void onCompleted(Object companion) {
        AppLog.i(AppLog.T.READER, "notifications update service > all tasks completed");
        stopSelf();
    }
}
