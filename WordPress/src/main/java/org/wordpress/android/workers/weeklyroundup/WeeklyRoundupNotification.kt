package org.wordpress.android.workers.weeklyroundup

import android.app.PendingIntent
import android.content.Context
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import org.wordpress.android.R

data class WeeklyRoundupNotification(
    val id: Int,
    @StringRes val channel: Int = R.string.notification_channel_weekly_roundup_id,
    val contentIntentBuilder: () -> PendingIntent,
    val contentTitle: String,
    val contentText: String,
    val priority: Int = NotificationCompat.PRIORITY_DEFAULT,
    val category: String = NotificationCompat.CATEGORY_REMINDER,
    val autoCancel: Boolean = true,
    val colorized: Boolean = true,
    @ColorRes val color: Int = R.color.blue_50,
    @DrawableRes val smallIcon: Int = R.drawable.ic_app_white_24dp
) {
    fun asNotificationCompatBuilder(context: Context): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, context.getString(channel))
                .setContentIntent(contentIntentBuilder())
                .setContentTitle(contentTitle)
                .setContentText(contentText)
                .setPriority(priority)
                .setCategory(category)
                .setAutoCancel(autoCancel)
                .setColorized(colorized)
                .setColor(ContextCompat.getColor(context, color))
                .setSmallIcon(smallIcon)
    }
}
