package org.wordpress.android.workers.reminder

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import javax.inject.Inject

class ReminderNotificationManager @Inject constructor(
    private val context: Context
) {
    fun notify(id: Int, notification: ReminderNotification) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.app.ActivityCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                NotificationManagerCompat.from(context).notify(id, notification.asNotificationCompatBuilder(context).build())
            }
        } else {
            NotificationManagerCompat.from(context).notify(id, notification.asNotificationCompatBuilder(context).build())
        }
    }
}
