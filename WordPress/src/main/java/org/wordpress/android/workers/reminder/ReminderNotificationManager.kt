package org.wordpress.android.workers.reminder

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import javax.inject.Inject

class ReminderNotificationManager @Inject constructor(
    private val context: Context
) {
    fun notify(id: Int, notification: ReminderNotification) {
        NotificationManagerCompat.from(context).notify(id, notification.asNotificationCompatBuilder(context).build())
    }
}
