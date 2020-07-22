package org.wordpress.android.ui.reader.services.discover

import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Context
import org.wordpress.android.ui.reader.services.ServiceCompletionListener
import org.wordpress.android.ui.reader.services.discover.ReaderDiscoverLogic.DiscoverTasks
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.READER
import org.wordpress.android.util.LocaleManager
import java.util.EnumSet

class ReaderDiscoverJobService : JobService(), ServiceCompletionListener {
    private lateinit var readerDiscoverLogic: ReaderDiscoverLogic

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleManager.setLocale(newBase))
    }

    override fun onStartJob(params: JobParameters?): Boolean {
        AppLog.i(READER, "reader discover job service > started")
        if (params?.extras?.containsKey(ReaderDiscoverServiceStarter.ARG_DISCOVER_TASKS) == true) {
            val tmp = params.extras[ReaderDiscoverServiceStarter.ARG_DISCOVER_TASKS] as IntArray
            val tasks = EnumSet.noneOf(DiscoverTasks::class.java)
            for (i in tmp) {
                tasks.add(DiscoverTasks.values()[i])
            }
            readerDiscoverLogic.performTasks(tasks, params)
        }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        AppLog.i(READER, "reader discover job service > stopped")
        jobFinished(params, false)
        return false
    }

    override fun onCreate() {
        super.onCreate()
        readerDiscoverLogic = ReaderDiscoverLogic(this)
        AppLog.i(READER, "reader discover job service > created")
    }

    override fun onDestroy() {
        AppLog.i(READER, "reader discover job service > destroyed")
        super.onDestroy()
    }

    override fun onCompleted(companion: Any) {
        AppLog.i(READER, "reader discover job service > all tasks completed")
        jobFinished(companion as JobParameters, false)
    }
}
