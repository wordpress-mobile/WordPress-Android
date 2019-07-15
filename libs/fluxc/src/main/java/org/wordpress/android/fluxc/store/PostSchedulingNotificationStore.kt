package org.wordpress.android.fluxc.store

import org.wordpress.android.fluxc.persistence.PostSchedulingNotificationSqlUtils
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PostSchedulingNotificationStore
@Inject constructor(private val sqlUtils: PostSchedulingNotificationSqlUtils) {
    fun schedule(postId: Int, scheduledTime: ScheduledTime): Int? {
        return sqlUtils.insert(postId, scheduledTime)
    }
    fun deletePostSchedulingNotifications(postId: Int){
        sqlUtils.deletePostSchedulingNotifications(postId)
    }

    fun getNotification(notificationId: Int): NotificationModel? {
        return sqlUtils.getNotification(notificationId)
    }

    fun getScheduledTime(postId: Int): ScheduledTime? {
        return sqlUtils.getScheduledTime(postId)
    }

    enum class ScheduledTime {
        ONE_HOUR, TEN_MINUTES, WHEN_PUBLISHED
    }

    data class NotificationModel(val notificationId: Int, val postId: Int, val scheduledTime: ScheduledTime)
}
