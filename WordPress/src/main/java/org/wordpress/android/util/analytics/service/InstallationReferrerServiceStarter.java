package org.wordpress.android.util.analytics.service;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import org.wordpress.android.util.AppLog;

import static org.wordpress.android.JobServiceId.JOB_INSTALL_REFERRER_SERVICE_ID;

public class InstallationReferrerServiceStarter {
    public static void startService(Context context) {
        if (context == null) {
            return;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Intent intent = new Intent(context, InstallationReferrerService.class);
            context.startService(intent);
        } else {
            // schedule the JobService here for API >= 26. The JobScheduler is available since API 21, but
            // since we are only using it elsewhere on API >= 26 to not break old behavior for pre-existing services,
            // let's stick to that version as well here.
            ComponentName componentName = new ComponentName(context, InstallationReferrerJobService.class);

            JobInfo jobInfo = new JobInfo.Builder(JOB_INSTALL_REFERRER_SERVICE_ID, componentName)
                    .setRequiresCharging(false)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setOverrideDeadline(0) // if possible, try to run right away
                    .build();

            JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            int resultCode = jobScheduler.schedule(jobInfo);
            if (resultCode == JobScheduler.RESULT_SUCCESS) {
                AppLog.i(AppLog.T.UTILS, "installation referrer job service > job scheduled");
            } else {
                AppLog.e(AppLog.T.UTILS, "installation referrer job service > job could not be scheduled");
            }
        }
    }
}
