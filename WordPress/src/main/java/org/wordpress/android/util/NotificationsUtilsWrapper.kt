package org.wordpress.android.util

import androidx.core.app.NotificationManagerCompat
import javax.inject.Inject

class NotificationsUtilsWrapper @Inject constructor() {
    fun cancelAll(activity : android.app.Activity) {
        NotificationManagerCompat.from(activity).cancelAll()
    }
}
