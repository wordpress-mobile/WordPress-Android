package org.wordpress.android.fluxc.persistence

import com.wellsql.generated.PostSchedulingReminderTable
import com.yarolegovich.wellsql.WellSql
import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PostSchedulingNotificationSqlUtils
@Inject constructor() {
    fun insert(
        postId: Int,
        scheduledTime: SchedulingReminderDbModel.Period
    ): Int? {
        WellSql.insert(
                PostSchedulingReminderBuilder(
                        postId = postId,
                        scheduledTime = scheduledTime.name
                )
        ).execute()
        return WellSql.select(PostSchedulingReminderBuilder::class.java)
                .where()
                .equals(PostSchedulingReminderTable.POST_ID, postId)
                .equals(PostSchedulingReminderTable.SCHEDULED_TIME, scheduledTime.name)
                .endWhere().asModel.firstOrNull()?.id
    }

    fun deleteSchedulingReminders(postId: Int) {
        WellSql.delete(PostSchedulingReminderBuilder::class.java)
                .where()
                .equals(PostSchedulingReminderTable.POST_ID, postId)
                .endWhere()
                .execute()
    }

    fun getSchedulingReminderPeriodDbModel(
        postId: Int
    ): SchedulingReminderDbModel.Period? {
        return WellSql.select(PostSchedulingReminderBuilder::class.java)
                .where()
                .equals(PostSchedulingReminderTable.POST_ID, postId)
                .endWhere().asModel.firstOrNull()?.scheduledTime?.let { SchedulingReminderDbModel.Period.valueOf(it) }
    }

    fun getSchedulingReminder(
        notificationId: Int
    ): SchedulingReminderDbModel? {
        return WellSql.select(PostSchedulingReminderBuilder::class.java)
                .where()
                .equals(PostSchedulingReminderTable.ID, notificationId)
                .endWhere().asModel.firstOrNull()
                ?.let {
                    SchedulingReminderDbModel(
                            it.id,
                            it.postId,
                            SchedulingReminderDbModel.Period.valueOf(it.scheduledTime)
                    )
                }
    }

    data class SchedulingReminderDbModel(val notificationId: Int, val postId: Int, val period: Period) {
        enum class Period {
            ONE_HOUR, TEN_MINUTES, WHEN_PUBLISHED
        }
    }

    @Table(name = "PostSchedulingReminder")
    data class PostSchedulingReminderBuilder(
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
