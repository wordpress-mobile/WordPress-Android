package org.wordpress.android.ui.posts

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import org.wordpress.android.R
import org.wordpress.android.WordPress
import javax.inject.Inject

class PublishNotificationReceiver : BroadcastReceiver() {
    @Inject lateinit var mPublishNotificationViewModel: PublishNotificationViewModel
    override fun onReceive(context: Context, intent: Intent) {
        (context.applicationContext as WordPress).component().inject(this)
        val notificationId = intent.getIntExtra(NOTIFICATION_ID, 0)
        val notificationUiModel = mPublishNotificationViewModel.loadNotification(notificationId)
        if (notificationUiModel != null) {
            val notificationCompat = NotificationCompat.Builder(
                    context,
                    context.getString(R.string.notification_channel_reminder_id)
            )
                    .setContentTitle(notificationUiModel.title)
                    .setContentText(notificationUiModel.message)
                    .setAutoCancel(true)
                    .setSmallIcon(R.drawable.ic_my_sites_white_24dp)
                    .build()
            NotificationManagerCompat.from(context).notify(notificationId, notificationCompat)
        }
    }

    companion object {
        const val NOTIFICATION_ID = "notification_id"
    }
}
