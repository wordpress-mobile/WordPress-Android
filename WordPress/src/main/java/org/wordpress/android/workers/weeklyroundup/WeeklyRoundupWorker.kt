package org.wordpress.android.workers.weeklyroundup

import android.annotation.SuppressLint
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import kotlinx.coroutines.coroutineScope

class WeeklyRoundupWorker(
    val context: Context,
    val notifier: WeeklyRoundupNotifier,
    workerParameters: WorkerParameters
) : CoroutineWorker(context, workerParameters) {
    override suspend fun doWork(): Result = coroutineScope {
        if (notifier.shouldShowNotifications()) {
            notifier.buildNotifications()
                    .onEach(::showNotification)
                    .also(notifier::onNotificationsShown)
        }

        Result.success()
    }

    @SuppressLint("MissingPermission")
    private fun showNotification(notification: WeeklyRoundupNotification) {
        NotificationManagerCompat.from(context)
                .notify(notification.id, notification.asNotificationCompatBuilder(context).build())
    }

    class Factory(
        private val notifier: WeeklyRoundupNotifier
    ) : WorkerFactory() {
        override fun createWorker(
            appContext: Context,
            workerClassName: String,
            workerParameters: WorkerParameters
        ) = if (workerClassName == WeeklyRoundupWorker::class.java.name) {
            WeeklyRoundupWorker(appContext, notifier, workerParameters)
        } else {
            null
        }
    }
}
