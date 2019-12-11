package org.wordpress.android.ui.notifications

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import javax.inject.Inject

class NotificationManagerWrapper
@Inject constructor(private val context: Context) {
    fun areNotificationsEnabled() = NotificationManagerCompat.from(context).areNotificationsEnabled()
}
