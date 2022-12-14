package org.wordpress.android.ui.comments.viewmodels

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.datasets.wrappers.ReaderCommentTableWrapper
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.persistence.comments.CommentsDao.CommentEntity
import org.wordpress.android.fluxc.store.CommentStore.CommentError
import org.wordpress.android.fluxc.store.CommentStore.CommentErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.CommentsStore
import org.wordpress.android.fluxc.store.CommentsStore.CommentsActionPayload
import org.wordpress.android.fluxc.store.CommentsStore.CommentsData.CommentsActionData
import org.wordpress.android.models.ReaderComment
import org.wordpress.android.models.usecases.LocalCommentCacheUpdateHandler
import org.wordpress.android.ui.comments.unified.CommentEssentials
import org.wordpress.android.ui.comments.unified.CommentIdentifier.NotificationCommentIdentifier
import org.wordpress.android.ui.comments.unified.CommentIdentifier.ReaderCommentIdentifier
import org.wordpress.android.ui.comments.unified.CommentIdentifier.SiteCommentIdentifier
import org.wordpress.android.ui.comments.unified.UnifiedCommentsEditViewModel
import org.wordpress.android.ui.comments.unified.UnifiedCommentsEditViewModel.EditCommentActionEvent
import org.wordpress.android.ui.comments.unified.UnifiedCommentsEditViewModel.EditCommentActionEvent.CANCEL_EDIT_CONFIRM
import org.wordpress.android.ui.comments.unified.UnifiedCommentsEditViewModel.EditCommentActionEvent.CLOSE
import org.wordpress.android.ui.comments.unified.UnifiedCommentsEditViewModel.EditCommentActionEvent.DONE
import org.wordpress.android.ui.comments.unified.UnifiedCommentsEditViewModel.EditCommentUiState
import org.wordpress.android.ui.comments.unified.UnifiedCommentsEditViewModel.FieldType
import org.wordpress.android.ui.comments.unified.UnifiedCommentsEditViewModel.FieldType.USER_EMAIL
import org.wordpress.android.ui.comments.unified.usecase.GetCommentUseCase
import org.wordpress.android.ui.notifications.utils.NotificationsActionsWrapper
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.analytics.AnalyticsUtils.AnalyticsCommentActionSource
import org.wordpress.android.util.analytics.AnalyticsUtilsWrapper
import org.wordpress.android.viewmodel.ResourceProvider

@ExperimentalCoroutinesApi
class UnifiedCommentsEditViewModelTest : BaseUnitTest() {
    @Mock lateinit var commentsStore: CommentsStore
    @Mock lateinit var resourceProvider: ResourceProvider
    @Mock lateinit var networkUtilsWrapper: NetworkUtilsWrapper
    @Mock private lateinit var localCommentCacheUpdateHandler: LocalCommentCacheUpdateHandler
    @Mock lateinit var getCommentUseCase: GetCommentUseCase
    @Mock lateinit var notificationActionsWrapper: NotificationsActionsWrapper
    @Mock lateinit var readerCommentTableWrapper: ReaderCommentTableWrapper
    @Mock lateinit var analyticsUtilsWrapper: AnalyticsUtilsWrapper

    private lateinit var viewModel: UnifiedCommentsEditViewModel

    private var uiState: MutableList<EditCommentUiState> = mutableListOf()
    private var uiActionEvent: MutableList<EditCommentActionEvent> = mutableListOf()
    private var onSnackbarMessage: MutableList<SnackbarMessageHolder> = mutableListOf()

    private val site = SiteModel().apply {
        id = LOCAL_SITE_ID
        siteId = REMOTE_SITE_ID
    }

    private val localCommentId = 1000
    private val remoteCommentId = 4321L
    private val postId = 678L
    private val siteCommentIdentifier = SiteCommentIdentifier(localCommentId, remoteCommentId)
    private val noteId = "noteId"
    private val notificationCommentIdentifier = NotificationCommentIdentifier(noteId, remoteCommentId)
    private val readerCommentIdentifier = ReaderCommentIdentifier(REMOTE_SITE_ID, postId, remoteCommentId)

