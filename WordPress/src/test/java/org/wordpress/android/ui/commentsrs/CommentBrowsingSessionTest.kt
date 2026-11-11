package org.wordpress.android.ui.commentsrs

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.CommentStatus
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.comments.unified.CommentsRsDataSource
import org.wordpress.android.ui.comments.unified.CommentsRsDataSource.RsComment
import org.wordpress.android.ui.comments.unified.CommentsRsDataSource.RsCommentsPageResult
import uniffi.wp_api.CommentListParams
import java.util.Date

class CommentBrowsingSessionTest {
    private val dataSource: CommentsRsDataSource = mock()
    private lateinit var session: CommentBrowsingSession
    private val site = SiteModel().apply { id = 1 }

    @Before
    fun setUp() {
        session = CommentBrowsingSession(dataSource)
    }

    @Test
    fun `start seeds ids and activates the session`() {
        session.start(site, listOf(1L, 2L), FIRST_CURSOR)

        assertThat(session.commentIds.value).containsExactly(1L, 2L)
        assertThat(session.isActive).isTrue()
        assertThat(session.canLoadMore).isTrue()
    }

    @Test
    fun `a null cursor means there is no next page`() {
        session.start(site, listOf(1L), null)

        assertThat(session.isActive).isTrue()
        assertThat(session.canLoadMore).isFalse()
    }

    @Test
    fun `loadMore appends the next page's ids and advances the cursor`() = runTest {
        session.start(site, listOf(1L, 2L), FIRST_CURSOR)
        whenever(dataSource.fetchCommentsPage(site, FIRST_CURSOR))
            .thenReturn(RsCommentsPageResult.Success(listOf(rsComment(3), rsComment(4)), SECOND_CURSOR))

        val added = session.loadMore()

        assertThat(added).isTrue()
        assertThat(session.commentIds.value).containsExactly(1L, 2L, 3L, 4L)
        assertThat(session.canLoadMore).isTrue()
    }

    @Test
    fun `loadMore dedups ids already shown`() = runTest {
        session.start(site, listOf(1L, 2L), FIRST_CURSOR)
        whenever(dataSource.fetchCommentsPage(site, FIRST_CURSOR))
            .thenReturn(RsCommentsPageResult.Success(listOf(rsComment(2), rsComment(3)), null))

        session.loadMore()

        assertThat(session.commentIds.value).containsExactly(1L, 2L, 3L)
        assertThat(session.canLoadMore).isFalse()
    }

    @Test
    fun `loadMore is a no-op when there is no next page`() = runTest {
        session.start(site, listOf(1L), null)

        val added = session.loadMore()

        assertThat(added).isFalse()
        verify(dataSource, never()).fetchCommentsPage(any(), any())
    }

    @Test
    fun `loadMore auto-advances past a fully-deduped page`() = runTest {
        session.start(site, listOf(1L, 2L), FIRST_CURSOR)
        // The next page is entirely dupes (server-side shift) but a cursor remains; the page after
        // it holds the genuinely new comment.
        whenever(dataSource.fetchCommentsPage(site, FIRST_CURSOR))
            .thenReturn(RsCommentsPageResult.Success(listOf(rsComment(1), rsComment(2)), SECOND_CURSOR))
        whenever(dataSource.fetchCommentsPage(site, SECOND_CURSOR))
            .thenReturn(RsCommentsPageResult.Success(listOf(rsComment(3)), null))

        val added = session.loadMore()

        assertThat(added).isTrue()
        assertThat(session.commentIds.value).containsExactly(1L, 2L, 3L)
    }

    @Test
    fun `loadMore returns false on a fetch error and keeps the current ids`() = runTest {
        session.start(site, listOf(1L), FIRST_CURSOR)
        whenever(dataSource.fetchCommentsPage(site, FIRST_CURSOR)).thenReturn(RsCommentsPageResult.Error("boom"))

        assertThat(session.loadMore()).isFalse()
        assertThat(session.commentIds.value).containsExactly(1L)
    }

    @Test
    fun `clear deactivates the session`() {
        session.start(site, listOf(1L, 2L), FIRST_CURSOR)

        session.clear()

        assertThat(session.isActive).isFalse()
        assertThat(session.canLoadMore).isFalse()
        assertThat(session.commentIds.value).isEmpty()
    }

    private fun rsComment(id: Long) = RsComment(
        remoteCommentId = id,
        authorName = "Author $id",
        authorAvatarUrl = "",
        dateGmt = Date(0),
        contentHtml = "content",
        url = "",
        postId = 99L,
        status = CommentStatus.APPROVED
    )

    companion object {
        private val FIRST_CURSOR = CommentListParams(page = 2u)
        private val SECOND_CURSOR = CommentListParams(page = 3u)
    }
}
