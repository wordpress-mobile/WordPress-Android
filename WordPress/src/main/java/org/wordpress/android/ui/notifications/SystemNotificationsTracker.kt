package org.wordpress.android.ui.notifications

import org.wordpress.android.analytics.AnalyticsTracker.Stat
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
import javax.inject.Inject

private const val SYSTEM_NOTIFICATIONS_ENABLED = "system_notifications_enabled"

class SystemNotificationsTracker
@Inject constructor(
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val appPrefs: AppPrefsWrapper,
    private val notificationManager: NotificationManagerWrapper
) {
    fun checkSystemNotificationsState() {
        val previousState = appPrefs.systemNotificationsEnabled
        val notificationsEnabled = notificationManager.areNotificationsEnabled()

        if (previousState != notificationsEnabled) {
            appPrefs.systemNotificationsEnabled = notificationsEnabled
            if (notificationsEnabled) {
                analyticsTracker.track(Stat.NOTIFICATIONS_ENABLED)
            } else {
                analyticsTracker.track(Stat.NOTIFICATIONS_DISABLED)
            }
        }
    }

    fun track(stat: Stat) {
        val notificationsEnabled = notificationManager.areNotificationsEnabled()
        analyticsTracker.track(
                stat,
                mapOf(SYSTEM_NOTIFICATIONS_ENABLED to notificationsEnabled)
        )
    }

    fun trackShownNotification(notificationType: NotificationType) {
        val notificationTypeValue = notificationType.toTypeValue()
        val properties = mapOf(NOTIFICATION_TYPE_KEY to notificationTypeValue)
        analyticsTracker.track(Stat.NOTIFICATION_SHOWN, properties)
    }

    fun trackTappedNotification(notificationType: NotificationType) {
        val notificationTypeValue = notificationType.toTypeValue()
        val properties = mapOf(NOTIFICATION_TYPE_KEY to notificationTypeValue)
        analyticsTracker.track(Stat.NOTIFICATION_TAPPED, properties)
    }

    fun trackDismissedNotification(notificationType: NotificationType) {
        val notificationTypeValue = notificationType.toTypeValue()
        val properties = mapOf(NOTIFICATION_TYPE_KEY to notificationTypeValue)
        analyticsTracker.track(Stat.NOTIFICATION_DISMISSED, properties)
    }

    private fun NotificationType.toTypeValue(): String {
        return when (this) {
            COMMENT -> COMMENT_VALUE
            LIKE -> LIKE_VALUE
            COMMENT_LIKE -> COMMENT_LIKE_VALUE
            AUTOMATTCHER -> AUTOMATTCHER_VALUE
            FOLLOW -> FOLLOW_VALUE
            REBLOG -> REBLOG_VALUE
            BADGE_RESET -> BADGE_RESET_VALUE
            NOTE_DELETE -> NOTE_DELETE_VALUE
            TEST_NOTE -> TEST_NOTE_VALUE
            UNKNOWN_NOTE -> UNKNOWN_NOTE_VALUE
            AUTHENTICATION -> AUTHENTICATION_TYPE_VALUE
            GROUP_NOTIFICATION -> GROUP_NOTES_TYPE_VALUE
            ACTIONS_RESULT -> ACTIONS_RESULT_TYPE_VALUE
            ACTIONS_PROGRESS -> ACTIONS_PROGRESS_TYPE_VALUE
            QUICK_START_REMINDER -> QUICK_START_REMINDER_TYPE_VALUE
            POST_UPLOAD_SUCCESS -> POST_UPLOAD_SUCCESS_TYPE_VALUE
            POST_UPLOAD_ERROR -> POST_UPLOAD_ERROR_TYPE_VALUE
            MEDIA_UPLOAD_SUCCESS -> MEDIA_UPLOAD_SUCCESS_TYPE_VALUE
            MEDIA_UPLOAD_ERROR -> MEDIA_UPLOAD_ERROR_TYPE_VALUE
            POST_PUBLISHED -> POST_PUBLISHED_TYPE_VALUE
            STORY_SAVE_SUCCESS -> STORY_SAVE_SUCCESS_TYPE_VALUE
            STORY_SAVE_ERROR -> STORY_SAVE_ERROR_TYPE_VALUE
            STORY_FRAME_SAVE_SUCCESS -> STORY_FRAME_SAVE_SUCCESS_TYPE_VALUE
            STORY_FRAME_SAVE_ERROR -> STORY_FRAME_SAVE_ERROR_TYPE_VALUE
            PENDING_DRAFTS -> PENDING_DRAFT_TYPE_VALUE
            ZENDESK -> ZENDESK_MESSAGE_TYPE_VALUE
        }
    }

    companion object {
        private const val NOTIFICATION_TYPE_KEY = "notification_type"

        private const val COMMENT_VALUE = "comment"
        private const val LIKE_VALUE = "like"
        private const val COMMENT_LIKE_VALUE = "comment_like"
        private const val AUTOMATTCHER_VALUE = "automattcher"
        private const val FOLLOW_VALUE = "follow"
        private const val REBLOG_VALUE = "reblog"
        private const val BADGE_RESET_VALUE = "badge_reset"
        private const val NOTE_DELETE_VALUE = "note_delete"
        private const val TEST_NOTE_VALUE = "test_note"
        private const val UNKNOWN_NOTE_VALUE = "unknown_note"
        private const val AUTHENTICATION_TYPE_VALUE = "authentication"
        private const val GROUP_NOTES_TYPE_VALUE = "group_notes"
        private const val ACTIONS_RESULT_TYPE_VALUE = "actions_result"
        private const val ACTIONS_PROGRESS_TYPE_VALUE = "actions_progress"
        private const val QUICK_START_REMINDER_TYPE_VALUE = "quick_start_reminder"
        private const val POST_UPLOAD_SUCCESS_TYPE_VALUE = "post_upload_success"
        private const val POST_UPLOAD_ERROR_TYPE_VALUE = "post_upload_error"
        private const val MEDIA_UPLOAD_SUCCESS_TYPE_VALUE = "media_upload_success"
        private const val MEDIA_UPLOAD_ERROR_TYPE_VALUE = "media_upload_error"
        private const val POST_PUBLISHED_TYPE_VALUE = "post_published"
        private const val STORY_SAVE_SUCCESS_TYPE_VALUE = "story_save_success"
        private const val STORY_SAVE_ERROR_TYPE_VALUE = "story_save_error"
        private const val STORY_FRAME_SAVE_SUCCESS_TYPE_VALUE = "story_frame_save_success"
        private const val STORY_FRAME_SAVE_ERROR_TYPE_VALUE = "story_frame_save_error"
        private const val PENDING_DRAFT_TYPE_VALUE = "pending_draft"
        private const val ZENDESK_MESSAGE_TYPE_VALUE = "zendesk_message"
    }
}
