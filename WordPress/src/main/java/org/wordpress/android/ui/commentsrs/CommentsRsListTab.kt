package org.wordpress.android.ui.commentsrs

import androidx.annotation.StringRes
import org.wordpress.android.R
import uniffi.wp_api.CommentStatus as RsCommentStatus

/**
 * Filter tabs for the rs comments list, matching the legacy unified list.
 *
 * [queryStatus] is the `status` query param for `/wp/v2/comments`. Three tabs use
 * [RsCommentStatus.Custom] because `WP_Comment_Query` only recognises the literal values
 * `approve` and `all` — wordpress-rs serialises [RsCommentStatus.Approved] as `approved`,
 * which WordPress core treats as an unknown status and returns nothing for (wordpress-rs
 * `comments.rs` serialisation test asserts `status=approved`). `all` means approved+hold,
 * matching the legacy ALL filter (APPROVED+UNAPPROVED).
 *
 * [UNREPLIED] has no server status: it queries `all` (like [ALL]) and is threaded client-side by
 * [filterUnreplied] to keep only top-level comments the user hasn't replied to, mirroring legacy.
 */
enum class CommentsRsListTab(
    @StringRes val labelResId: Int,
    @StringRes val emptyMessageResId: Int,
    /** The `selected_filter` property value for COMMENT_FILTER_CHANGED, matching the legacy list. */
    @StringRes val trackingLabelResId: Int,
    val queryStatus: RsCommentStatus
) {
    ALL(
        R.string.comment_status_all,
        R.string.comments_empty_list,
        R.string.comment_tracker_label_all,
        RsCommentStatus.Custom("all")
    ),
    PENDING(
        R.string.comment_status_unapproved,
        R.string.comments_empty_list_filtered_pending,
        R.string.comment_tracker_label_pending,
        RsCommentStatus.Hold
    ),
    UNREPLIED(
        R.string.comment_status_unreplied,
        R.string.comments_empty_list_filtered_unreplied,
        R.string.comment_tracker_label_unreplied,
        RsCommentStatus.Custom("all")
    ),
    APPROVED(
        R.string.comment_status_approved,
        R.string.comments_empty_list_filtered_approved,
        R.string.comment_tracker_label_approved,
        RsCommentStatus.Custom("approve")
    ),
    SPAM(
        R.string.comment_status_spam,
        R.string.comments_empty_list_filtered_spam,
        R.string.comment_tracker_label_spam,
        RsCommentStatus.Spam
    ),
    TRASHED(
        R.string.comment_status_trash,
        R.string.comments_empty_list_filtered_trashed,
        R.string.comment_tracker_label_trashed,
        RsCommentStatus.Trash
    )
}
