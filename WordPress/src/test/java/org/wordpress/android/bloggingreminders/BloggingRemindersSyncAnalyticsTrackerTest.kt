package org.wordpress.android.bloggingreminders

import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.bloggingreminders.BloggingRemindersSyncAnalyticsTracker.ErrorType
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper

class BloggingRemindersSyncAnalyticsTrackerTest {
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()
    private val classToTest = BloggingRemindersSyncAnalyticsTracker(analyticsTrackerWrapper)

    @Test
    fun `Should track get blogging reminders start correctly`() {
        classToTest.trackStart()
        verify(analyticsTrackerWrapper).track(Stat.BLOGGING_REMINDERS_SYNC_START)
    }

    @Test
    fun `Should track get blogging reminders success correctly`() {
        val remindersSyncedCount = 3
        classToTest.trackSuccess(remindersSyncedCount)
        verify(analyticsTrackerWrapper).track(
            Stat.BLOGGING_REMINDERS_SYNC_SUCCESS, mapOf(REMINDERS_SYNCED_COUNT to remindersSyncedCount)
        )
    }

    @Test
    fun `Should track failed with QueryBloggingRemindersError correctly`() {
        classToTest.trackFailed(ErrorType.QueryBloggingRemindersError)
        verify(analyticsTrackerWrapper).track(
            Stat.BLOGGING_REMINDERS_SYNC_FAILED,
            mapOf("error_type" to "query_blogging_reminders_error")
        )
    }

    @Test
    fun `Should track failed with UpdateBloggingRemindersError correctly`() {
        classToTest.trackFailed(ErrorType.UpdateBloggingRemindersError)
        verify(analyticsTrackerWrapper).track(
            Stat.BLOGGING_REMINDERS_SYNC_FAILED,
            mapOf("error_type" to "update_blogging_reminders_error")
        )
    }
}
