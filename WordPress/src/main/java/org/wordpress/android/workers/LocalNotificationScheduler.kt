package org.wordpress.android.workers

import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import org.wordpress.android.viewmodel.ContextProvider
import org.wordpress.android.workers.LocalNotification.Type
import javax.inject.Inject

class LocalNotificationScheduler @Inject constructor(
    contextProvider: ContextProvider
) {
    private val workManager = WorkManager.getInstance(contextProvider.getContext())

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
