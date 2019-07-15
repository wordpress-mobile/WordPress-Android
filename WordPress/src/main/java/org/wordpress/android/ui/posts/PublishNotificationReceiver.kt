package org.wordpress.android.ui.posts

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import org.wordpress.android.R

class PublishNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra(NOTIFICATION_ID, 0)
        val title = intent.getStringExtra(NOTIFICATION_TITLE)
        val message = intent.getStringExtra(NOTIFICATION_MESSAGE)
        val notificationCompat = NotificationCompat.Builder(
                context,
                context.getString(R.string.notification_channel_normal_id)
        )
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setSmallIcon(R.drawable.ic_my_sites_white_24dp)
                .build()
        NotificationManagerCompat.from(context).notify(notificationId, notificationCompat)
    }

    companion object {
        const val NOTIFICATION_ID = "notification_id"
        const val NOTIFICATION_TITLE = "notification_title"
        const val NOTIFICATION_MESSAGE = "notification_message"
    }
}
