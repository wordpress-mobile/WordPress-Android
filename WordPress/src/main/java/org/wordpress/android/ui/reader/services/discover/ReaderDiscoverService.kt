package org.wordpress.android.ui.reader.services.discover

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import org.wordpress.android.ui.reader.services.ServiceCompletionListener
import org.wordpress.android.ui.reader.services.discover.ReaderDiscoverLogic.DiscoverTasks
import org.wordpress.android.ui.reader.services.discover.ReaderDiscoverServiceStarter.ARG_DISCOVER_TASKS
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.READER
import org.wordpress.android.util.LocaleManager
import java.util.EnumSet

/**
 * Service which updates data for discover tab in Reader, relies on EventBus to notify of changes.
 */
class ReaderDiscoverService : Service(), ServiceCompletionListener {
    private lateinit var readerDiscoverLogic: ReaderDiscoverLogic
    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleManager.setLocale(newBase))
    }

    override fun onCreate() {
        super.onCreate()
        readerDiscoverLogic = ReaderDiscoverLogic(this)
        AppLog.i(READER, "reader discover service > created")
    }

    override fun onDestroy() {
        AppLog.i(READER, "reader discover service > destroyed")
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null && intent.hasExtra(ARG_DISCOVER_TASKS)) {
            val tasks = intent.getSerializableExtra(ARG_DISCOVER_TASKS) as EnumSet<DiscoverTasks>
            readerDiscoverLogic.performTasks(tasks, null)
        }
        return START_NOT_STICKY
    }

    override fun onCompleted(companion: Any) {
        AppLog.i(READER, "reader discover service > all tasks completed")
        stopSelf()
    }
}
