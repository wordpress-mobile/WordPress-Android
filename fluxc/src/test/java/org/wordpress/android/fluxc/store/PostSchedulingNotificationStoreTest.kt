package org.wordpress.android.fluxc.store

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.persistence.PostSchedulingNotificationSqlUtils
import org.wordpress.android.fluxc.persistence.PostSchedulingNotificationSqlUtils.SchedulingReminderDbModel
import org.wordpress.android.fluxc.store.PostSchedulingNotificationStore.SchedulingReminderModel

@RunWith(MockitoJUnitRunner::class)
class PostSchedulingNotificationStoreTest {
    @Mock lateinit var sqlUtils: PostSchedulingNotificationSqlUtils
    private lateinit var store: PostSchedulingNotificationStore
    private val postId = 1
    private val notificationId = 2
    private val periodMappings = mapOf(
            SchedulingReminderModel.Period.ONE_HOUR to SchedulingReminderDbModel.Period.ONE_HOUR,
            SchedulingReminderModel.Period.TEN_MINUTES to SchedulingReminderDbModel.Period.TEN_MINUTES,
            SchedulingReminderModel.Period.WHEN_PUBLISHED to SchedulingReminderDbModel.Period.WHEN_PUBLISHED
    )

    @Before
    fun setUp() {
        store = PostSchedulingNotificationStore(sqlUtils)
    }

    @Test
    fun `schedule deletes previous notification and inserts update when set`() {
        periodMappings.entries.forEach { (schedulingReminderModel, dbModel) ->
            store.schedule(postId, schedulingReminderModel)

            inOrder(sqlUtils).apply {
                this.verify(sqlUtils).deleteSchedulingReminders(postId)
                this.verify(sqlUtils).insert(postId, dbModel)
            }
        }
    }

    @Test
    fun `schedule deletes previous notification when OFF`() {
        store.schedule(postId, SchedulingReminderModel.Period.OFF)

        verify(sqlUtils).deleteSchedulingReminders(postId)
        verify(sqlUtils, never()).insert(any(), any())
    }

    @Test
    fun `deletes notification per post`() {
        store.deleteSchedulingReminders(postId)

        verify(sqlUtils).deleteSchedulingReminders(postId)
    }

    @Test
    fun `returns notification from database`() {
        periodMappings.entries.forEach { (schedulingReminderModel, dbModel) ->
            whenever(sqlUtils.getSchedulingReminder(notificationId)).thenReturn(
                    SchedulingReminderDbModel(
                            notificationId,
                            postId,
                            dbModel
                    )
            )
            val schedulingReminder = store.getSchedulingReminder(notificationId)

            assertThat(schedulingReminder!!.notificationId).isEqualTo(notificationId)
            assertThat(schedulingReminder.postId).isEqualTo(postId)
            assertThat(schedulingReminder.scheduledTime).isEqualTo(schedulingReminderModel)
        }
    }

    @Test
    fun `returns scheduling reminder period from database`() {
        periodMappings.entries.forEach { (schedulingReminderModel, dbModel) ->
            whenever(sqlUtils.getSchedulingReminderPeriodDbModel(postId)).thenReturn(dbModel)
            val schedulingReminderPeriod = store.getSchedulingReminderPeriod(postId)

            assertThat(schedulingReminderPeriod).isEqualTo(schedulingReminderModel)
        }
    }
}
