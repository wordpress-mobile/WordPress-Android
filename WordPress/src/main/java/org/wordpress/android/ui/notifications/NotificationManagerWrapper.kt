package org.wordpress.android.ui.notifications

import android.Manifest
import android.app.Notification
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import javax.inject.Inject

class NotificationManagerWrapper @Inject constructor(private val context: Context) {
    fun areNotificationsEnabled() = NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun notify(id: Int, notification: Notification) {
        // Check if notifications are enabled at system level first
        if (!areNotificationsEnabled()) {
            AppLog.d(T.NOTIFS, "Notifications disabled by user, skipping notification with id: $id")
            return
        }

        // Check for POST_NOTIFICATIONS permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            AppLog.w(T.NOTIFS, "POST_NOTIFICATIONS permission not granted, skipping notification with id: $id")
            return
        }

        NotificationManagerCompat.from(context).notify(id, notification)
    }

    fun cancel(id: Int) = NotificationManagerCompat.from(context).cancel(id)
}
