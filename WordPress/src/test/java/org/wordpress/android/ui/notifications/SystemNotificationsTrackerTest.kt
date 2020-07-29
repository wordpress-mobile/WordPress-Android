package org.wordpress.android.ui.notifications

import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.analytics.AnalyticsTracker.Stat.NOTIFICATION_DISMISSED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.NOTIFICATION_SHOWN
import org.wordpress.android.analytics.AnalyticsTracker.Stat.NOTIFICATION_TAPPED
import org.wordpress.android.push.NotificationType
import org.wordpress.android.push.NotificationType.ACTIONS_PROGRESS
import org.wordpress.android.push.NotificationType.ACTIONS_RESULT
import org.wordpress.android.push.NotificationType.AUTHENTICATION
import org.wordpress.android.push.NotificationType.AUTOMATTCHER
import org.wordpress.android.push.NotificationType.BADGE_RESET
import org.wordpress.android.push.NotificationType.COMMENT
import org.wordpress.android.push.NotificationType.COMMENT_LIKE
import org.wordpress.android.push.NotificationType.FOLLOW
import org.wordpress.android.push.NotificationType.GROUP_NOTIFICATION
import org.wordpress.android.push.NotificationType.LIKE
import org.wordpress.android.push.NotificationType.MEDIA_UPLOAD_ERROR
import org.wordpress.android.push.NotificationType.MEDIA_UPLOAD_SUCCESS
import org.wordpress.android.push.NotificationType.NOTE_DELETE
import org.wordpress.android.push.NotificationType.PENDING_DRAFTS
import org.wordpress.android.push.NotificationType.POST_PUBLISHED
import org.wordpress.android.push.NotificationType.POST_UPLOAD_ERROR
import org.wordpress.android.push.NotificationType.POST_UPLOAD_SUCCESS
import org.wordpress.android.push.NotificationType.QUICK_START_REMINDER
import org.wordpress.android.push.NotificationType.REBLOG
import org.wordpress.android.push.NotificationType.STORY_FRAME_SAVE_ERROR
import org.wordpress.android.push.NotificationType.STORY_FRAME_SAVE_SUCCESS
import org.wordpress.android.push.NotificationType.STORY_SAVE_ERROR
import org.wordpress.android.push.NotificationType.STORY_SAVE_SUCCESS
import org.wordpress.android.push.NotificationType.TEST_NOTE
import org.wordpress.android.push.NotificationType.UNKNOWN_NOTE
import org.wordpress.android.push.NotificationType.ZENDESK
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper

@RunWith(MockitoJUnitRunner::class)
class SystemNotificationsTrackerTest {
    @Mock lateinit var analyticsTracker: AnalyticsTrackerWrapper
    @Mock lateinit var appPrefs: AppPrefsWrapper
    @Mock lateinit var notificationManager: NotificationManagerWrapper
    private lateinit var systemNotificationsTracker: SystemNotificationsTracker
    private val typeToValue = mapOf(
            COMMENT to "comment",
            LIKE to "like",
            COMMENT_LIKE to "comment_like",
            AUTOMATTCHER to "automattcher",
            FOLLOW to "follow",
            REBLOG to "reblog",
            BADGE_RESET to "badge_reset",
            NOTE_DELETE to "note_delete",
            TEST_NOTE to "test_note",
            UNKNOWN_NOTE to "unknown_note",
            AUTHENTICATION to "authentication",
            GROUP_NOTIFICATION to "group_notes",
            ACTIONS_RESULT to "actions_result",
            ACTIONS_PROGRESS to "actions_progress",
            QUICK_START_REMINDER to "quick_start_reminder",
            POST_UPLOAD_SUCCESS to "post_upload_success",
            POST_UPLOAD_ERROR to "post_upload_error",
            MEDIA_UPLOAD_SUCCESS to "media_upload_success",
            MEDIA_UPLOAD_ERROR to "media_upload_error",
            POST_PUBLISHED to "post_published",
            STORY_SAVE_SUCCESS to "story_save_success",
            STORY_SAVE_ERROR to "story_save_error",
            STORY_FRAME_SAVE_SUCCESS to "story_frame_save_success",
            STORY_FRAME_SAVE_ERROR to "story_frame_save_error",
            PENDING_DRAFTS to "pending_draft",
            ZENDESK to "zendesk_message"
    )
    @Before
    fun setUp() {
        systemNotificationsTracker = SystemNotificationsTracker(
                analyticsTracker,
                appPrefs,
                notificationManager
        )
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
    fun `notification dismiss tracked correctly`() {
        val notificationTypes = NotificationType.values().toMutableSet()
        typeToValue.forEach { (notificationType, trackingValue) ->
            verifyTrackDismissedNotification(notificationType = notificationType, typeValue = trackingValue)
            notificationTypes.remove(notificationType)
        }

        // Check that all the items are covered by the test
        assertThat(notificationTypes).isEmpty()
    }

    @Test
    fun `notification tap tracked correctly`() {
        val notificationTypes = NotificationType.values().toMutableSet()
        typeToValue.forEach { (notificationType, trackingValue) ->
            verifyTrackTappedNotification(notificationType = notificationType, typeValue = trackingValue)
            notificationTypes.remove(notificationType)
        }

        // Check that all the items are covered by the test
        assertThat(notificationTypes).isEmpty()
    }

    @Test
    fun `notification shown tracked correctly`() {
        val notificationTypes = NotificationType.values().toMutableSet()
        typeToValue.forEach { (notificationType, trackingValue) ->
            verifyTrackShowNotification(notificationType = notificationType, typeValue = trackingValue)
            notificationTypes.remove(notificationType)
        }

        // Check that all the items are covered by the test
        assertThat(notificationTypes).isEmpty()
    }

    private fun verifyTrackDismissedNotification(
        notificationType: NotificationType,
        typeValue: String
    ) {
        systemNotificationsTracker.trackDismissedNotification(notificationType)

        verify(analyticsTracker).track(
                NOTIFICATION_DISMISSED,
                mapOf("notification_type" to typeValue)
        )
    }

    private fun verifyTrackTappedNotification(
        notificationType: NotificationType,
        typeValue: String
    ) {
        systemNotificationsTracker.trackTappedNotification(notificationType)

        verify(analyticsTracker).track(
                NOTIFICATION_TAPPED,
                mapOf("notification_type" to typeValue)
        )
    }

    private fun verifyTrackShowNotification(
        notificationType: NotificationType,
        typeValue: String
    ) {
        systemNotificationsTracker.trackShownNotification(notificationType)

        verify(analyticsTracker).track(
                NOTIFICATION_SHOWN,
                mapOf("notification_type" to typeValue)
        )
    }
}
