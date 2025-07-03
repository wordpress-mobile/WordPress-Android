package org.wordpress.android.ui.notifications

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import javax.inject.Inject

class NotificationManagerWrapper
@Inject constructor(private val context: Context) {
    fun areNotificationsEnabled() = NotificationManagerCompat.from(context).areNotificationsEnabled()
    fun notify(id: Int, notification: Notification) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.app.ActivityCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                NotificationManagerCompat.from(context).notify(id, notification)
            }
        } else {
            NotificationManagerCompat.from(context).notify(id, notification)
        }
    }
}
