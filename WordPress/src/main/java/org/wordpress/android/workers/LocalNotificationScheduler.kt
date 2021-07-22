package org.wordpress.android.workers

import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import org.wordpress.android.workers.LocalNotification.Type
import javax.inject.Inject

class LocalNotificationScheduler(private val workManager: WorkManager) {
    @Inject constructor(context: Context) : this(WorkManager.getInstance(context))

    fun scheduleOneTimeNotification(localNotification: LocalNotification) {
        val work = OneTimeWorkRequestBuilder<LocalNotificationWorker>()
                .setInitialDelay(localNotification.delay, localNotification.delayUnits)
                .addTag(localNotification.type.tag)
                .setInputData(LocalNotificationWorker.buildData(localNotification))
                .build()

        workManager.enqueue(work)
    }

    fun cancelScheduledNotification(notificationType: Type) {
        workManager.cancelAllWorkByTag(notificationType.tag)
    }
}