    @Before
    fun setup() = test {
        whenever(networkUtilsWrapper.isNetworkAvailable())
                .thenReturn(true)
        whenever(getCommentUseCase.execute(site, remoteCommentId))
                .thenReturn(COMMENT_ENTITY)

        whenever(readerCommentTableWrapper.getComment(REMOTE_SITE_ID, postId, remoteCommentId))
                .thenReturn(READER_COMMENT_ENTITY)

        viewModel = UnifiedCommentsEditViewModel(
                mainDispatcher = testDispatcher(),
                bgDispatcher = testDispatcher(),
                commentsStore = commentsStore,
                resourceProvider = resourceProvider,
                networkUtilsWrapper = networkUtilsWrapper,
                localCommentCacheUpdateHandler = localCommentCacheUpdateHandler,
                getCommentUseCase = getCommentUseCase,
                notificationActionsWrapper = notificationActionsWrapper,
                readerCommentTableWrapper = readerCommentTableWrapper,
                analyticsUtilsWrapper
        )

        setupObservers()
    }

    @Test
    fun `watchers are init on view recreation`() {
        viewModel.start(site, siteCommentIdentifier)

        viewModel.start(site, siteCommentIdentifier)

        assertThat(uiState.first().shouldInitWatchers).isFalse
        assertThat(uiState.last().shouldInitWatchers).isTrue
    }

    @Test
    fun `Should display error SnackBar if mapped CommentEssentials is NOT VALID`() = test {
        whenever(getCommentUseCase.execute(site, remoteCommentId))
                .thenReturn(null)
        viewModel.start(site, siteCommentIdentifier)
        assertThat(onSnackbarMessage.firstOrNull()).isNotNull
    }

    @Test
    fun `Should display correct SnackBar error message if mapped CommentEssentials is NOT VALID`() = test {
        whenever(getCommentUseCase.execute(site, remoteCommentId))
                .thenReturn(null)
        viewModel.start(site, siteCommentIdentifier)
        val expected = UiStringRes(R.string.error_load_comment)
        val actual = onSnackbarMessage.first().message
        assertEquals(expected, actual)
    }

    @Test
    fun `Should show and hide progress after start`() = test {
        viewModel.start(site, siteCommentIdentifier)

        assertThat(uiState[0].showProgress).isTrue
        assertThat(uiState[2].showProgress).isFalse
    }

    @Test
    fun `Should get comment from GetCommentUseCase`() = test {
        viewModel.start(site, siteCommentIdentifier)
        verify(getCommentUseCase).execute(site, remoteCommentId)
    }

    @Test
    fun `Should map CommentIdentifier to CommentEssentials`() = test {
        viewModel.start(site, siteCommentIdentifier)
        assertThat(uiState[1].editedComment).isEqualTo(COMMENT_ESSENTIALS)
    }

    @Test
    fun `Should map CommentIdentifier to default CommentEssentials if CommentIdentifier comment not found`() = test {
        whenever(getCommentUseCase.execute(site, remoteCommentId))
                .thenReturn(null)
        viewModel.start(site, siteCommentIdentifier)
        assertThat(uiState[1].editedComment).isEqualTo(CommentEssentials())
    }

    @Test
    fun `Should map CommentIdentifier to default CommentEssentials if CommentIdentifier not handled`() = test {
        // ReaderCommentIdentifier is not supported by this class yet
        viewModel.start(site, ReaderCommentIdentifier(0L, 0L, 0L))
        assertThat(uiState[1].editedComment).isEqualTo(CommentEssentials())
    }

    @Test
    fun `onActionMenuClicked triggers snackbar if no network`() = test {
        whenever(networkUtilsWrapper.isNetworkAvailable())
                .thenReturn(false)
        viewModel.onActionMenuClicked()
        assertThat(onSnackbarMessage.firstOrNull()).isNotNull
    }

