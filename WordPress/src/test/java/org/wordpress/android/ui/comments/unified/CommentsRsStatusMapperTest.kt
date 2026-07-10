package org.wordpress.android.ui.comments.unified

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.fluxc.model.CommentStatus
import uniffi.wp_api.CommentStatus as RsCommentStatus

class CommentsRsStatusMapperTest {
    @Test
    fun `app status maps to the matching rs status`() {
        assertThat(CommentStatus.APPROVED.toRsCommentStatus()).isEqualTo(RsCommentStatus.Approved)
        assertThat(CommentStatus.UNAPPROVED.toRsCommentStatus()).isEqualTo(RsCommentStatus.Hold)
        assertThat(CommentStatus.SPAM.toRsCommentStatus()).isEqualTo(RsCommentStatus.Spam)
        assertThat(CommentStatus.TRASH.toRsCommentStatus()).isEqualTo(RsCommentStatus.Trash)
    }

    @Test
    fun `app statuses without an rs equivalent fall back to approved`() {
        assertThat(CommentStatus.ALL.toRsCommentStatus()).isEqualTo(RsCommentStatus.Approved)
        assertThat(CommentStatus.DELETED.toRsCommentStatus()).isEqualTo(RsCommentStatus.Approved)
        assertThat(CommentStatus.UNREPLIED.toRsCommentStatus()).isEqualTo(RsCommentStatus.Approved)
    }

    @Test
    fun `rs status maps to the matching app status`() {
        assertThat(RsCommentStatus.Approved.toAppCommentStatus()).isEqualTo(CommentStatus.APPROVED)
        assertThat(RsCommentStatus.Hold.toAppCommentStatus()).isEqualTo(CommentStatus.UNAPPROVED)
        assertThat(RsCommentStatus.Spam.toAppCommentStatus()).isEqualTo(CommentStatus.SPAM)
        assertThat(RsCommentStatus.Trash.toAppCommentStatus()).isEqualTo(CommentStatus.TRASH)
    }

    @Test
    fun `custom rs status falls back to all`() {
        assertThat(RsCommentStatus.Custom("something-else").toAppCommentStatus()).isEqualTo(CommentStatus.ALL)
    }
}
