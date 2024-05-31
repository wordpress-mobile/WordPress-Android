@file:Suppress("DEPRECATION")

package org.wordpress.android.ui.comments

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.isA
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.CommentModel
import org.wordpress.android.fluxc.model.CommentStatus
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.comments.CommentsMapper
import org.wordpress.android.fluxc.persistence.comments.CommentsDao
import org.wordpress.android.fluxc.store.CommentsStore
import org.wordpress.android.models.Note
import org.wordpress.android.ui.comments.unified.CommentsStoreAdapter
import org.wordpress.android.ui.notifications.NotificationEvents
import org.wordpress.android.util.EventBusWrapper

@ExperimentalCoroutinesApi
class CommentDetailViewModelTest : BaseUnitTest() {
    private val commentsStore: CommentsStore = mock()
    private val commentsStoreAdapter: CommentsStoreAdapter = mock()
    private val eventBusWrapper: EventBusWrapper = mock()
    private val commentsMapper: CommentsMapper = mock()
    private lateinit var viewModel: CommentDetailViewModel

    @Before
    fun setup() {
        viewModel = CommentDetailViewModel(
            testDispatcher(),
            commentsStore,
            commentsStoreAdapter,
            eventBusWrapper,
            commentsMapper
        )
    }

    @Test
    fun `when like a comment from comment list, then update comment`() {
        // Given
        val comment: CommentModel = mock()
        val site: SiteModel = mock()

        whenever(comment.iLike).thenReturn(false)

        // When
        viewModel.likeComment(comment, site)

        // Then
        verify(comment).setILike(true)
        verify(commentsStoreAdapter).dispatch(any())
        assert(viewModel.updatedComment.value == comment)
    }

    @Test
    fun `when like a comment from a notification, then update comment and notification`() {
        // Given
        val comment: CommentModel = mock()
        val site: SiteModel = mock()
        val note: Note = mock()

        whenever(comment.iLike).thenReturn(false)

        // When
        viewModel.likeComment(comment, site, note)

        // Then
        verify(comment).setILike(true)
        verify(commentsStoreAdapter).dispatch(any())
        verify(eventBusWrapper).postSticky(isA<NotificationEvents.OnNoteCommentLikeChanged>())
        assert(viewModel.updatedComment.value == comment)
    }

    @Test
    fun `when dispatch moderation action, then update comment`() {
        // Given
        val comment: CommentModel = mock()
        val site: SiteModel = mock()

        // When
        viewModel.dispatchModerationAction(site, comment, CommentStatus.APPROVED)

        // Then
        verify(commentsStoreAdapter).dispatch(any())
        verify(comment).setStatus(CommentStatus.APPROVED.toString())
        assert(viewModel.updatedComment.value == comment)
    }

    @Test
    fun `when comment fetched, then update comment`() = test {
        // Given
        val commentId = 123L
        val site: SiteModel = mock()
        val comment:CommentModel = mock()
        val commentEntity: CommentsDao.CommentEntity = mock()
        val result: CommentsStore.CommentsActionPayload<CommentsStore.CommentsData.CommentsActionData> = mock()

        whenever(result.data).thenReturn(mock())
        whenever(result.data?.comments).thenReturn(listOf(commentEntity))
        whenever(commentsStore.fetchComment(any(), any(), eq(null))).thenReturn(result)
        whenever(commentsMapper.commentEntityToLegacyModel(commentEntity)).thenReturn(comment)

        // When
        viewModel.fetchComment(site, commentId)

        // Then
        verify(commentsStore).fetchComment(site, commentId, null)
        assert(viewModel.updatedComment.value == comment)
    }
}
