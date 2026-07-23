package org.wordpress.android.ui.commentsrs

/** A rendered row in the rs comment list: either a date-group header or a comment. */
sealed interface CommentsRsListRow {
    data class DateHeader(val label: String) : CommentsRsListRow
    data class Item(val comment: CommentRsUiModel) : CommentsRsListRow
}

/**
 * Interleaves date subheaders into [comments] (already in display order), mirroring the legacy
 * list: a header before the first comment and before every comment whose date label differs from
 * the previous one. The label is the row's own [CommentRsUiModel.relativeDate] — the same
 * javaDateToTimeSpan value the legacy list groups by — so no extra date handling is needed here.
 *
 * The label doubles as the header's LazyColumn key: comments are date-sorted, so a label maps to
 * exactly one contiguous group (unique key), and keying by the label rather than the group's first
 * comment keeps the header stable when a newer comment is prepended into an existing group.
 */
fun withDateHeaders(comments: List<CommentRsUiModel>): List<CommentsRsListRow> {
    val rows = ArrayList<CommentsRsListRow>(comments.size + 1)
    var lastLabel: String? = null
    for (comment in comments) {
        if (comment.relativeDate != lastLabel) {
            rows.add(CommentsRsListRow.DateHeader(comment.relativeDate))
            lastLabel = comment.relativeDate
        }
        rows.add(CommentsRsListRow.Item(comment))
    }
    return rows
}
