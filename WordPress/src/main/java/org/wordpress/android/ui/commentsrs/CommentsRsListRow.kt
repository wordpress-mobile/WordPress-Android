package org.wordpress.android.ui.commentsrs

/** A rendered row in the rs comment list: either a date-group header or a comment. */
sealed interface CommentsRsListRow {
    /** A date subheader. [keyId] is the first comment id in the group, for a stable LazyColumn key. */
    data class DateHeader(val label: String, val keyId: Long) : CommentsRsListRow
    data class Item(val comment: CommentRsUiModel) : CommentsRsListRow
}

/**
 * Interleaves date subheaders into [comments] (already in display order), mirroring the legacy
 * list: a header before the first comment and before every comment whose date label differs from
 * the previous one. The label is the row's own [CommentRsUiModel.relativeDate] — the same
 * javaDateToTimeSpan value the legacy list groups by — so no extra date handling is needed here.
 */
fun withDateHeaders(comments: List<CommentRsUiModel>): List<CommentsRsListRow> {
    val rows = ArrayList<CommentsRsListRow>(comments.size + 1)
    var lastLabel: String? = null
    for (comment in comments) {
        if (comment.relativeDate != lastLabel) {
            rows.add(CommentsRsListRow.DateHeader(comment.relativeDate, comment.remoteCommentId))
            lastLabel = comment.relativeDate
        }
        rows.add(CommentsRsListRow.Item(comment))
    }
    return rows
}
