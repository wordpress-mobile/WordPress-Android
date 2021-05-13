package org.wordpress.android.push.local

import androidx.core.app.NotificationManagerCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import org.wordpress.android.push.local.LocalPush.Type
import org.wordpress.android.viewmodel.ContextProvider
import javax.inject.Inject

class LocalPushScheduler
@Inject constructor(
    private val contextProvider: ContextProvider,
    private val localPushHandlerFactory: LocalPushHandlerFactory
) {
    fun scheduleOneTimeNotification(localNotification: LocalPush): Boolean {
        val localPushHandler = localPushHandlerFactory.buildLocalPushHandler(localNotification.type)
        if (localPushHandler.shouldShowNotification()) {
            val work = OneTimeWorkRequestBuilder<LocalPushScheduleWorker>()
                    .setInitialDelay(localNotification.delay, localNotification.delayUnits)
                    .addTag(localNotification.type.tag)
                    .setInputData(
                            LocalPushScheduleWorker.buildData(
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
