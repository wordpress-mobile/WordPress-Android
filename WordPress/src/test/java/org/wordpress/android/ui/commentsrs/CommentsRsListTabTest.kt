package org.wordpress.android.ui.commentsrs

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import uniffi.wp_api.CommentStatus as RsCommentStatus

class CommentsRsListTabTest {
    @Test
    fun `all tab queries status all which the server treats as approved plus hold`() {
        assertThat(CommentsRsListTab.ALL.queryStatus).isEqualTo(RsCommentStatus.Custom("all"))
    }

    @Test
    fun `approved tab queries the literal approve status`() {
        // WP_Comment_Query only recognises "approve"; the RsCommentStatus.Approved enum
        // serialises to "approved", which the server treats as unknown and returns nothing for.
        assertThat(CommentsRsListTab.APPROVED.queryStatus).isEqualTo(RsCommentStatus.Custom("approve"))
    }

    @Test
    fun `remaining tabs use the built-in statuses`() {
        assertThat(CommentsRsListTab.PENDING.queryStatus).isEqualTo(RsCommentStatus.Hold)
        assertThat(CommentsRsListTab.SPAM.queryStatus).isEqualTo(RsCommentStatus.Spam)
        assertThat(CommentsRsListTab.TRASHED.queryStatus).isEqualTo(RsCommentStatus.Trash)
    }
}
