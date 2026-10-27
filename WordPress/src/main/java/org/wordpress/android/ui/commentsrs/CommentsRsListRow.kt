package org.wordpress.android.ui.commentsrs

import org.wordpress.android.ui.rs.contentlist.ContentDateGroup
import org.wordpress.android.ui.rs.contentlist.ContentDateGrouper

/** A rendered row in the rs comment list: either a date-group header or a comment. */
sealed interface CommentsRsListRow {
    /**
     * The bucketed header ("This week", "Earlier in July"), shared with the
     * posts list. [key] carries an ordinal because a list that is not strictly date-ordered can
     * reopen a bucket, and a duplicate LazyColumn key is a hard crash.
     */
    data class GroupHeader(val group: ContentDateGroup, val key: String) : CommentsRsListRow

    data class Item(val comment: CommentRsUiModel) : CommentsRsListRow
}

/**
 * Interleaves [ContentDateGroup] headers into [comments], so comments
 * bucket the same way posts do rather than getting a header per distinct day.
 *
 * A comment with no usable timestamp (the field defaults to 0 for anything built before the raw
 * date was carried on the model) is emitted without opening a bucket, rather than being dropped
 * or filed under a wrong one.
 */
fun withDateGroups(comments: List<CommentRsUiModel>): List<CommentsRsListRow> {
    val rows = ArrayList<CommentsRsListRow>(comments.size + 1)
    var currentGroupKey: String? = null
    var headerCount = 0
    for (comment in comments) {
        if (comment.dateGmtMillis > 0L) {
            val group = ContentDateGrouper.groupOf(comment.dateGmtMillis)
            if (group.key != currentGroupKey) {
                rows.add(CommentsRsListRow.GroupHeader(group, "header_${headerCount++}_${group.key}"))
                currentGroupKey = group.key
            }
        }
        rows.add(CommentsRsListRow.Item(comment))
    }
    return rows
}
