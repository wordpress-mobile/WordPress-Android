package org.wordpress.android.ui.comments.unified

import org.wordpress.android.R

enum class CommentFilter(val labelResId: Int) {
    ALL(R.string.comment_status_all),
    PENDING(R.string.comment_status_unapproved),
    APPROVED(R.string.comment_status_approved),
    UNREPLIED(R.string.comment_status_unreplied),
    TRASHED(R.string.comment_status_trash),
    SPAM(R.string.comment_status_spam),
    DELETE(R.string.comment_status_trash)
}