    @Test
    fun `onActionMenuClicked triggers snackbar if comment update error`() = test {
        whenever(commentsStore.getCommentByLocalSiteAndRemoteId(site.id, remoteCommentId))
                .thenReturn(listOf(COMMENT_ENTITY))
        whenever(commentsStore.updateEditComment(eq(site), any()))
                .thenReturn(CommentsActionPayload(CommentError(GENERIC_ERROR, "error")))
        viewModel.start(site, siteCommentIdentifier)
        viewModel.onActionMenuClicked()
        assertThat(onSnackbarMessage.firstOrNull()).isNotNull
    }

    @Test
    fun `onActionMenuClicked triggers DONE action if comment update successfully`() = test {
        whenever(commentsStore.getCommentByLocalSiteAndRemoteId(site.id, remoteCommentId))
                .thenReturn(listOf(COMMENT_ENTITY))
        whenever(commentsStore.updateEditComment(eq(site), any()))
                .thenReturn(CommentsActionPayload(CommentsActionData(emptyList(), 0)))
        viewModel.start(site, siteCommentIdentifier)
        viewModel.onActionMenuClicked()
        assertThat(uiActionEvent.firstOrNull()).isEqualTo(DONE)
        verify(localCommentCacheUpdateHandler).requestCommentsUpdate()
    }

    @Test
    fun `onBackPressed triggers CLOSE when no edits`() {
        viewModel.start(site, siteCommentIdentifier)
        viewModel.onBackPressed()
        assertThat(uiActionEvent.firstOrNull()).isEqualTo(CLOSE)
    }

    @Test
    fun `onBackPressed triggers CANCEL_EDIT_CONFIRM when edits are present`() {
        val emailFieldType: FieldType = mock()
        whenever(emailFieldType.matches(USER_EMAIL))
                .thenReturn(true)
        whenever(emailFieldType.isValid)
                .thenReturn { true }

        viewModel.start(site, siteCommentIdentifier)
        viewModel.onValidateField("edited user email", emailFieldType)
        viewModel.onBackPressed()

        assertThat(uiActionEvent.firstOrNull()).isEqualTo(CANCEL_EDIT_CONFIRM)
    }

    @Test
    fun `onConfirmEditingDiscard triggers CLOSE`() {
        viewModel.onConfirmEditingDiscard()
        assertThat(uiActionEvent.firstOrNull()).isEqualTo(CLOSE)
    }

    @Test
    fun `Should DISABLE edit name for a comment from registered user`() = test {
        viewModel.start(site, siteCommentIdentifier)
        assertThat(uiState.first().inputSettings.enableEditName).isFalse
    }

    @Test
    fun `Should ENABLE edit name for a comment from unregistered user`() = test {
        whenever(getCommentUseCase.execute(site, remoteCommentId))
                .thenReturn(UNREGISTERED_COMMENT_ENTITY)

        viewModel.start(site, siteCommentIdentifier)
        assertThat(uiState.last().inputSettings.enableEditName).isTrue
    }

    @Test
    fun `Should DISABLE edit URL for a comment from registered user`() = test {
        viewModel.start(site, siteCommentIdentifier)
        assertThat(uiState.last().inputSettings.enableEditUrl).isFalse
    }

    @Test
    fun `Should ENABLE edit URL for a comment from unregistered user`() = test {
        whenever(getCommentUseCase.execute(site, remoteCommentId))
                .thenReturn(UNREGISTERED_COMMENT_ENTITY)

        viewModel.start(site, siteCommentIdentifier)
        assertThat(uiState.last().inputSettings.enableEditUrl).isTrue
    }

    @Test
    fun `Should DISABLE edit email for a comment from registered user`() = test {
        viewModel.start(site, siteCommentIdentifier)
        assertThat(uiState.last().inputSettings.enableEditEmail).isFalse
    }

    @Test
    fun `Should ENABLE edit email for a comment from unregistered user`() = test {
        whenever(getCommentUseCase.execute(site, remoteCommentId))
                .thenReturn(UNREGISTERED_COMMENT_ENTITY)

        viewModel.start(site, siteCommentIdentifier)
        assertThat(uiState.last().inputSettings.enableEditEmail).isTrue
    }

