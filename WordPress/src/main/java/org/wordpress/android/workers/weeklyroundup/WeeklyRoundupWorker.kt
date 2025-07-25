package org.wordpress.android.workers.weeklyroundup

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import kotlinx.coroutines.coroutineScope
import org.wordpress.android.ui.notifications.NotificationManagerWrapper

class WeeklyRoundupWorker(
    val context: Context,
    val notifier: WeeklyRoundupNotifier,
    val notificationManagerWrapper: NotificationManagerWrapper,
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

    private fun showNotification(notification: WeeklyRoundupNotification) {
        notificationManagerWrapper.notify(notification.id, notification.asNotificationCompatBuilder(context).build())
    }

    class Factory(
        private val notifier: WeeklyRoundupNotifier,
        private val notificationManagerWrapper: NotificationManagerWrapper
    ) : WorkerFactory() {
        override fun createWorker(
            appContext: Context,
            workerClassName: String,
            workerParameters: WorkerParameters
        ) = if (workerClassName == WeeklyRoundupWorker::class.java.name) {
            WeeklyRoundupWorker(appContext, notifier, notificationManagerWrapper, workerParameters)
        } else {
            null
        }
    }
}
