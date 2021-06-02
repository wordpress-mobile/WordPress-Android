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

            WorkManager.getInstance(contextProvider.getContext()).enqueue(
                    work
            )
            return true
        } else {
            return false
        }
    }

    fun cancelScheduledNotification(notificationType: Type) {
        WorkManager.getInstance(contextProvider.getContext()).cancelAllWorkByTag(notificationType.tag)
    }

    fun cancelNotification(pushId: Int) {
        if (pushId != -1) {
            with(NotificationManagerCompat.from(contextProvider.getContext())) {
                cancel(pushId)
            }
        }
    }
}
