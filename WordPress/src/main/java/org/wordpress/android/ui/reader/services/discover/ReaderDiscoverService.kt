package org.wordpress.android.ui.reader.services.discover

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import org.wordpress.android.WordPress
import org.wordpress.android.ui.reader.services.ServiceCompletionListener
import org.wordpress.android.ui.reader.services.discover.ReaderDiscoverLogic.DiscoverTasks
import org.wordpress.android.ui.reader.services.discover.ReaderDiscoverServiceStarter.ARG_DISCOVER_TASK
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.READER
import org.wordpress.android.util.LocaleManager
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.CoroutineContext

/**
 * Service which updates data for discover tab in Reader, relies on EventBus to notify of changes.
 */
class ReaderDiscoverService : Service(), ServiceCompletionListener, CoroutineScope {
    @Inject @field:Named("IO_THREAD") lateinit var ioDispatcher: CoroutineDispatcher
    private lateinit var readerDiscoverLogic: ReaderDiscoverLogic

    private var job: Job = Job()

    override val coroutineContext: CoroutineContext
        get() = ioDispatcher + job

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleManager.setLocale(newBase))
    }

    override fun onCreate() {
        super.onCreate()
        val component = (application as WordPress).component()
        component.inject(this)
        readerDiscoverLogic = ReaderDiscoverLogic(this, this, component)
        AppLog.i(READER, "reader discover service > created")
    }

    override fun onDestroy() {
        AppLog.i(READER, "reader discover service > destroyed")
        job.cancel()
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null && intent.hasExtra(ARG_DISCOVER_TASK)) {
            val task = intent.getSerializableExtra(ARG_DISCOVER_TASK) as DiscoverTasks
            readerDiscoverLogic.performTasks(task, null)
        }
        return START_NOT_STICKY
    }

    override fun onCompleted(companion: Any) {
        AppLog.i(READER, "reader discover service > all tasks completed")
        stopSelf()
    }
}
