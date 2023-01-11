package org.wordpress.android.ui.posts

import org.wordpress.android.R
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

class PublishNotificationReceiverViewModel
@Inject constructor(
    private val postSchedulingNotificationStore: PostSchedulingNotificationStore,
    private val postStore: PostStore,
    private val resourceProvider: ResourceProvider
) {
    fun loadNotification(notificationId: Int): NotificationUiModel? {
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
                        OFF -> return null
                    }
                    val title = resourceProvider.getString(titleRes)
                    val message = resourceProvider.getString(messageRes, post.title)
                    return NotificationUiModel(title, message)
                }
            }
        }
        return null
    }

    data class NotificationUiModel(val title: String, val message: String)
}
