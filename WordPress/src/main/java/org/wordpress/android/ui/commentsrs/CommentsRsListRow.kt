package org.wordpress.android.ui.commentsrs

/** A rendered row in the rs comment list: either a date-group header or a comment. */
sealed interface CommentsRsListRow {
    /** [key] is a guaranteed-unique LazyColumn key; [label] is the (date) text shown to the user. */
    data class DateHeader(val label: String, val key: String) : CommentsRsListRow
    data class Item(val comment: CommentRsUiModel) : CommentsRsListRow
}

/**
 * Interleaves date subheaders into [comments] (already in display order), mirroring the legacy
 * list: a header before the first comment and before every comment whose date label differs from
 * the previous one. The label is the row's own [CommentRsUiModel.relativeDate] — the same
 * javaDateToTimeSpan value the legacy list groups by — so no extra date handling is needed here.
 *
 * Each header carries a unique [CommentsRsListRow.DateHeader.key] for the LazyColumn. Comments are
 * date-sorted, so a label normally maps to one contiguous group and the key is just the label —
 * which keeps the header stable when a newer comment is prepended into an existing group. Should
 * the list ever arrive out of date order, a repeated label is disambiguated rather than emitting a
 * duplicate key (which LazyColumn rejects with a hard crash, unlike the legacy RecyclerView).
 */
fun withDateHeaders(comments: List<CommentRsUiModel>): List<CommentsRsListRow> {
    val rows = ArrayList<CommentsRsListRow>(comments.size + 1)
    val usedKeys = HashSet<String>()
    var lastLabel: String? = null
    for (comment in comments) {
        if (comment.relativeDate != lastLabel) {
            rows.add(CommentsRsListRow.DateHeader(comment.relativeDate, uniqueKey(comment.relativeDate, usedKeys)))
            lastLabel = comment.relativeDate
        }
        rows.add(CommentsRsListRow.Item(comment))
    }
    return rows
}

/** A stable per-label key, suffixed only if the same label recurs non-contiguously (see above). */
private fun uniqueKey(label: String, used: MutableSet<String>): String {
    val base = "header_$label"
    if (used.add(base)) return base
    var n = 1
    while (!used.add("$base#$n")) n++
    return "$base#$n"
}
