package org.wordpress.android.ui.notifications.services;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PersistableBundle;

import org.wordpress.android.ui.notifications.NotificationsListFragment;
import org.wordpress.android.util.AppLog;

import static org.wordpress.android.JobServiceId.JOB_NOTIFICATIONS_UPDATE_SERVICE_ID;

public class NotificationsUpdateServiceStarter {
    public static final String IS_TAPPED_ON_NOTIFICATION = "is-tapped-on-notification";

    public static void startService(Context context) {
        if (context == null) {
            return;
        }
        startService(context, null);
    }

    public static void startService(Context context, String noteId) {
        if (context == null) {
            return;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Intent intent = new Intent(context, NotificationsUpdateService.class);
            if (noteId != null) {
                intent.putExtra(NotificationsListFragment.NOTE_ID_EXTRA, noteId);
                intent.putExtra(IS_TAPPED_ON_NOTIFICATION, true);
            }
            context.startService(intent);
        } else {
            // schedule the JobService here for API >= 26. The JobScheduler is available since API 21, but
            // it's preferable to use it only since enforcement in API 26 to not break any old behavior
            ComponentName componentName = new ComponentName(context, NotificationsUpdateJobService.class);

            PersistableBundle extras = new PersistableBundle();
            if (noteId != null) {
                extras.putString(NotificationsListFragment.NOTE_ID_EXTRA, noteId);
                extras.putBoolean(IS_TAPPED_ON_NOTIFICATION, true);
            }

            JobInfo jobInfo = new JobInfo.Builder(JOB_NOTIFICATIONS_UPDATE_SERVICE_ID, componentName)
                    .setRequiresCharging(false)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setOverrideDeadline(0) // if possible, try to run right away
                    .setExtras(extras)
                    .build();

            JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            int resultCode = jobScheduler.schedule(jobInfo);
            if (resultCode == JobScheduler.RESULT_SUCCESS) {
                AppLog.i(AppLog.T.READER, "notifications update job service > job scheduled");
            } else {
                AppLog.e(AppLog.T.READER, "notifications update job service > job could not be scheduled");
            }
        }
    }
}
