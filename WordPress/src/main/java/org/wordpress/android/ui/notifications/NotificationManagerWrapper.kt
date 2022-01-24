package org.wordpress.android.ui.notifications

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import javax.inject.Inject

class NotificationManagerWrapper
@Inject constructor(private val context: Context) {
    fun areNotificationsEnabled() = NotificationManagerCompat.from(context).areNotificationsEnabled()
    fun notify(id: Int, notification: Notification) = NotificationManagerCompat.from(context).notify(id, notification)
}
