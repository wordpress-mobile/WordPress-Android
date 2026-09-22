package org.wordpress.android.ui.comments.unified

/**
 * A moderation action the comment detail can be performing.
 *
 * Exists so the screen can show progress against the specific button that was tapped, rather than
 * a screen-wide spinner that leaves the user guessing which action is in flight.
 */
enum class CommentModerationAction {
    APPROVE,
    UNAPPROVE,
    SPAM,
    TRASH,
    RESTORE,
    DELETE
}
