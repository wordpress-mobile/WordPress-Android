package org.wordpress.android.ui.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.model.CommentStatus
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.models.ReaderComment
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.ui.reader.ReaderPostPagerActivity.DirectOperation
import org.wordpress.android.ui.reader.actions.ReaderActions.UpdateResult
import org.wordpress.android.ui.reader.repository.ReaderCommentRepository
import org.wordpress.android.ui.reader.repository.ReaderComments
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.analytics.AnalyticsUtils.AnalyticsCommentActionSource
import org.wordpress.android.util.analytics.AnalyticsUtilsWrapper
import org.wordpress.android.util.config.ReaderCommentsModerationFeatureConfig

@ExperimentalCoroutinesApi
class ReaderCommentListViewModelTest : BaseUnitTest() {
    @Mock lateinit var repository: ReaderCommentRepository
    @Mock lateinit var accountStore: AccountStore
    @Mock lateinit var moderationFeatureConfig: ReaderCommentsModerationFeatureConfig
    @Mock lateinit var networkUtilsWrapper: NetworkUtilsWrapper
    @Mock lateinit var readerTracker: ReaderTracker
    @Mock lateinit var analyticsUtilsWrapper: AnalyticsUtilsWrapper

    private lateinit var viewModel: ReaderCommentListViewModel

    private val post = ReaderPost().apply {
        blogId = BLOG_ID
        postId = POST_ID
        isCommentsOpen = true
    }
    private val first = comment(1)
    private val second = comment(2)

    @Before
    fun setUp() {
        runBlocking {
            val account = mock<AccountModel>()
            whenever(account.userId).thenReturn(USER_ID)
            whenever(accountStore.account).thenReturn(account)
            whenever(accountStore.hasAccessToken()).thenReturn(true)
            whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
            whenever(repository.getPost(BLOG_ID, POST_ID)).thenReturn(post)
            whenever(repository.getSuggestions(BLOG_ID)).thenReturn(emptyList())
            whenever(repository.getComments(post)).thenReturn(ReaderComments(listOf(first, second), hasMore = false))
            whenever(repository.fetchComments(eq(BLOG_ID), eq(POST_ID), any())).thenReturn(UpdateResult.UNCHANGED)
        }
    }

    @After
    fun tearDown() {
        if (::viewModel.isInitialized) viewModel.viewModelScope.cancel()
    }

    @Test
    fun `opening shows the stored comments, fetches the first page and tracks the source`() = test {
        viewModel = createViewModel(source = "notification")

        assertThat(viewModel.uiState.value.comments.map { it.commentId }).containsExactly(1L, 2L)
        verify(repository).fetchComments(BLOG_ID, POST_ID, false)
        verify(readerTracker).trackPost(Stat.READER_ARTICLE_COMMENTS_OPENED, post, "notification")
    }

    @Test
    fun `offline with no comments shows the network message without fetching`() = test {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)
        whenever(repository.getComments(post)).thenReturn(ReaderComments(emptyList(), hasMore = false))
        viewModel = createViewModel()

