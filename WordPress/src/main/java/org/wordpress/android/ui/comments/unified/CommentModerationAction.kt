package org.wordpress.android.ui.comments.unified

/** Identifies which moderation button should show progress while its request is in flight. */
enum class CommentModerationAction {
    APPROVE,
    UNAPPROVE,
    SPAM,
    TRASH,
    RESTORE,
    DELETE
}
