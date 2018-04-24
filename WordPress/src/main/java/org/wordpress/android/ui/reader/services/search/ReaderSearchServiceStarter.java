package org.wordpress.android.ui.reader.services.search;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;

import org.wordpress.android.util.AppLog;

import static org.wordpress.android.JobServiceId.JOB_READER_SEARCH_SERVICE_ID;

/**
 * service which searches for reader posts on wordpress.com
 */

public class ReaderSearchServiceStarter {
    public static final String ARG_QUERY = "query";
    public static final String ARG_OFFSET = "offset";

    public static void startService(Context context, @NonNull String query, int offset) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Intent intent = new Intent(context, ReaderSearchService.class);
            intent.putExtra(ARG_QUERY, query);
            intent.putExtra(ARG_OFFSET, offset);
            context.startService(intent);
        } else {
            // schedule the JobService here for API >= 26. The JobScheduler is available since API 21, but
            // it's preferable to use it only since enforcement in API 26 to not break any old behavior
            ComponentName componentName = new ComponentName(context, ReaderSearchJobService.class);

            PersistableBundle extras = new PersistableBundle();
            extras.putString(ARG_QUERY, query);
            extras.putInt(ARG_OFFSET, offset);

            JobInfo jobInfo = new JobInfo.Builder(JOB_READER_SEARCH_SERVICE_ID, componentName)
                    .setRequiresCharging(false)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setOverrideDeadline(0) // if possible, try to run right away
                    .setExtras(extras)
                    .build();

            JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            int resultCode = jobScheduler.schedule(jobInfo);
            if (resultCode == JobScheduler.RESULT_SUCCESS) {
                AppLog.i(AppLog.T.READER, "reader search job service > job scheduled");
            } else {
                AppLog.e(AppLog.T.READER, "reader search job service > job could not be scheduled");
            }
        }
    }
}
