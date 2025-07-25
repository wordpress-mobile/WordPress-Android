package org.wordpress.android.workers.reminder

import android.content.Context
import org.wordpress.android.ui.notifications.NotificationManagerWrapper
import javax.inject.Inject

class ReminderNotificationManager @Inject constructor(
    private val context: Context,
    private val notificationManagerWrapper: NotificationManagerWrapper
) {
    fun notify(id: Int, notification: ReminderNotification) {
        notificationManagerWrapper.notify(id, notification.asNotificationCompatBuilder(context).build())
    }
}