    @Test
    fun `Should ENABLE edit comment content for a comment from registered user`() = test {
        viewModel.start(site, siteCommentIdentifier)
        assertThat(uiState.last().inputSettings.enableEditComment).isTrue
    }

    @Test
    fun `Should ENABLE edit comment content for a comment from unregistered user`() = test {
        whenever(getCommentUseCase.execute(site, remoteCommentId))
                .thenReturn(UNREGISTERED_COMMENT_ENTITY)

        viewModel.start(site, siteCommentIdentifier)
        assertThat(uiState.last().inputSettings.enableEditComment).isTrue
    }

    @Test
    fun `Should update notification entity on save if NotificationCommentIdentifier`() = test {
        whenever(commentsStore.getCommentByLocalSiteAndRemoteId(site.id, remoteCommentId))
                .thenReturn(listOf(COMMENT_ENTITY))
        whenever(commentsStore.updateEditComment(eq(site), any()))
                .thenReturn(CommentsActionPayload(CommentsActionData(emptyList(), 0)))
        viewModel.start(site, notificationCommentIdentifier)
        viewModel.onActionMenuClicked()
        verify(notificationActionsWrapper).downloadNoteAndUpdateDB(noteId)
    }

    @Test
    fun `Should NOT update notification entity on save if SiteCommentIdentifier`() = test {
        whenever(commentsStore.getCommentByLocalSiteAndRemoteId(site.id, remoteCommentId))
                .thenReturn(listOf(COMMENT_ENTITY))
        whenever(commentsStore.updateEditComment(eq(site), any()))
                .thenReturn(CommentsActionPayload(CommentsActionData(emptyList(), 0)))
        viewModel.start(site, siteCommentIdentifier)
        viewModel.onActionMenuClicked()
        verify(notificationActionsWrapper, times(0)).downloadNoteAndUpdateDB(noteId)
    }

    @Test
    fun `Should NOT update notification entity on save if ReaderCommentIdentifier`() = test {
        whenever(commentsStore.getCommentByLocalSiteAndRemoteId(site.id, remoteCommentId))
                .thenReturn(listOf(COMMENT_ENTITY))
        whenever(commentsStore.updateEditComment(eq(site), any()))
                .thenReturn(CommentsActionPayload(CommentsActionData(emptyList(), 0)))
        viewModel.start(site, readerCommentIdentifier)
        viewModel.onActionMenuClicked()
        verify(notificationActionsWrapper, times(0)).downloadNoteAndUpdateDB(noteId)
    }

    @Test
    fun `Should update local reader entity on save if ReaderCommentIdentifier`() = test {
        whenever(commentsStore.getCommentByLocalSiteAndRemoteId(site.id, remoteCommentId))
                .thenReturn(listOf(COMMENT_ENTITY))
        whenever(commentsStore.updateEditComment(eq(site), any()))
                .thenReturn(CommentsActionPayload(CommentsActionData(emptyList(), 0)))
        viewModel.start(site, readerCommentIdentifier)
        viewModel.onActionMenuClicked()
        verify(readerCommentTableWrapper).addOrUpdateComment(any())
    }

    @Test
    fun `Should trigger DONE action on save if NotificationCommentIdentifier and notification entity update success`() {
        test {
            whenever(commentsStore.getCommentByLocalSiteAndRemoteId(site.id, remoteCommentId))
                    .thenReturn(listOf(COMMENT_ENTITY))
            whenever(commentsStore.updateEditComment(eq(site), any()))
                    .thenReturn(CommentsActionPayload(CommentsActionData(emptyList(), 0)))
            whenever(notificationActionsWrapper.downloadNoteAndUpdateDB(noteId))
                    .thenReturn(true)
            viewModel.start(site, notificationCommentIdentifier)
            viewModel.onActionMenuClicked()
            assertThat(uiActionEvent.firstOrNull()).isEqualTo(DONE)
        }
    }