        verify(repository, never()).fetchComments(any(), any(), any())
        assertThat(viewModel.uiState.value.emptyMessage).isEqualTo(R.string.no_network_message)
    }

    @Test
    fun `the next page is only fetched when the server has more`() = test {
        viewModel = createViewModel()
        viewModel.onLoadMore()
        verify(repository, never()).fetchComments(BLOG_ID, POST_ID, true)

        whenever(repository.getComments(post)).thenReturn(ReaderComments(listOf(first, second), hasMore = true))
        viewModel = createViewModel()
        viewModel.onLoadMore()
        verify(repository).fetchComments(BLOG_ID, POST_ID, true)
    }

    @Test
    fun `a comment jump highlights and scrolls to the comment after the first fetch`() = test {
        viewModel = createViewModel(directOperation = DirectOperation.COMMENT_JUMP, commentId = 2)

        val state = viewModel.uiState.value
        assertThat(state.scrollToCommentId).isEqualTo(2L)
        assertThat(state.comments.single { it.isHighlighted }.commentId).isEqualTo(2L)
    }

    @Test
    fun `the direct operation isn't repeated when the screen is restored`() = test {
        val savedStateHandle = savedState(directOperation = DirectOperation.COMMENT_JUMP, commentId = 2)
        createViewModel(savedStateHandle).viewModelScope.cancel()
        viewModel = createViewModel(savedStateHandle)

        assertThat(viewModel.uiState.value.scrollToCommentId).isNull()
    }

    @Test
    fun `a comment reply opens the sheet for that comment`() = test {
        viewModel = createViewModel(directOperation = DirectOperation.COMMENT_REPLY, commentId = 1)

        val state = viewModel.uiState.value
        assertThat(state.isReplySheetShown).isTrue
        assertThat(state.replyHint).isEqualTo(R.string.reader_hint_comment_on_comment)
        assertThat(state.comments.single { it.isReplyTarget }.commentId).isEqualTo(1L)
    }

    @Test
    fun `a comment like while logged out asks the user to sign in`() = test {
        whenever(accountStore.hasAccessToken()).thenReturn(false)
        viewModel = createViewModel(directOperation = DirectOperation.COMMENT_LIKE, commentId = 1)

        viewModel.events.test {
            assertThat(awaitItem()).isEqualTo(ReaderCommentListEvent.ShowSignInPrompt)
        }
        verify(repository, never()).setLikeLocally(any(), any(), any())
    }

    @Test
    fun `a posted reply replaces the pending comment`() = test {
        val pending = comment(PENDING_ID)
        whenever(repository.addPendingComment(post, "hello", 1, USER_ID)).thenReturn(pending)
        whenever(repository.submitComment(post, pending)).thenReturn(comment(3))
        viewModel = createViewModel()
        viewModel.onReplyClicked(1)

        viewModel.events.test {
            // sent on opening, since the post is on WP.com
            assertThat(awaitItem()).isEqualTo(ReaderCommentListEvent.BindSuggestionService)
            viewModel.onSendReply(" hello ")

            assertThat(awaitItem()).isEqualTo(ReaderCommentListEvent.ClearReply)
            expectNoEvents()
        }
        verify(readerTracker).trackPost(Stat.READER_ARTICLE_COMMENT_REPLIED_TO, post)
        val state = viewModel.uiState.value
        assertThat(state.isSubmitting).isFalse
        assertThat(state.comments.none { it.isPending }).isTrue
        assertThat(state.replyHint).isEqualTo(R.string.reader_hint_comment_on_post)
    }

    @Test
    fun `a reply that fails to post is restored`() = test {
        val pending = comment(PENDING_ID)
        whenever(repository.addPendingComment(post, "hello", 0, USER_ID)).thenReturn(pending)
        whenever(repository.submitComment(post, pending)).thenReturn(null)
        viewModel = createViewModel()

        viewModel.events.test {
            // sent on opening, since the post is on WP.com
            assertThat(awaitItem()).isEqualTo(ReaderCommentListEvent.BindSuggestionService)
            viewModel.onSendReply("hello")

            assertThat(awaitItem()).isEqualTo(ReaderCommentListEvent.ClearReply)
            assertThat(awaitItem()).isEqualTo(ReaderCommentListEvent.RestoreReply("hello"))
            assertThat(awaitItem()).isEqualTo(
                ReaderCommentListEvent.ShowToast(R.string.reader_toast_err_comment_failed, isLong = true)
            )
        }
        assertThat(viewModel.uiState.value.isSubmitting).isFalse
    }

    @Test
    fun `a moderated comment is hidden and only sent once its snackbar goes`() = test {
        whenever(repository.moderateComment(first, CommentStatus.SPAM)).thenReturn(true)
        viewModel = createViewModel()

        viewModel.onModerateClicked(1, ReaderCommentModeration.SPAM)
        assertThat(viewModel.uiState.value.comments.map { it.commentId }).containsExactly(2L)
        verify(repository, never()).moderateComment(any(), any())

        viewModel.onModerationUndoFinished(1, isUndone = false)
        verify(repository).moderateComment(first, CommentStatus.SPAM)
        verify(analyticsUtilsWrapper).trackCommentActionWithReaderPostDetails(
            Stat.COMMENT_SPAMMED,
            AnalyticsCommentActionSource.READER,
            post
        )
    }

    @Test
    fun `undoing a moderation shows the comment again without sending it`() = test {
        viewModel = createViewModel()

        viewModel.onModerateClicked(1, ReaderCommentModeration.TRASH)
        viewModel.onModerationUndoFinished(1, isUndone = true)

        assertThat(viewModel.uiState.value.comments.map { it.commentId }).containsExactly(1L, 2L)
        verify(repository, never()).moderateComment(any(), any())
    }

    private fun createViewModel(
        directOperation: DirectOperation? = null,
        commentId: Long = 0,
        source: String? = null
    ) = createViewModel(savedState(directOperation, commentId, source))

    private fun createViewModel(savedStateHandle: SavedStateHandle) = ReaderCommentListViewModel(
        savedStateHandle,
        repository,
        accountStore,
        moderationFeatureConfig,
        networkUtilsWrapper,
        readerTracker,
        analyticsUtilsWrapper,
        testScope(),
        testDispatcher()
    )

    private fun savedState(directOperation: DirectOperation? = null, commentId: Long = 0, source: String? = null) =
        SavedStateHandle(
            mapOf(
                ReaderConstants.ARG_BLOG_ID to BLOG_ID,
                ReaderConstants.ARG_POST_ID to POST_ID,
                ReaderConstants.ARG_DIRECT_OPERATION to directOperation,
                ReaderConstants.ARG_COMMENT_ID to commentId,
                ReaderConstants.ARG_SOURCE to source
            )
        )

    private fun comment(id: Long) = ReaderComment().apply {
        commentId = id
        blogId = BLOG_ID
        postId = POST_ID
    }

    companion object {
        private const val BLOG_ID = 10L
        private const val POST_ID = 20L
        private const val USER_ID = 30L
        private const val PENDING_ID = 99L
    }
}
