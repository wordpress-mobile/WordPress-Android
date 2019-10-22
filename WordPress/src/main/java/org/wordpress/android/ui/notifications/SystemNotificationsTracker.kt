package org.wordpress.android.ui.notifications

import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.push.NotificationPushId
import org.wordpress.android.push.NotificationPushId.ACTIONS_PROGRESS_NOTIFICATION_ID
import org.wordpress.android.push.NotificationPushId.ACTIONS_RESULT_NOTIFICATION_ID
import org.wordpress.android.push.NotificationPushId.AUTH_PUSH_NOTIFICATION_ID
import org.wordpress.android.push.NotificationPushId.GROUP_NOTIFICATION_ID
import org.wordpress.android.push.NotificationPushId.MEDIA_UPLOAD_ERROR
import org.wordpress.android.push.NotificationPushId.MEDIA_UPLOAD_SUCCESS
import org.wordpress.android.push.NotificationPushId.PENDING_DRAFTS_NOTIFICATION_ID
import org.wordpress.android.push.NotificationPushId.POST_PUBLISHED_NOTIFICATION
import org.wordpress.android.push.NotificationPushId.POST_UPLOAD_ERROR
import org.wordpress.android.push.NotificationPushId.POST_UPLOAD_SUCCESS
import org.wordpress.android.push.NotificationPushId.PUSH_NOTIFICATION_ID
import org.wordpress.android.push.NotificationPushId.QUICK_START_REMINDER_NOTIFICATION
import org.wordpress.android.push.NotificationPushId.ZENDESK_PUSH_NOTIFICATION_ID
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

    fun trackDismissedNotification(pushId: Int) {
        val notificationTypeValue = when (NotificationPushId.fromValue(pushId)) {
            PUSH_NOTIFICATION_ID -> PUSH_NOTES_VALUE
            AUTH_PUSH_NOTIFICATION_ID -> AUTHENTICATION_TYPE_VALUE
            GROUP_NOTIFICATION_ID -> GROUP_NOTES_TYPE_VALUE
            ACTIONS_RESULT_NOTIFICATION_ID -> ACTIONS_RESULT_TYPE_VALUE
            ACTIONS_PROGRESS_NOTIFICATION_ID -> ACTIONS_PROGRESS_TYPE_VALUE
            QUICK_START_REMINDER_NOTIFICATION -> QUICK_START_REMINDER_TYPE_VALUE
            POST_UPLOAD_SUCCESS -> POST_UPLOAD_SUCCESS_TYPE_VALUE
            POST_UPLOAD_ERROR -> POST_UPLOAD_ERROR_TYPE_VALUE
            MEDIA_UPLOAD_SUCCESS -> MEDIA_UPLOAD_SUCCESS_TYPE_VALUE
            MEDIA_UPLOAD_ERROR -> MEDIA_UPLOAD_ERROR_TYPE_VALUE
            POST_PUBLISHED_NOTIFICATION -> POST_PUBLISHED_TYPE_VALUE
            PENDING_DRAFTS_NOTIFICATION_ID -> PENDING_DRAFT_TYPE_VALUE
            ZENDESK_PUSH_NOTIFICATION_ID -> ZENDESK_MESSAGE_TYPE_VALUE
            null -> return
        }
        val properties = mapOf(NOTIFICATION_TYPE_KEY to notificationTypeValue)
        analyticsTracker.track(Stat.NOTIFICATION_DISMISSED, properties)
    }

    companion object {
        private const val NOTIFICATION_TYPE_KEY = "notification_type"
        private const val PUSH_NOTES_VALUE = "push_notes"
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
        private const val PENDING_DRAFT_TYPE_VALUE = "pending_draft"
        private const val ZENDESK_MESSAGE_TYPE_VALUE = "zendesk_message"
    }
}
