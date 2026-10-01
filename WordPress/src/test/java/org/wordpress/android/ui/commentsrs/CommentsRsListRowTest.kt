package org.wordpress.android.ui.commentsrs

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.fluxc.model.CommentStatus
import org.wordpress.android.ui.commentsrs.CommentsRsListRow.GroupHeader
import org.wordpress.android.ui.commentsrs.CommentsRsListRow.Item
import org.wordpress.android.ui.rs.contentlist.ContentDateGroup
import java.util.Calendar

class CommentsRsListRowTest {
    @Test
    fun `empty list produces no rows`() {
        assertThat(withDateGroups(emptyList())).isEmpty()
    }

    @Test
    fun `comments from today share one Today header`() {
        val a = comment(id = 1, date = "4 minutes ago", millis = todayAt(hour = 14))
        val b = comment(id = 2, date = "2 hours ago", millis = todayAt(hour = 9))

        val rows = withDateGroups(listOf(a, b))

        assertThat(rows).containsExactly(
            GroupHeader(ContentDateGroup.Today, "header_0_today"),
            Item(a),
            Item(b)
        )
    }

    @Test
    fun `today and yesterday get separate headers`() {
        val today = comment(id = 1, date = "4 minutes ago", millis = todayAt(hour = 14))
        val yesterday = comment(id = 2, date = "Yesterday", millis = daysAgoAt(days = 1, hour = 14))

        val headers = withDateGroups(listOf(today, yesterday)).filterIsInstance<GroupHeader>()

        assertThat(headers.map { it.group })
            .containsExactly(ContentDateGroup.Today, ContentDateGroup.Yesterday)
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
        // Defensive: a list that is not strictly date-ordered can
        // reopen a bucket, and a duplicate LazyColumn key is a hard crash.
        val rows = withDateGroups(
            listOf(
                comment(id = 1, date = "Today", millis = todayAt(hour = 14)),
                comment(id = 2, date = "March 3", millis = daysAgoAt(days = 365, hour = 14)),
                comment(id = 3, date = "Today", millis = todayAt(hour = 14))
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

    /**
     * A timestamp at [hour] on today's date. Built from the calendar rather than offset from the
     * clock so the bucket a test expects does not depend on the time of day the suite runs at -
     * "two hours ago" is yesterday if you run it at 00:30.
     */
    private fun todayAt(hour: Int) = daysAgoAt(days = 0, hour = hour)

    private fun daysAgoAt(days: Int, hour: Int): Long = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, -days)
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

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
}
