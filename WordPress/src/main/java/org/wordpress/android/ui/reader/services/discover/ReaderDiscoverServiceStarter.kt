package org.wordpress.android.ui.reader.services.discover

import android.app.job.JobInfo
import android.app.job.JobInfo.Builder
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
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

    fun startService(context: Context, task: DiscoverTasks): Boolean {
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
            return false
        }
        return true
    }
}
