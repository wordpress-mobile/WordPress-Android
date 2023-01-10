package org.wordpress.android.workers.notification.local

import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import org.wordpress.android.workers.notification.local.LocalNotification.Type
import javax.inject.Inject

class LocalNotificationScheduler(private val workManager: WorkManager) {
    @Inject
    constructor(context: Context) : this(WorkManager.getInstance(context))

    fun scheduleOneTimeNotification(vararg localNotifications: LocalNotification) {
        workManager.enqueue(localNotifications.map { buildOneTimeWorkRequest(it) })
    }

    fun cancelScheduledNotification(notificationType: Type) {
        workManager.cancelAllWorkByTag(notificationType.tag)
    }

    private fun buildOneTimeWorkRequest(localNotification: LocalNotification) =
        OneTimeWorkRequestBuilder<LocalNotificationWorker>()
            .setInitialDelay(localNotification.delay, localNotification.delayUnits)
            .addTag(localNotification.type.tag)
            .setInputData(LocalNotificationWorker.buildData(localNotification))
            .build()
}
