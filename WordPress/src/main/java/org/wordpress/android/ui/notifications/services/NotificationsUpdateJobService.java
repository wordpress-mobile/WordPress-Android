package org.wordpress.android.ui.notifications.services;

import android.annotation.TargetApi;
import android.app.job.JobParameters;
import android.app.job.JobService;

import org.wordpress.android.ui.notifications.NotificationsListFragment;
import org.wordpress.android.util.AppLog;

@TargetApi(21)
public class NotificationsUpdateJobService extends JobService
        implements NotificationsUpdateLogic.ServiceCompletionListener {
    public static final String IS_TAPPED_ON_NOTIFICATION = "is-tapped-on-notification";

    private NotificationsUpdateLogic mNotificationsUpdateLogic;

    @TargetApi(22)
    @Override
    public boolean onStartJob(JobParameters params) {
        String noteId = null;
        boolean isStartedByTappingOnNotification = false;
        if (params.getExtras() != null && params.getExtras().containsKey(NotificationsListFragment.NOTE_ID_EXTRA)) {
            noteId = params.getExtras().getString(NotificationsListFragment.NOTE_ID_EXTRA);
            isStartedByTappingOnNotification = params.getExtras().getBoolean(
                    IS_TAPPED_ON_NOTIFICATION, false);
        }
        mNotificationsUpdateLogic.performRefresh(noteId, isStartedByTappingOnNotification, params);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        jobFinished(params, false);
        return false;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        AppLog.i(AppLog.T.NOTIFS, "notifications update job service > created");
        mNotificationsUpdateLogic = new NotificationsUpdateLogic(this);
    }

    @Override
    public void onDestroy() {
        AppLog.i(AppLog.T.NOTIFS, "notifications update job service > destroyed");
        super.onDestroy();
    }

    @Override
    public void onCompleted(Object companion) {
        AppLog.i(AppLog.T.NOTIFS, "notifications update job service > all tasks completed");
        jobFinished((JobParameters) companion, false);
    }
}
