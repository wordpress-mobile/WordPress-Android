package org.wordpress.android.ui.comments.viewmodels

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.CommentStatus.APPROVED
import org.wordpress.android.fluxc.model.CommentStatus.SPAM
import org.wordpress.android.fluxc.model.CommentStatus.TRASH
import org.wordpress.android.fluxc.model.CommentStatus.UNAPPROVED
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.persistence.comments.CommentsDao.CommentEntity
import org.wordpress.android.fluxc.store.CommentStore.CommentError
import org.wordpress.android.fluxc.store.CommentStore.CommentErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.CommentsStore
import org.wordpress.android.fluxc.store.CommentsStore.CommentsActionPayload
import org.wordpress.android.fluxc.store.CommentsStore.CommentsData.CommentsActionData
import org.wordpress.android.models.usecases.LocalCommentCacheUpdateHandler
import org.wordpress.android.ui.comments.unified.CommentDetailsActionEvent
import org.wordpress.android.ui.comments.unified.CommentDetailsActionEvent.Close
import org.wordpress.android.ui.comments.unified.CommentDetailsActionEvent.LaunchEditComment
import org.wordpress.android.ui.comments.unified.CommentDetailsActionEvent.ReplySent
import org.wordpress.android.ui.comments.unified.CommentIdentifier.SiteCommentIdentifier
import org.wordpress.android.ui.comments.unified.UnifiedCommentDetailsViewModel
import org.wordpress.android.ui.comments.unified.UnifiedCommentDetailsViewModel.CommentDetailsUiState
import org.wordpress.android.ui.comments.unified.usecase.GetCommentUseCase
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.util.NetworkUtilsWrapper

@ExperimentalCoroutinesApi
class UnifiedCommentDetailsViewModelTest : BaseUnitTest() {
    @Mock
    lateinit var commentsStore: CommentsStore

    @Mock
    lateinit var getCommentUseCase: GetCommentUseCase

    @Mock
    lateinit var localCommentCacheUpdateHandler: LocalCommentCacheUpdateHandler

    @Mock
    lateinit var networkUtilsWrapper: NetworkUtilsWrapper

    private lateinit var viewModel: UnifiedCommentDetailsViewModel

    private val uiStates = mutableListOf<CommentDetailsUiState>()
    private val uiActionEvents = mutableListOf<CommentDetailsActionEvent>()
    private val snackbarMessages = mutableListOf<SnackbarMessageHolder>()

    private val site = SiteModel().apply {
        id = LOCAL_SITE_ID
        siteId = REMOTE_SITE_ID
    }

    @Before
    fun setup() = test {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
        whenever(getCommentUseCase.execute(site, REMOTE_COMMENT_ID)).thenReturn(COMMENT_ENTITY)
        whenever(commentsStore.moderateCommentLocally(eq(site), eq(REMOTE_COMMENT_ID), any()))
            .thenReturn(successPayload())
        whenever(commentsStore.pushLocalCommentByRemoteId(site, REMOTE_COMMENT_ID))
            .thenReturn(successPayload())
        whenever(commentsStore.likeComment(eq(site), eq(REMOTE_COMMENT_ID), eq(null), any()))
            .thenReturn(successPayload())
        whenever(commentsStore.createNewReply(eq(site), any(), any()))
            .thenReturn(successPayload())

        viewModel = UnifiedCommentDetailsViewModel(
            mainDispatcher = testDispatcher(),
            bgDispatcher = testDispatcher(),
            commentsStore = commentsStore,
            getCommentUseCase = getCommentUseCase,
            localCommentCacheUpdateHandler = localCommentCacheUpdateHandler,
            networkUtilsWrapper = networkUtilsWrapper
        )

        setupObservers()
    }

    @Test
    fun `start loads comment and populates ui state`() = test {
        viewModel.start(site, REMOTE_COMMENT_ID)

        val state = uiStates.last()
        assertThat(state.contentVisible).isTrue
        assertThat(state.authorName).isEqualTo("authorName")
        assertThat(state.commentText).isEqualTo("content")
        assertThat(state.status).isEqualTo(APPROVED)
    }

    @Test
    fun `start shows error snackbar when comment cannot be loaded`() = test {
        whenever(getCommentUseCase.execute(site, REMOTE_COMMENT_ID)).thenReturn(null)

        viewModel.start(site, REMOTE_COMMENT_ID)

        assertThat(snackbarMessages.firstOrNull()).isNotNull
    }

    @Test
    fun `onApproveClicked toggles approved comment to unapproved`() = test {
        viewModel.start(site, REMOTE_COMMENT_ID)

        viewModel.onApproveClicked()

        verify(commentsStore).moderateCommentLocally(site, REMOTE_COMMENT_ID, UNAPPROVED)
        verify(commentsStore).pushLocalCommentByRemoteId(site, REMOTE_COMMENT_ID)
        verify(localCommentCacheUpdateHandler, atLeastOnce()).requestCommentsUpdate()
        assertThat(uiStates.last().status).isEqualTo(UNAPPROVED)
    }

    @Test
    fun `onSpamClicked marks comment as spam and closes screen`() = test {
        viewModel.start(site, REMOTE_COMMENT_ID)

        viewModel.onSpamClicked()

        verify(commentsStore).moderateCommentLocally(site, REMOTE_COMMENT_ID, SPAM)
        assertThat(uiActionEvents).contains(Close)
    }

