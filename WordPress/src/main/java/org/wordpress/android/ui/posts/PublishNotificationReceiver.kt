package org.wordpress.android.ui.posts

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.push.NotificationType
import org.wordpress.android.push.NotificationsProcessingService
import org.wordpress.android.ui.notifications.SystemNotificationsTracker
import javax.inject.Inject

class PublishNotificationReceiver : BroadcastReceiver() {
    @Inject
    lateinit var publishNotificationReceiverViewModel: PublishNotificationReceiverViewModel

    @Inject
    lateinit var systemNotificationsTracker: SystemNotificationsTracker
    override fun onReceive(context: Context, intent: Intent) {
        (context.applicationContext as WordPress).component().inject(this)
        val notificationId = intent.getIntExtra(NOTIFICATION_ID, 0)
        val uiModel = publishNotificationReceiverViewModel.loadNotification(notificationId)
        if (uiModel != null) {
            val notificationType = NotificationType.POST_PUBLISHED
            val notificationCompat = NotificationCompat.Builder(
                context,
                context.getString(R.string.notification_channel_reminder_id)
            )
                .setContentTitle(uiModel.title)
                .setContentText(uiModel.message)
                .setAutoCancel(true)
                .setSmallIcon(R.drawable.ic_app_white_24dp)
                .setDeleteIntent(
                    NotificationsProcessingService.getPendingIntentForNotificationDismiss(
                        context,
                        notificationId,
                        notificationType
                    )
                )
                .build()
            NotificationManagerCompat.from(context).notify(notificationId, notificationCompat)
            systemNotificationsTracker.trackShownNotification(notificationType)
        }
    }

    companion object {
        const val NOTIFICATION_ID = "notification_id"
    }
}
