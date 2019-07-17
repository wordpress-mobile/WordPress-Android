package org.wordpress.android.fluxc.store

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.inOrder
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
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
        periodMappings.entries.forEach { (domainModel, dbModel) ->
            store.schedule(postId, domainModel)

            inOrder(sqlUtils).apply {
                this.verify(sqlUtils).deletePostSchedulingNotifications(postId)
                this.verify(sqlUtils).insert(postId, dbModel)
            }
        }
    }

    @Test
    fun `schedule deletes previous notification when OFF`() {
        store.schedule(postId, SchedulingReminderModel.Period.OFF)

        verify(sqlUtils).deletePostSchedulingNotifications(postId)
        verify(sqlUtils, never()).insert(any(), any())
    }

    @Test
    fun `deletes notification per post`() {
        store.deletePostSchedulingNotifications(postId)

        verify(sqlUtils).deletePostSchedulingNotifications(postId)
    }

    @Test
    fun `returns notification from database`() {
        periodMappings.entries.forEach { (domainModel, dbModel) ->
            whenever(sqlUtils.getNotification(notificationId)).thenReturn(
                    SchedulingReminderDbModel(
                            notificationId,
                            postId,
                            dbModel
                    )
            )
            val schedulingReminderModel = store.getNotification(notificationId)

            assertThat(schedulingReminderModel!!.notificationId).isEqualTo(notificationId)
            assertThat(schedulingReminderModel.postId).isEqualTo(postId)
            assertThat(schedulingReminderModel.scheduledTime).isEqualTo(domainModel)
        }
    }

    @Test
    fun `returns scheduling reminder period from database`() {
        periodMappings.entries.forEach { (domainModel, dbModel) ->
            whenever(sqlUtils.getSchedulingReminderPeriodDbModel(postId)).thenReturn(dbModel)
            val schedulingReminderPeriod = store.getSchedulingReminderPeriod(postId)

            assertThat(schedulingReminderPeriod).isEqualTo(domainModel)
        }
    }
}
