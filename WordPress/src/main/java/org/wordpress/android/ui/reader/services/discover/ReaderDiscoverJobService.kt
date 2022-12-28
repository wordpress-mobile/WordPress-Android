package org.wordpress.android.ui.reader.services.discover

import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Context
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import org.wordpress.android.ui.reader.services.ServiceCompletionListener
import org.wordpress.android.ui.reader.services.discover.ReaderDiscoverLogic.DiscoverTasks
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.READER
import org.wordpress.android.util.LocaleManager
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.CoroutineContext

@AndroidEntryPoint
class ReaderDiscoverJobService : JobService(), ServiceCompletionListener, CoroutineScope {
    @Inject
    @field:Named("IO_THREAD")
    lateinit var ioDispatcher: CoroutineDispatcher
    @Inject
    lateinit var readerDiscoverLogic: ReaderDiscoverLogic

    private var job: Job = Job()

    override val coroutineContext: CoroutineContext
        get() = ioDispatcher + job

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleManager.setLocale(newBase))
    }

    override fun onStartJob(params: JobParameters): Boolean {
        AppLog.i(READER, "reader discover job service > started")

        val task = DiscoverTasks.values()[(params.extras[ReaderDiscoverServiceStarter.ARG_DISCOVER_TASK] as Int)]

        readerDiscoverLogic.performTasks(task, params, this, this)
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        AppLog.i(READER, "reader discover job service > stopped")
        jobFinished(params, false)
        return false
    }

    override fun onCreate() {
        super.onCreate()
        AppLog.i(READER, "reader discover job service > created")
    }

    override fun onDestroy() {
        AppLog.i(READER, "reader discover job service > destroyed")
        job.cancel()
        super.onDestroy()
    }

    override fun onCompleted(companion: Any) {
        AppLog.i(READER, "reader discover job service > all tasks completed")
        jobFinished(companion as JobParameters, false)
    }
}
