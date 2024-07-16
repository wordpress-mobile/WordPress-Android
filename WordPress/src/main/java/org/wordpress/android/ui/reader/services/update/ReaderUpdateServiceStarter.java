package org.wordpress.android.ui.reader.services.update;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.os.PersistableBundle;

import org.wordpress.android.util.AppLog;

import java.util.EnumSet;

import static org.wordpress.android.JobServiceId.JOB_READER_UPDATE_SERVICE_ID;

/*
 * this class provides a way to decide which kind of Service to start, depending on the platform we're running on
 */
public class ReaderUpdateServiceStarter {
    public static final String ARG_UPDATE_TASKS = "update_tasks";

    public static void startService(Context context, EnumSet<ReaderUpdateLogic.UpdateTask> tasks) {
        ComponentName componentName = new ComponentName(context, ReaderUpdateJobService.class);

        PersistableBundle extras = new PersistableBundle();
        extras.putIntArray(ARG_UPDATE_TASKS, getIntArrayFromEnumSet(tasks));

        JobInfo jobInfo = new JobInfo.Builder(JOB_READER_UPDATE_SERVICE_ID, componentName)
                .setRequiresCharging(false)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setOverrideDeadline(0) // if possible, try to run right away
                .setExtras(extras)
                .build();

        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        int resultCode = jobScheduler.schedule(jobInfo);
        if (resultCode == JobScheduler.RESULT_SUCCESS) {
            AppLog.i(AppLog.T.READER, "reader service > job scheduled");
        } else {
            AppLog.e(AppLog.T.READER, "reader service > job could not be scheduled");
        }
    }

    private static int[] getIntArrayFromEnumSet(EnumSet<ReaderUpdateLogic.UpdateTask> enumSet) {
        int[] ordinals2 = new int[enumSet.size()];
        int index = 0;
        for (ReaderUpdateLogic.UpdateTask e : enumSet) {
            ordinals2[index++] = e.ordinal();
        }
        return ordinals2;
    }
}
