package org.wordpress.android.ui.notifications.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import org.wordpress.android.ui.notifications.NotificationsListFragment;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.LocaleManager;

import static org.wordpress.android.ui.notifications.services.NotificationsUpdateServiceStarter
        .IS_TAPPED_ON_NOTIFICATION;

public class NotificationsUpdateService extends Service implements NotificationsUpdateLogic.ServiceCompletionListener {
    private NotificationsUpdateLogic mNotificationsUpdateLogic;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleManager.setLocale(newBase));
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        AppLog.i(AppLog.T.NOTIFS, "notifications update service > created");
        mNotificationsUpdateLogic = new NotificationsUpdateLogic(LocaleManager.getLanguage(this), this);
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
        AppLog.i(AppLog.T.NOTIFS, "notifications update service > all tasks completed");
        stopSelf();
    }
}
