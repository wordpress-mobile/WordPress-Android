package org.wordpress.android.ui.posts

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.fluxc.model.post.PostStatus.SCHEDULED
import org.wordpress.android.fluxc.store.PostSchedulingNotificationStore
import org.wordpress.android.fluxc.store.PostSchedulingNotificationStore.SchedulingReminderModel.Period.OFF
import org.wordpress.android.fluxc.store.PostSchedulingNotificationStore.SchedulingReminderModel.Period.ONE_HOUR
import org.wordpress.android.fluxc.store.PostSchedulingNotificationStore.SchedulingReminderModel.Period.TEN_MINUTES
import org.wordpress.android.fluxc.store.PostSchedulingNotificationStore.SchedulingReminderModel.Period.WHEN_PUBLISHED
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject

class PublishNotificationReceiver : BroadcastReceiver() {
    @Inject lateinit var postSchedulingNotificationStore: PostSchedulingNotificationStore
    @Inject lateinit var postStore: PostStore
    @Inject lateinit var resourceProvider: ResourceProvider
    override fun onReceive(context: Context, intent: Intent) {
        (context.applicationContext as WordPress).component().inject(this)
        val notificationId = intent.getIntExtra(NOTIFICATION_ID, 0)
        postSchedulingNotificationStore.getSchedulingReminder(notificationId)?.let { notification ->
            postStore.getPostByLocalPostId(notification.postId)?.let { post ->
                val postStatus = PostStatus.fromPost(post)
                if (postStatus == SCHEDULED) {
                    val (titleRes, messageRes) = when (notification.scheduledTime) {
                        ONE_HOUR -> Pair(
                                R.string.notification_scheduled_post_one_hour_reminder,
                                R.string.notification_post_will_be_published_in_one_hour
                        )
                        TEN_MINUTES -> Pair(
                                R.string.notification_scheduled_post_ten_minute_reminder,
                                R.string.notification_post_will_be_published_in_ten_minutes
                        )
                        WHEN_PUBLISHED -> Pair(
                                R.string.notification_scheduled_post,
                                R.string.notification_post_has_been_published
                        )
                        OFF -> return
                    }
                    val title = resourceProvider.getString(titleRes)
                    val message = resourceProvider.getString(messageRes, post.title)
                    val notificationCompat = NotificationCompat.Builder(
                            context,
                            context.getString(R.string.notification_channel_reminder_id)
                    )
                            .setContentTitle(title)
                            .setContentText(message)
                            .setAutoCancel(true)
                            .setSmallIcon(R.drawable.ic_my_sites_white_24dp)
                            .build()
                    NotificationManagerCompat.from(context).notify(notificationId, notificationCompat)
                }
            }
        }
    }

    companion object {
        const val NOTIFICATION_ID = "notification_id"
    }
}
