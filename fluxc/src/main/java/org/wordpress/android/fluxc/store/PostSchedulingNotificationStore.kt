package org.wordpress.android.fluxc.store

import org.wordpress.android.fluxc.persistence.PostSchedulingNotificationSqlUtils
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PostSchedulingNotificationStore
@Inject constructor(private val sqlUtils: PostSchedulingNotificationSqlUtils) {
    fun schedule(postId: Long, scheduledTime: ScheduledTime): Int? {
        return sqlUtils.insert(postId, scheduledTime)
    }
    fun deletePostSchedulingNotifications(postId: Long){
        sqlUtils.deletePostSchedulingNotifications(postId)
    }

    fun isScheduled(notificationId: Int): Boolean {
        return sqlUtils.isScheduled(notificationId)
    }

    fun getScheduledTime(postId: Long): ScheduledTime {
        return sqlUtils.getScheduledTime(postId)
    }

    enum class ScheduledTime {
        ONE_HOUR, TEN_MINUTES, WHEN_PUBLISHED, OFF
    }
}
