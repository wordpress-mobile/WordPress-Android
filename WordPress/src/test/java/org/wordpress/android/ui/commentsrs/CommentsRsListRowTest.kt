package org.wordpress.android.ui.commentsrs

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.fluxc.model.CommentStatus
import org.wordpress.android.ui.commentsrs.CommentsRsListRow.DateHeader
import org.wordpress.android.ui.commentsrs.CommentsRsListRow.GroupHeader
import org.wordpress.android.ui.commentsrs.CommentsRsListRow.Item
import org.wordpress.android.ui.rs.contentlist.ContentDateGroup

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

    @Test
    fun `date groups bucket comments from the same week under one header`() {
        val a = comment(id = 1, date = "Today", millis = now())
        val b = comment(id = 2, date = "Yesterday", millis = now() - DAY_MILLIS)

        val rows = withDateGroups(listOf(a, b))

        assertThat(rows).containsExactly(
            GroupHeader(ContentDateGroup.ThisWeek, "header_0_this_week"),
            Item(a),
            Item(b)
        )
    }

    @Test
    fun `date groups open a new header when the bucket changes`() {
        val recent = comment(id = 1, date = "Today", millis = now())
        val old = comment(id = 2, date = "March 3", millis = now() - (365L * DAY_MILLIS))

        val headers = withDateGroups(listOf(recent, old)).filterIsInstance<GroupHeader>()

        assertThat(headers).hasSize(2)
        assertThat(headers.first().group).isEqualTo(ContentDateGroup.ThisWeek)
        assertThat(headers.map { it.key }).doesNotHaveDuplicates()
    }

    @Test
    fun `a comment with no timestamp is kept but opens no bucket`() {
        // dateGmtMillis defaults to 0 for anything built before the raw date was carried; such a
        // row must still render rather than being dropped or filed under a wrong bucket.
        val undated = comment(id = 1, date = "Today", millis = 0L)

        val rows = withDateGroups(listOf(undated))

        assertThat(rows).containsExactly(Item(undated))
    }

    @Test
    fun `date group headers never share a key when a bucket reopens`() {
        // Defensive, as for the pre-redesign headers: a list that is not strictly date-ordered can
        // reopen a bucket, and a duplicate LazyColumn key is a hard crash.
        val rows = withDateGroups(
            listOf(
                comment(id = 1, date = "Today", millis = now()),
                comment(id = 2, date = "March 3", millis = now() - (365L * DAY_MILLIS)),
                comment(id = 3, date = "Today", millis = now())
            )
        )

        val headers = rows.filterIsInstance<GroupHeader>()
        assertThat(headers).hasSize(3)
        assertThat(headers.map { it.key }).doesNotHaveDuplicates()
    }

    @Test
    fun `only non-approved statuses carry a row badge`() {
        assertThat(comment(id = 1, date = "Today", status = CommentStatus.APPROVED).statusBadgeResId).isNull()
        assertThat(comment(id = 2, date = "Today", status = CommentStatus.UNAPPROVED).statusBadgeResId).isNotNull()
        assertThat(comment(id = 3, date = "Today", status = CommentStatus.SPAM).statusBadgeResId).isNotNull()
        assertThat(comment(id = 4, date = "Today", status = CommentStatus.TRASH).statusBadgeResId).isNotNull()
    }

    /** A header as it appears in the normal (contiguous) case: key derived directly from the label. */
    private fun header(label: String) = DateHeader(label, "header_$label")

    private fun now() = System.currentTimeMillis()

    private fun comment(
        id: Long,
        date: String,
        millis: Long = 0L,
        status: CommentStatus = CommentStatus.APPROVED
    ) = CommentRsUiModel(
        remoteCommentId = id,
        authorName = "Jane",
        avatarUrl = "",
        snippet = "hello",
        relativeDate = date,
        status = status,
        postId = 99L,
        dateGmtMillis = millis
    )

    companion object {
        private const val DAY_MILLIS = 24L * 60L * 60L * 1000L
    }
}
