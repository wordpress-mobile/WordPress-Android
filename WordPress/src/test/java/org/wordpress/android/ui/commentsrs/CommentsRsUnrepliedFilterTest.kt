package org.wordpress.android.ui.commentsrs

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.fluxc.model.CommentStatus
import org.wordpress.android.ui.comments.unified.CommentsRsDataSource.RsComment
import java.util.Date

class CommentsRsUnrepliedFilterTest {
    @Test
    fun `keeps a top-level comment by someone else with no replies`() {
        val comment = comment(id = 1, authorId = OTHER_USER)

        assertThat(filterUnreplied(listOf(comment), MY_USER)).containsExactly(comment)
    }

    @Test
    fun `keeps a top-level comment whose only reply is by another user`() {
        val topLevel = comment(id = 1, authorId = OTHER_USER)
        val otherReply = comment(id = 2, authorId = OTHER_USER, parentId = 1)

        assertThat(filterUnreplied(listOf(topLevel, otherReply), MY_USER)).containsExactly(topLevel)
    }

    @Test
    fun `drops a top-level comment once the current user has replied to it`() {
        val topLevel = comment(id = 1, authorId = OTHER_USER)
        val myReply = comment(id = 2, authorId = MY_USER, parentId = 1)

        assertThat(filterUnreplied(listOf(topLevel, myReply), MY_USER)).isEmpty()
    }

    @Test
    fun `drops a top-level comment authored by the current user`() {
        val mine = comment(id = 1, authorId = MY_USER)

        assertThat(filterUnreplied(listOf(mine), MY_USER)).isEmpty()
    }

    @Test
    fun `ignores replies themselves as candidates`() {
        // A reply (parentId != 0) is never a candidate, even when nobody has answered it.
        val reply = comment(id = 2, authorId = OTHER_USER, parentId = 99)

        assertThat(filterUnreplied(listOf(reply), MY_USER)).isEmpty()
    }

    @Test
    fun `matches a reply against a parent fetched on an earlier page`() {
        // The parent and the current user's reply can arrive on different pages; threading over the
        // accumulated set still drops the parent.
        val parent = comment(id = 1, authorId = OTHER_USER)
        val laterMyReply = comment(id = 50, authorId = MY_USER, parentId = 1)

        assertThat(filterUnreplied(listOf(parent, laterMyReply), MY_USER)).isEmpty()
    }

    @Test
    fun `over-reports rather than hiding when the current user id is unknown`() {
        // With a null id nothing counts as "mine", so a comment the user actually replied to still
        // shows — the same fail-open behaviour the legacy util has when it can't resolve the email.
        val topLevel = comment(id = 1, authorId = OTHER_USER)
        val myReply = comment(id = 2, authorId = MY_USER, parentId = 1)

        assertThat(filterUnreplied(listOf(topLevel, myReply), myUserId = null)).containsExactly(topLevel)
    }

    private fun comment(id: Long, authorId: Long, parentId: Long = 0) = RsComment(
        remoteCommentId = id,
        authorId = authorId,
        parentId = parentId,
        authorName = "Author $id",
        authorAvatarUrl = "",
        dateGmt = Date(0),
        contentHtml = "content",
        url = "",
        postId = 99L,
        status = CommentStatus.APPROVED
    )

    companion object {
        private const val MY_USER = 7L
        private const val OTHER_USER = 42L
    }
}