    @Test
    fun `Should trigger DONE action on save if ReaderCommentIdentifier and reader entity updated`() {
        test {
            whenever(commentsStore.getCommentByLocalSiteAndRemoteId(site.id, remoteCommentId))
                    .thenReturn(listOf(COMMENT_ENTITY))
            whenever(commentsStore.updateEditComment(eq(site), any()))
                    .thenReturn(CommentsActionPayload(CommentsActionData(emptyList(), 0)))
            viewModel.start(site, readerCommentIdentifier)
            viewModel.onActionMenuClicked()
            assertThat(uiActionEvent.firstOrNull()).isEqualTo(DONE)
        }
    }

    @Test
    fun `Should call requestCommentsUpdate() on save if NotificationCommentIdentifier`() = test {
        whenever(commentsStore.getCommentByLocalSiteAndRemoteId(site.id, remoteCommentId))
                .thenReturn(listOf(COMMENT_ENTITY))
        whenever(commentsStore.updateEditComment(eq(site), any()))
                .thenReturn(CommentsActionPayload(CommentsActionData(emptyList(), 0)))
        whenever(notificationActionsWrapper.downloadNoteAndUpdateDB(noteId))
                .thenReturn(true)
        viewModel.start(site, notificationCommentIdentifier)
        viewModel.onActionMenuClicked()
        verify(localCommentCacheUpdateHandler).requestCommentsUpdate()
    }

    @Test
    fun `Should call requestCommentsUpdate() on save if ReaderCommentIdentifier`() = test {
        whenever(commentsStore.getCommentByLocalSiteAndRemoteId(site.id, remoteCommentId))
                .thenReturn(listOf(COMMENT_ENTITY))
        whenever(commentsStore.updateEditComment(eq(site), any()))
                .thenReturn(CommentsActionPayload(CommentsActionData(emptyList(), 0)))
        viewModel.start(site, readerCommentIdentifier)
        viewModel.onActionMenuClicked()
        verify(localCommentCacheUpdateHandler).requestCommentsUpdate()
    }

    @Test
    fun `Should display SnackBar error on save if failed to update notification entity`() = test {
        whenever(commentsStore.getCommentByLocalSiteAndRemoteId(site.id, remoteCommentId))
                .thenReturn(listOf(COMMENT_ENTITY))
        whenever(commentsStore.updateEditComment(eq(site), any()))
                .thenReturn(CommentsActionPayload(CommentsActionData(emptyList(), 0)))
        whenever(notificationActionsWrapper.downloadNoteAndUpdateDB(noteId))
                .thenReturn(false)
        viewModel.start(site, notificationCommentIdentifier)
        viewModel.onActionMenuClicked()
        assertThat(onSnackbarMessage.firstOrNull()).isNotNull
    }

    @Test
    fun `Should display correct error message on save if failed to update notification entity`() = test {
        whenever(commentsStore.getCommentByLocalSiteAndRemoteId(site.id, remoteCommentId))
                .thenReturn(listOf(COMMENT_ENTITY))
        whenever(commentsStore.updateEditComment(eq(site), any()))
                .thenReturn(CommentsActionPayload(CommentsActionData(emptyList(), 0)))
        whenever(notificationActionsWrapper.downloadNoteAndUpdateDB(noteId))
                .thenReturn(false)
        viewModel.start(site, notificationCommentIdentifier)
        viewModel.onActionMenuClicked()
        val expected = UiStringRes(R.string.error_edit_notification)
        val actual = onSnackbarMessage.first().message
        assertEquals(expected, actual)
    }

    @Test
    fun `should correct tracking method for NotificationCommentIdentifier`() = test {
        whenever(commentsStore.getCommentByLocalSiteAndRemoteId(site.id, remoteCommentId))
                .thenReturn(listOf(COMMENT_ENTITY))
        whenever(commentsStore.updateEditComment(eq(site), any()))
                .thenReturn(CommentsActionPayload(CommentsActionData(emptyList(), 0)))
        whenever(notificationActionsWrapper.downloadNoteAndUpdateDB(noteId))
                .thenReturn(true)
        viewModel.start(site, notificationCommentIdentifier)
        viewModel.onActionMenuClicked()
        verify(analyticsUtilsWrapper).trackCommentActionWithSiteDetails(
                Stat.COMMENT_EDITED,
                AnalyticsCommentActionSource.NOTIFICATIONS,
                site
        )
    }

