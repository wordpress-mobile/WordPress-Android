package org.wordpress.android.workers.reminder

import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.PRIORITY_DEFAULT

data class ReminderNotification(
    val channel: String,
    val contentIntentBuilder: () -> PendingIntent,
    val contentTitle: String,
    val contentText: String,
    val priority: Int = PRIORITY_DEFAULT,
    val category: String,
    val autoCancel: Boolean = true,
    val colorized: Boolean = true,
    val color: Int,
    val smallIcon: Int
) {
    fun asNotificationCompatBuilder(context: Context): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, channel)
                .setContentIntent(contentIntentBuilder.invoke())
                .setContentTitle(contentTitle)
                .setContentText(contentText)
                .setPriority(priority)
                .setCategory(category)
                .setAutoCancel(autoCancel)
                .setColorized(colorized)
                .setColor(color)
                .setSmallIcon(smallIcon)
    }
}
