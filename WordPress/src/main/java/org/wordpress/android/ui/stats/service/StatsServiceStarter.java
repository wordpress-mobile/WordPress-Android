package org.wordpress.android.ui.stats.service;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;

import org.wordpress.android.JobServiceId;
import org.wordpress.android.util.AppLog;

import static org.wordpress.android.JobServiceId.JOB_STATS_SERVICE_ID;

public class StatsServiceStarter {
    public static final String ARG_START_ID = "start-id";
    private static int jobId = 0;

    public static void startService(Context context, Bundle originalExtras) {
        if (context == null || originalExtras == null) {
            return;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Intent intent = new Intent(context, StatsService.class);
            intent.putExtras(originalExtras);
            context.startService(intent);
        } else {
            // schedule the JobService here for API >= 26. The JobScheduler is available since API 21, but
            // it's preferable to use it only since enforcement in API 26 to not break any old behavior
            ComponentName componentName = new ComponentName(context, StatsJobService.class);

            PersistableBundle extras = passBundleExtrasToPersistableBundle(originalExtras);

            // don't schedule the same kind of request twice for Stats - just wait for the pending Job to be
            // executed.
            if (!JobServiceId.isJobServiceWithSameParamsPending(context, componentName, extras, ARG_START_ID)) {
                // if not found, let's add a new Job Id and schedule this onw
                extras.putInt(ARG_START_ID, getNewStartId());

                JobInfo jobInfo = new JobInfo.Builder(JOB_STATS_SERVICE_ID + jobId, componentName)
                        .setRequiresCharging(false)
                        .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                        .setOverrideDeadline(0) // if possible, try to run right away
                        .setExtras(extras)
                        .build();

                JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
                int resultCode = jobScheduler.schedule(jobInfo);
                if (resultCode == JobScheduler.RESULT_SUCCESS) {
                    AppLog.i(AppLog.T.STATS, "stats job service > job scheduled");
                } else {
                    AppLog.e(AppLog.T.STATS, "stats job service > job could not be scheduled");
                }
            }
        }
    }

    private static PersistableBundle passBundleExtrasToPersistableBundle(Bundle originalExtras) {
        PersistableBundle bundle = new PersistableBundle();
        if (originalExtras != null) {
            if (originalExtras.containsKey(StatsService.ARG_BLOG_ID)) {
                bundle.putLong(StatsService.ARG_BLOG_ID, originalExtras.getLong(StatsService.ARG_BLOG_ID));
            }

            if (originalExtras.containsKey(StatsService.ARG_PERIOD)) {
                bundle.putInt(StatsService.ARG_PERIOD, originalExtras.getInt(StatsService.ARG_PERIOD));
            }

            if (originalExtras.containsKey(StatsService.ARG_DATE)) {
                bundle.putString(StatsService.ARG_DATE, originalExtras.getString(StatsService.ARG_DATE));
            }

            if (originalExtras.containsKey(StatsService.ARG_SECTION)) {
                bundle.putIntArray(StatsService.ARG_SECTION, originalExtras.getIntArray(StatsService.ARG_SECTION));
            }

            if (originalExtras.containsKey(StatsService.ARG_MAX_RESULTS)) {
                bundle.putInt(StatsService.ARG_MAX_RESULTS, originalExtras.getInt(StatsService.ARG_MAX_RESULTS));
            }

            if (originalExtras.containsKey(StatsService.ARG_PAGE_REQUESTED)) {
                bundle.putInt(StatsService.ARG_PAGE_REQUESTED, originalExtras.getInt(StatsService.ARG_PAGE_REQUESTED));
            }
        }

        return bundle;
    }

    private static int getNewStartId() {
        if (jobId == (Integer.MAX_VALUE - 1)) {
            // just restart count to avoid overflow
            jobId = 0;
        }
        return ++jobId;
    }
}
