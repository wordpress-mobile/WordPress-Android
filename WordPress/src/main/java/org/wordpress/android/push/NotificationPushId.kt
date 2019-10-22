package org.wordpress.android.push

enum class NotificationPushId(val value: Int) {
    PUSH_NOTIFICATION_ID(10000),
    AUTH_PUSH_NOTIFICATION_ID(20000),
    GROUP_NOTIFICATION_ID(30000),
    ACTIONS_RESULT_NOTIFICATION_ID(40000),
    ACTIONS_PROGRESS_NOTIFICATION_ID(50000),
    PENDING_DRAFTS_NOTIFICATION_ID(600001),
    QUICK_START_REMINDER_NOTIFICATION(4001),
    POST_UPLOAD_SUCCESS(5000),
    POST_UPLOAD_ERROR(5100),
    MEDIA_UPLOAD_SUCCESS(5200),
    MEDIA_UPLOAD_ERROR(5300),
    POST_PUBLISHED_NOTIFICATION(5400),
    ZENDESK_PUSH_NOTIFICATION_ID(1999999999);

    companion object {
        fun fromValue(value: Int): NotificationPushId? {
            return values().find { it.value == value }
        }
    }
}
