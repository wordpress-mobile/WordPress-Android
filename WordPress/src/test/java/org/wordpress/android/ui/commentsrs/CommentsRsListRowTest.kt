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
            DateHeader("Today", keyId = 1),
            Item(comment)
        )
    }

    @Test
    fun `consecutive comments with the same date share one header`() {
        val a = comment(id = 1, date = "Today")
        val b = comment(id = 2, date = "Today")

        assertThat(withDateHeaders(listOf(a, b))).containsExactly(
            DateHeader("Today", keyId = 1),
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
            DateHeader("Today", keyId = 1),
            Item(a),
            Item(b),
            DateHeader("Yesterday", keyId = 3),
            Item(c),
            DateHeader("January 8", keyId = 4),
            Item(d)
        )
    }

    @Test
    fun `each header is keyed by the first comment in its group`() {
        val rows = withDateHeaders(
            listOf(
                comment(id = 10, date = "Today"),
                comment(id = 11, date = "Yesterday")
            )
        )

        assertThat(rows.filterIsInstance<DateHeader>().map { it.keyId }).containsExactly(10, 11)
    }

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
