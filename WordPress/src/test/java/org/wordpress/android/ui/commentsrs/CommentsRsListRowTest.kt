package org.wordpress.android.ui.commentsrs

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.fluxc.model.CommentStatus
import org.wordpress.android.ui.commentsrs.CommentsRsListRow.DateHeader
import org.wordpress.android.ui.commentsrs.CommentsRsListRow.Item

class CommentsRsListRowTest {
    @Test
    fun `empty list produces no rows`() {
        assertThat(withDateHeaders(emptyList())).isEmpty()
    }

    @Test
    fun `a single comment gets a leading date header`() {
        val comment = comment(id = 1, date = "Today")

        assertThat(withDateHeaders(listOf(comment))).containsExactly(
            header("Today"),
            Item(comment)
        )
    }

    @Test
    fun `consecutive comments with the same date share one header`() {
        val a = comment(id = 1, date = "Today")
        val b = comment(id = 2, date = "Today")

        assertThat(withDateHeaders(listOf(a, b))).containsExactly(
            header("Today"),
            Item(a),
            Item(b)
        )
    }

    @Test
    fun `a new header is inserted whenever the date label changes`() {
        val a = comment(id = 1, date = "Today")
        val b = comment(id = 2, date = "Today")
        val c = comment(id = 3, date = "Yesterday")
        val d = comment(id = 4, date = "January 8")

        assertThat(withDateHeaders(listOf(a, b, c, d))).containsExactly(
            header("Today"),
            Item(a),
            Item(b),
            header("Yesterday"),
            Item(c),
            header("January 8"),
            Item(d)
        )
    }

    @Test
    fun `a header stays identical when a newer comment is prepended into its group`() {
        // The header is keyed by its label, so adding a same-day comment at the top of the group
        // must not change the header's identity (which would make it re-animate on refresh).
        val before = withDateHeaders(listOf(comment(id = 1, date = "Today")))
        val after = withDateHeaders(listOf(comment(id = 2, date = "Today"), comment(id = 1, date = "Today")))

        val beforeHeader = before.filterIsInstance<DateHeader>().single()
        val afterHeader = after.filterIsInstance<DateHeader>().single()
        assertThat(afterHeader).isEqualTo(beforeHeader)
    }

    @Test
    fun `a label that recurs non-contiguously gets distinct header keys instead of crashing`() {
        // Defensive: comments are normally date-sorted so a label is one contiguous group, but if
        // the list ever arrives out of order two groups can share a label. LazyColumn rejects
        // duplicate keys with a crash, so each header must still get a unique key.
        val rows = withDateHeaders(
            listOf(
                comment(id = 1, date = "Today"),
                comment(id = 2, date = "Yesterday"),
                comment(id = 3, date = "Today")
            )
        )

        val headers = rows.filterIsInstance<DateHeader>()
        assertThat(headers.map { it.label }).containsExactly("Today", "Yesterday", "Today")
        assertThat(headers.map { it.key }).doesNotHaveDuplicates()
    }

    /** A header as it appears in the normal (contiguous) case: key derived directly from the label. */
    private fun header(label: String) = DateHeader(label, "header_$label")

    private fun comment(id: Long, date: String) = CommentRsUiModel(
        remoteCommentId = id,
        authorName = "Jane",
        avatarUrl = "",
        snippet = "hello",
        relativeDate = date,
        status = CommentStatus.APPROVED,
        postId = 99L
    )
}
