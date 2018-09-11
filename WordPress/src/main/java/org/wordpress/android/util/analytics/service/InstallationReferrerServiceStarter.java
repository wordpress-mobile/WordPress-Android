package org.wordpress.android.util.analytics.service;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PersistableBundle;

import org.wordpress.android.util.AppLog;

import static org.wordpress.android.JobServiceId.JOB_INSTALL_REFERRER_SERVICE_ID;

public class InstallationReferrerServiceStarter {
    public static final String ARG_REFERRER = "arg_referrer";
    public static void startService(Context context, String referrer) {
        if (context == null) {
            return;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Intent intent = new Intent(context, InstallationReferrerService.class);
            if (referrer != null) {
                intent.putExtra(ARG_REFERRER, referrer);
            }
            context.startService(intent);
        } else {
            // schedule the JobService here for API >= 26. The JobScheduler is available since API 21, but
            // since we are only using it elsewhere on API >= 26 to not break old behavior for pre-existing services,
            // let's stick to that version as well here.
            ComponentName componentName = new ComponentName(context, InstallationReferrerJobService.class);
            PersistableBundle extras = new PersistableBundle();
            if (referrer != null) {
                extras.putString(ARG_REFERRER, referrer);
            }

            JobInfo jobInfo = new JobInfo.Builder(JOB_INSTALL_REFERRER_SERVICE_ID, componentName)
                    .setRequiresCharging(false)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setOverrideDeadline(0) // if possible, try to run right away
                    .setExtras(extras)
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
