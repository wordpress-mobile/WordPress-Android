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
import java.util.EnumSet

/*
* This class provides a way to decide which kind of Service to start, depending on the platform we're running on.
*
*/
object ReaderDiscoverServiceStarter {
    const val ARG_DISCOVER_TASKS = "discover_tasks"

    fun startService(context: Context, tasks: EnumSet<DiscoverTasks>) {
        if (VERSION.SDK_INT < VERSION_CODES.O) {
            if (tasks.size == 0) {
                return
            }
            val intent = Intent(context, ReaderDiscoverService::class.java)
            intent.putExtra(ARG_DISCOVER_TASKS, tasks)
            context.startService(intent)
        } else {
            // schedule the JobService here for API >= 26. The JobScheduler is available since API 21, but
            // it's preferable to use it only since enforcement in API 26 to not break any old behavior
            val componentName = ComponentName(context, ReaderDiscoverService::class.java)
            val extras = PersistableBundle()
            extras.putIntArray(
                    ARG_DISCOVER_TASKS,
                    getIntArrayFromEnumSet(tasks)
            )
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

    private fun getIntArrayFromEnumSet(enumSet: EnumSet<DiscoverTasks>): IntArray {
        val ordinals2 = IntArray(enumSet.size)
        var index = 0
        for (e in enumSet) {
            ordinals2[index++] = e.ordinal
        }
        return ordinals2
    }
}
