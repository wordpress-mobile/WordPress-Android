package org.wordpress.android.workers.reminder

import android.annotation.SuppressLint
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import javax.inject.Inject

class ReminderNotificationManager @Inject constructor(
    private val context: Context
) {
    @SuppressLint("MissingPermission")
    fun notify(id: Int, notification: ReminderNotification) {
        NotificationManagerCompat.from(context).notify(id, notification.asNotificationCompatBuilder(context).build())
    }
}
