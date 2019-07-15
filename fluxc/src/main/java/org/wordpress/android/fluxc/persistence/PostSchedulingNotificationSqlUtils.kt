package org.wordpress.android.fluxc.persistence

import com.wellsql.generated.PostSchedulingNotificationTable
import com.yarolegovich.wellsql.WellSql
import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table
import org.wordpress.android.fluxc.store.PostSchedulingNotificationStore.NotificationModel
import org.wordpress.android.fluxc.store.PostSchedulingNotificationStore.ScheduledTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PostSchedulingNotificationSqlUtils
@Inject constructor() {
    fun insert(
        postId: Int,
        scheduledTime: ScheduledTime
    ): Int? {
        deletePostSchedulingNotifications(postId)
        WellSql.insert(
                PostSchedulingNotificationBuilder(
                        postId = postId,
                        scheduledTime = scheduledTime.name
                )
        ).execute()
        return WellSql.select(PostSchedulingNotificationBuilder::class.java)
                .where()
                .equals(PostSchedulingNotificationTable.POST_ID, postId)
                .equals(PostSchedulingNotificationTable.SCHEDULED_TIME, scheduledTime.name)
                .endWhere().asModel.firstOrNull()?.id
    }

    fun deletePostSchedulingNotifications(postId: Int) {
        WellSql.delete(PostSchedulingNotificationBuilder::class.java)
                .where()
                .equals(PostSchedulingNotificationTable.POST_ID, postId)
                .endWhere()
                .execute()
    }

    fun getScheduledTime(
        postId: Int
    ): ScheduledTime? {
        return WellSql.select(PostSchedulingNotificationBuilder::class.java)
                .where()
                .equals(PostSchedulingNotificationTable.POST_ID, postId)
                .endWhere().asModel.firstOrNull()?.scheduledTime?.let { ScheduledTime.valueOf(it) }
    }

    fun getNotification(
        notificationId: Int
    ): NotificationModel? {
        return WellSql.select(PostSchedulingNotificationBuilder::class.java)
                .where()
                .equals(PostSchedulingNotificationTable.ID, notificationId)
                .endWhere().asModel.firstOrNull()
                ?.let { NotificationModel(it.id, it.postId, ScheduledTime.valueOf(it.scheduledTime)) }
    }

    @Table(name = "PostSchedulingNotification")
    data class PostSchedulingNotificationBuilder(
        @PrimaryKey @Column private var mId: Int = -1,
        @Column var postId: Int,
        @Column var scheduledTime: String
    ) : Identifiable {
        constructor() : this(-1, -1, "")

        override fun setId(id: Int) {
            this.mId = id
        }

        override fun getId() = mId
    }
}
