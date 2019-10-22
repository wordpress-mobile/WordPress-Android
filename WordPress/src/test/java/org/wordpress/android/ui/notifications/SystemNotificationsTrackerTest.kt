package org.wordpress.android.ui.notifications

import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.analytics.AnalyticsTracker.Stat.NOTIFICATION_DISMISSED
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper

@RunWith(MockitoJUnitRunner::class)
class SystemNotificationsTrackerTest {
    @Mock lateinit var analyticsTracker: AnalyticsTrackerWrapper
    @Mock lateinit var appPrefs: AppPrefsWrapper
    @Mock lateinit var notificationManager: NotificationManagerWrapper
    private lateinit var systemNotificationsTracker: SystemNotificationsTracker
    @Before
    fun setUp() {
        systemNotificationsTracker = SystemNotificationsTracker(analyticsTracker, appPrefs, notificationManager)
    }

    @Test
    fun `tracks NOTIFICATIONS_ENABLED when system notifications enabled`() {
        whenever(appPrefs.systemNotificationsEnabled).thenReturn(false)
        whenever(notificationManager.areNotificationsEnabled()).thenReturn(true)

        systemNotificationsTracker.checkSystemNotificationsState()

        verify(analyticsTracker).track(Stat.NOTIFICATIONS_ENABLED)
        verify(appPrefs).systemNotificationsEnabled = true
    }

    @Test
    fun `tracks NOTIFICATIONS_DISABLED when system notifications disabled`() {
        whenever(appPrefs.systemNotificationsEnabled).thenReturn(true)
        whenever(notificationManager.areNotificationsEnabled()).thenReturn(false)

        systemNotificationsTracker.checkSystemNotificationsState()

        verify(analyticsTracker).track(Stat.NOTIFICATIONS_DISABLED)
        verify(appPrefs).systemNotificationsEnabled = false
    }

    @Test
    fun `does not track when system notifications haven't changed`() {
        whenever(appPrefs.systemNotificationsEnabled).thenReturn(false)
        whenever(notificationManager.areNotificationsEnabled()).thenReturn(false)

        systemNotificationsTracker.checkSystemNotificationsState()

        verifyZeroInteractions(analyticsTracker)
    }

    @Test
    fun `push notes dismiss tracked correctly`() {
        verifyTrackDismissedNotification(notificationId = 10000, typeValue = "push_notes")
    }

    @Test
    fun `authentication notifications dismiss tracked correctly`() {
        verifyTrackDismissedNotification(notificationId = 20000, typeValue = "authentication")
    }

    @Test
    fun `group notes dismiss tracked correctly`() {
        verifyTrackDismissedNotification(notificationId = 30000, typeValue = "group_notes")
    }

    @Test
    fun `actions result notification dismiss tracked correctly`() {
        verifyTrackDismissedNotification(notificationId = 40000, typeValue = "actions_result")
    }

    @Test
    fun `actions progress notification dismiss tracked correctly`() {
        verifyTrackDismissedNotification(notificationId = 50000, typeValue = "actions_progress")
    }

    @Test
    fun `pending draft notification dismiss tracked correctly`() {
        verifyTrackDismissedNotification(notificationId = 600001, typeValue = "pending_draft")
    }

    @Test
    fun `quick start reminder dismiss tracked correctly`() {
        verifyTrackDismissedNotification(notificationId = 4001, typeValue = "quick_start_reminder")
    }

    @Test
    fun `post upload success notification dismiss tracked correctly`() {
        verifyTrackDismissedNotification(notificationId = 5000, typeValue = "post_upload_success")
    }

    @Test
    fun `post upload error notification dismiss tracked correctly`() {
        verifyTrackDismissedNotification(notificationId = 5100, typeValue = "post_upload_error")
    }

    @Test
    fun `media upload success notification dismiss tracked correctly`() {
        verifyTrackDismissedNotification(notificationId = 5200, typeValue = "media_upload_success")
    }

    @Test
    fun `media upload error notification dismiss tracked correctly`() {
        verifyTrackDismissedNotification(notificationId = 5300, typeValue = "media_upload_error")
    }

    @Test
    fun `post published notification dismiss tracked correctly`() {
        verifyTrackDismissedNotification(notificationId = 5400, typeValue = "post_published")
    }

    @Test
    fun `zendesk notification dismiss tracked correctly`() {
        verifyTrackDismissedNotification(notificationId = 1999999999, typeValue = "zendesk_message")
    }

    private fun verifyTrackDismissedNotification(
        notificationId: Int,
        typeValue: String
    ) {
        systemNotificationsTracker.trackDismissedNotification(notificationId)

        verify(analyticsTracker).track(
                NOTIFICATION_DISMISSED,
                mapOf("notification_type" to typeValue)
        )
    }
}
