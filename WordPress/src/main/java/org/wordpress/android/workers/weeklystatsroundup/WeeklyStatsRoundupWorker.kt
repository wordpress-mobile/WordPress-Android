package org.wordpress.android.workers.weeklystatsroundup

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import kotlinx.coroutines.coroutineScope
import org.wordpress.android.workers.reminder.ReminderConfig

class WeeklyStatsRoundupWorker(
    val context: Context,
    val scheduler: WeeklyStatsRoundupScheduler,
    val notifier: WeeklyStatsRoundupNotifier,
    workerParameters: WorkerParameters
) : CoroutineWorker(context, workerParameters) {
    override suspend fun doWork(): Result = coroutineScope {
        val siteId = inputData.getInt(
                WeeklyStatsRoundupScheduler.REMINDER_SITE_ID,
                WeeklyStatsRoundupNotifier.NO_SITE_ID)
        val reminderConfig = ReminderConfig.fromMap(inputData.keyValueMap)

        if (notifier.shouldNotify(siteId)) {
            notifier.notify(siteId)
            scheduler.schedule(siteId, reminderConfig)
        }

        Result.success()
    }

    class Factory(
        private val scheduler: WeeklyStatsRoundupScheduler,
        private val notifier: WeeklyStatsRoundupNotifier
    ) : WorkerFactory() {
        override fun createWorker(
            appContext: Context,
            workerClassName: String,
            workerParameters: WorkerParameters
        ) = if (workerClassName == WeeklyStatsRoundupWorker::class.java.name) {
            WeeklyStatsRoundupWorker(appContext, scheduler, notifier, workerParameters)
        } else {
            null
        }
    }
}
