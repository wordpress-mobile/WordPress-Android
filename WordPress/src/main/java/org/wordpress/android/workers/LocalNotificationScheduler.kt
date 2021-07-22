package org.wordpress.android.workers

import androidx.core.app.NotificationManagerCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import org.wordpress.android.viewmodel.ContextProvider
import org.wordpress.android.workers.LocalNotification.Type
import javax.inject.Inject

class LocalNotificationScheduler @Inject constructor(
    private val contextProvider: ContextProvider,
    private val localNotificationHandlerFactory: LocalNotificationHandlerFactory
) {
    private val workManager = WorkManager.getInstance(contextProvider.getContext())
    
    fun scheduleOneTimeNotification(localNotification: LocalNotification): Boolean {
        val localPushHandler = localNotificationHandlerFactory.buildLocalNotificationHandler(localNotification.type)
        if (localPushHandler.shouldShowNotification()) {
            val work = OneTimeWorkRequestBuilder<LocalNotificationWorker>()
                    .setInitialDelay(localNotification.delay, localNotification.delayUnits)
                    .addTag(localNotification.type.tag)
                    .setInputData(
                            LocalNotificationWorker.buildData(
                                    localNotification
                            )
                    )
                    .build()

            workManager.enqueue(work)
            return true
        } else {
            return false
        }
    }

    fun cancelScheduledNotification(notificationType: Type) {
        workManager.cancelAllWorkByTag(notificationType.tag)
    }

    fun cancelNotification(pushId: Int) {
        if (pushId != -1) {
            with(NotificationManagerCompat.from(contextProvider.getContext())) {
                cancel(pushId)
            }
        }
    }
}
