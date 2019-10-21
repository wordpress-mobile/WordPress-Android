package org.wordpress.android.push

enum class NotificationActionType(val value: String) {
    ARG_ACTION_LIKE("action_like"),
    ARG_ACTION_REPLY("action_reply"),
    ARG_ACTION_APPROVE("action_approve"),
    ARG_ACTION_NOTIFICATION_DISMISS("action_dismiss"),
    ARG_ACTION_DRAFT_PENDING_DISMISS("action_draft_pending_dismiss"),
    ARG_ACTION_DRAFT_PENDING_IGNORE("action_draft_pending_ignore"),
    ARG_ACTION_AUTH_APPROVE("action_auth_aprove"),
    ARG_ACTION_AUTH_IGNORE("action_auth_ignore"),
}
