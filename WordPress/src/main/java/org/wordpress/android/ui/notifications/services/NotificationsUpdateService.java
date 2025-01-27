package org.wordpress.android.ui.notifications.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import org.wordpress.android.ui.notifications.NotificationsListFragment;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.PerAppLocaleManager;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

import static org.wordpress.android.ui.notifications.services.NotificationsUpdateServiceStarter.IS_TAPPED_ON_NOTIFICATION;

@AndroidEntryPoint
public class NotificationsUpdateService extends Service implements NotificationsUpdateLogic.ServiceCompletionListener {
    private NotificationsUpdateLogic mNotificationsUpdateLogic;

    @Inject PerAppLocaleManager mPerAppLocaleManager;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        AppLog.i(AppLog.T.NOTIFS, "notifications update service > created");
        mNotificationsUpdateLogic = new NotificationsUpdateLogic(
                mPerAppLocaleManager.getCurrentLocaleLanguageCode(),
                this
        );
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
