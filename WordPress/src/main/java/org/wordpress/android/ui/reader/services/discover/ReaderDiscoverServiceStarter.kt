package org.wordpress.android.ui.reader.services.discover

import android.app.job.JobInfo
import android.app.job.JobInfo.Builder
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.PersistableBundle
import org.wordpress.android.JobServiceId
import org.wordpress.android.ui.reader.services.discover.ReaderDiscoverLogic.DiscoverTasks
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.READER

/*
* This class provides a way to decide which kind of Service to start, depending on the platform we're running on.
*
*/
object ReaderDiscoverServiceStarter {
    const val ARG_DISCOVER_TASK = "discover_task"

    fun startService(context: Context, task: DiscoverTasks) {
        if (VERSION.SDK_INT < VERSION_CODES.O) {
            val intent = Intent(context, ReaderDiscoverService::class.java)
            intent.putExtra(ARG_DISCOVER_TASK, task)
            context.startService(intent)
        } else {
            // schedule the JobService here for API >= 26. The JobScheduler is available since API 21, but
            // it's preferable to use it only since enforcement in API 26 to not break any old behavior
            val componentName = ComponentName(context, ReaderDiscoverJobService::class.java)
            val extras = PersistableBundle()
            extras.putInt(ARG_DISCOVER_TASK, task.ordinal)
            val jobInfo = Builder(JobServiceId.JOB_READER_DISCOVER_SERVICE_ID, componentName)
                    .setRequiresCharging(false)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setOverrideDeadline(0) // if possible, try to run right away
                    .setExtras(extras)
                    .build()
            val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            val resultCode = jobScheduler.schedule(jobInfo)
            if (resultCode == JobScheduler.RESULT_SUCCESS) {
                AppLog.i(READER, "reader discover service > job scheduled")
            } else {
                AppLog.e(READER, "reader discover service > job could not be scheduled")
            }
        }
    }
}