    @Test
    fun `onTrashClicked trashes comment and closes screen`() = test {
        viewModel.start(site, REMOTE_COMMENT_ID)

        viewModel.onTrashClicked()

        verify(commentsStore).moderateCommentLocally(site, REMOTE_COMMENT_ID, TRASH)
        assertThat(uiActionEvents).contains(Close)
    }

    @Test
    fun `moderation reverts status and shows snackbar on error`() = test {
        whenever(commentsStore.pushLocalCommentByRemoteId(site, REMOTE_COMMENT_ID))
            .thenReturn(errorPayload())
        viewModel.start(site, REMOTE_COMMENT_ID)

        viewModel.onApproveClicked()

        assertThat(snackbarMessages).isNotEmpty
        assertThat(uiStates.last().status).isEqualTo(APPROVED)
    }

    @Test
    fun `moderation shows snackbar and does not call store when offline`() = test {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)
        viewModel.start(site, REMOTE_COMMENT_ID)

        viewModel.onApproveClicked()

        verify(commentsStore, times(0)).pushLocalCommentByRemoteId(site, REMOTE_COMMENT_ID)
        assertThat(snackbarMessages).isNotEmpty
    }

    @Test
    fun `onLikeClicked likes comment and updates state`() = test {
        viewModel.start(site, REMOTE_COMMENT_ID)

        viewModel.onLikeClicked()

        verify(commentsStore).likeComment(site, REMOTE_COMMENT_ID, null, true)
        assertThat(uiStates.last().isLiked).isTrue
    }

    @Test
    fun `onEditClicked emits launch edit event with site comment identifier`() = test {
        viewModel.start(site, REMOTE_COMMENT_ID)

        viewModel.onEditClicked()

        val event = uiActionEvents.last()
        assertThat(event).isInstanceOf(LaunchEditComment::class.java)
        assertThat((event as LaunchEditComment).commentIdentifier)
            .isEqualTo(SiteCommentIdentifier(LOCAL_COMMENT_ID, REMOTE_COMMENT_ID))
    }

    @Test
    fun `onReplyClicked creates reply and emits reply sent event`() = test {
        viewModel.start(site, REMOTE_COMMENT_ID)

        viewModel.onReplyClicked("nice post")

        verify(commentsStore).createNewReply(eq(site), any(), any())
        verify(localCommentCacheUpdateHandler, atLeastOnce()).requestCommentsUpdate()
        assertThat(uiActionEvents).contains(ReplySent)
    }

    @Test
    fun `onReplyClicked does nothing for blank text`() = test {
        viewModel.start(site, REMOTE_COMMENT_ID)

        viewModel.onReplyClicked("   ")

        verify(commentsStore, times(0)).createNewReply(eq(site), any(), any())
    }

    @Test
    fun `onReplyClicked shows snackbar and does not call store when offline`() = test {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)
        viewModel.start(site, REMOTE_COMMENT_ID)

        viewModel.onReplyClicked("nice post")

        verify(commentsStore, times(0)).createNewReply(eq(site), any(), any())
        assertThat(snackbarMessages).isNotEmpty
    }

    @Test
    fun `onReplyClicked shows snackbar and no reply sent event on error`() = test {
        whenever(commentsStore.createNewReply(eq(site), any(), any())).thenReturn(errorPayload())
        viewModel.start(site, REMOTE_COMMENT_ID)

        viewModel.onReplyClicked("nice post")

        assertThat(snackbarMessages).isNotEmpty
        assertThat(uiActionEvents).doesNotContain(ReplySent)
    }

    private fun setupObservers() {
        uiStates.clear()
        uiActionEvents.clear()
        snackbarMessages.clear()

        viewModel.uiState.observeForever { uiStates.add(it) }
        viewModel.uiActionEvent.observeForever { it.applyIfNotHandled { uiActionEvents.add(this) } }
        viewModel.onSnackbarMessage.observeForever { it.applyIfNotHandled { snackbarMessages.add(this) } }
    }

    private fun successPayload() = CommentsActionPayload(CommentsActionData(emptyList(), 0))

    private fun errorPayload() = CommentsActionPayload<CommentsActionData>(CommentError(GENERIC_ERROR, "error"))

    companion object {
        private const val LOCAL_SITE_ID = 123
        private const val REMOTE_SITE_ID = 456L
        private const val LOCAL_COMMENT_ID = 1000
        private const val REMOTE_COMMENT_ID = 4321L

        private val COMMENT_ENTITY = CommentEntity(
            id = LOCAL_COMMENT_ID.toLong(),
            remoteCommentId = REMOTE_COMMENT_ID,
            remotePostId = 0,
            authorId = 4,
            localSiteId = LOCAL_SITE_ID,
            remoteSiteId = REMOTE_SITE_ID,
            authorUrl = "authorUrl",
            authorName = "authorName",
            authorEmail = "authorEmail",
            authorProfileImageUrl = null,
            postTitle = "postTitle",
            status = "approved",
            datePublished = null,
            publishedTimestamp = 0,
            content = "content",
            url = null,
            hasParent = false,
            parentId = 0,
            iLike = false
        )
    }
}