    @Test
    fun `should correct tracking method for ReaderCommentIdentifier`() = test {
        whenever(commentsStore.getCommentByLocalSiteAndRemoteId(site.id, remoteCommentId))
                .thenReturn(listOf(COMMENT_ENTITY))
        whenever(commentsStore.updateEditComment(eq(site), any()))
                .thenReturn(CommentsActionPayload(CommentsActionData(emptyList(), 0)))
        viewModel.start(site, readerCommentIdentifier)
        viewModel.onActionMenuClicked()
        verify(analyticsUtilsWrapper).trackCommentActionWithSiteDetails(
                Stat.COMMENT_EDITED,
                AnalyticsCommentActionSource.READER,
                site
        )
    }

    @Test
    fun `should correct tracking method for SiteCommentIdentifier`() = test {
        whenever(commentsStore.getCommentByLocalSiteAndRemoteId(site.id, remoteCommentId))
                .thenReturn(listOf(COMMENT_ENTITY))
        whenever(commentsStore.updateEditComment(eq(site), any()))
                .thenReturn(CommentsActionPayload(CommentsActionData(emptyList(), 0)))
        viewModel.start(site, siteCommentIdentifier)
        viewModel.onActionMenuClicked()
        verify(analyticsUtilsWrapper).trackCommentActionWithSiteDetails(
                Stat.COMMENT_EDITED,
                AnalyticsCommentActionSource.SITE_COMMENTS,
                site
        )
    }

    private fun setupObservers() {
        uiState.clear()
        uiActionEvent.clear()
        onSnackbarMessage.clear()

        viewModel.uiState.observeForever {
            uiState.add(it)
        }

        viewModel.uiActionEvent.observeForever {
            it.applyIfNotHandled {
                uiActionEvent.add(this)
            }
        }

        viewModel.onSnackbarMessage.observeForever {
            it.applyIfNotHandled {
                onSnackbarMessage.add(this)
            }
        }
    }

    companion object {
        private const val LOCAL_SITE_ID = 123
        private const val REMOTE_SITE_ID = 456L

        private val COMMENT_ENTITY = CommentEntity(
                id = 1000,
                remoteCommentId = 0,
                remotePostId = 0,
                authorId = 4,
                localSiteId = LOCAL_SITE_ID,
                remoteSiteId = REMOTE_SITE_ID,
                authorUrl = "authorUrl",
                authorName = "authorName",
                authorEmail = "authorEmail",
                authorProfileImageUrl = null,
                postTitle = null,
                status = null,
                datePublished = null,
                publishedTimestamp = 0,
                content = "content",
                url = null,
                hasParent = false,
                parentId = 0,
                iLike = false
        )

        private val UNREGISTERED_COMMENT_ENTITY = CommentEntity(
                id = 1000,
                remoteCommentId = 0,
                remotePostId = 0,
                authorId = 0,
                localSiteId = LOCAL_SITE_ID,
                remoteSiteId = REMOTE_SITE_ID,
                authorUrl = "authorUrl",
                authorName = "authorName",
                authorEmail = "authorEmail",
                authorProfileImageUrl = null,
                postTitle = null,
                status = null,
                datePublished = null,
                publishedTimestamp = 0,
                content = "content",
                url = null,
                hasParent = false,
                parentId = 0,
                iLike = false
        )

        private val READER_COMMENT_ENTITY = ReaderComment().apply {
            blogId = REMOTE_SITE_ID
            authorUrl = "authorUrl"
            authorName = "authorName"
            authorEmail = "authorEmail"
            text = "content"
        }

        private val COMMENT_ESSENTIALS = CommentEssentials(
                commentId = COMMENT_ENTITY.id,
                userName = COMMENT_ENTITY.authorName!!,
                commentText = COMMENT_ENTITY.content!!,
                userUrl = COMMENT_ENTITY.authorUrl!!,
                userEmail = COMMENT_ENTITY.authorEmail!!
        )
    }
}
