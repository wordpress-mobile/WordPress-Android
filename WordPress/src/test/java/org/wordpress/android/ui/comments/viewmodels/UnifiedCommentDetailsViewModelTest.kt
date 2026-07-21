package org.wordpress.android.ui.comments.viewmodels

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.check
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.datasets.wrappers.NotificationsTableWrapper
import org.wordpress.android.fluxc.model.CommentStatus
import org.wordpress.android.fluxc.model.CommentStatus.APPROVED
import org.wordpress.android.fluxc.model.CommentStatus.SPAM
import org.wordpress.android.fluxc.model.CommentStatus.TRASH
import org.wordpress.android.fluxc.model.CommentStatus.UNAPPROVED
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.persistence.comments.CommentsDao.CommentEntity
import org.wordpress.android.fluxc.store.CommentsStore
import org.wordpress.android.fluxc.store.CommentsStore.CommentsActionPayload
import org.wordpress.android.fluxc.store.CommentsStore.CommentsData.CommentsActionData
import org.wordpress.android.models.Note
import org.wordpress.android.models.usecases.LocalCommentCacheUpdateHandler
import org.wordpress.android.ui.comments.unified.CommentDetailsActionEvent
import org.wordpress.android.ui.comments.unified.CommentDetailsActionEvent.Close
import org.wordpress.android.ui.comments.unified.CommentDetailsActionEvent.LaunchEditComment
import org.wordpress.android.ui.comments.unified.CommentDetailsActionEvent.OpenPostInReader
import org.wordpress.android.ui.comments.unified.CommentDetailsActionEvent.ReplySent
import org.wordpress.android.ui.comments.unified.CommentIdentifier.NotificationCommentIdentifier
import org.wordpress.android.ui.comments.unified.CommentIdentifier.SiteCommentIdentifier
import org.wordpress.android.ui.comments.unified.CommentsRsDataSource
import org.wordpress.android.ui.comments.unified.CommentsRsDataSource.RsComment
import org.wordpress.android.ui.comments.unified.CommentsRsDataSource.RsResult
import org.wordpress.android.ui.comments.unified.UnifiedCommentDetailsViewModel
import org.wordpress.android.ui.comments.unified.UnifiedCommentDetailsViewModel.CommentDetailsUiState
import org.wordpress.android.ui.notifications.utils.NotificationsActionsWrapper
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.DateTimeUtilsWrapper
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.analytics.AnalyticsUtils.AnalyticsCommentActionSource
import org.wordpress.android.util.analytics.AnalyticsUtilsWrapper
import java.util.Date

@ExperimentalCoroutinesApi
class UnifiedCommentDetailsViewModelTest : BaseUnitTest() {
    @Mock
    lateinit var commentsRsDataSource: CommentsRsDataSource

    @Mock
    lateinit var commentsStore: CommentsStore

    @Mock
    lateinit var localCommentCacheUpdateHandler: LocalCommentCacheUpdateHandler

    @Mock
    lateinit var networkUtilsWrapper: NetworkUtilsWrapper

    @Mock
    lateinit var dateTimeUtilsWrapper: DateTimeUtilsWrapper

    @Mock
    lateinit var notificationsActionsWrapper: NotificationsActionsWrapper

    @Mock
    lateinit var notificationsTableWrapper: NotificationsTableWrapper

    @Mock
    lateinit var analyticsUtilsWrapper: AnalyticsUtilsWrapper

    private lateinit var viewModel: UnifiedCommentDetailsViewModel

    private val uiStates = mutableListOf<CommentDetailsUiState>()
    private val uiActionEvents = mutableListOf<CommentDetailsActionEvent>()
    private val snackbarMessages = mutableListOf<SnackbarMessageHolder>()
    private val moderatedStatuses = mutableListOf<CommentStatus>()

    private val site = SiteModel().apply {
        id = LOCAL_SITE_ID
        siteId = REMOTE_SITE_ID
    }

    @Before
    fun setup() = test {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
        whenever(commentsRsDataSource.getComment(site, REMOTE_COMMENT_ID)).thenReturn(RS_COMMENT)
        whenever(commentsStore.getCommentByLocalSiteAndRemoteId(LOCAL_SITE_ID, REMOTE_COMMENT_ID))
            .thenReturn(listOf(CACHED_COMMENT))
        whenever(commentsRsDataSource.updateStatus(eq(site), eq(REMOTE_COMMENT_ID), any())).thenReturn(RsResult.Success)
        whenever(commentsRsDataSource.delete(site, REMOTE_COMMENT_ID)).thenReturn(RsResult.Success)
        whenever(commentsRsDataSource.createReply(eq(site), any(), any(), any())).thenReturn(RsResult.Success)
        whenever(commentsStore.likeComment(eq(site), eq(REMOTE_COMMENT_ID), eq(null), any()))
            .thenReturn(successPayload())
        whenever(dateTimeUtilsWrapper.javaDateToTimeSpan(any())).thenReturn("2 hours ago")
        whenever(notificationsActionsWrapper.downloadNoteAndUpdateDB(any())).thenReturn(true)

        viewModel = createViewModel()

        setupObservers()
    }

    @Test
    fun `start loads comment via rs and populates ui state`() = test {
        viewModel.start(site, REMOTE_COMMENT_ID)

        val state = uiStates.last()
        assertThat(state.contentVisible).isTrue
        assertThat(state.authorName).isEqualTo("authorName")
        assertThat(state.commentText).isEqualTo("content")
        assertThat(state.status).isEqualTo(APPROVED)
    }

    @Test
    fun `start shows error snackbar when comment cannot be loaded`() = test {
        whenever(commentsRsDataSource.getComment(site, REMOTE_COMMENT_ID)).thenReturn(null)

        viewModel.start(site, REMOTE_COMMENT_ID)

        assertThat(snackbarMessages.firstOrNull()).isNotNull
    }

    @Test
    fun `refresh failure keeps the loaded comment and does not close the screen`() = test {
        viewModel.start(site, REMOTE_COMMENT_ID)
        whenever(commentsRsDataSource.getComment(site, REMOTE_COMMENT_ID)).thenReturn(null)

        viewModel.onCommentEdited()

        assertThat(uiStates.last().contentVisible).isTrue
        assertThat(uiStates.last().status).isEqualTo(APPROVED)
        assertThat(uiActionEvents).doesNotContain(Close)
        assertThat(snackbarMessages).isNotEmpty
    }

    @Test
    fun `moderation during a refresh uses the previously loaded status`() = test {
        viewModel.start(site, REMOTE_COMMENT_ID)
        whenever(commentsRsDataSource.getComment(site, REMOTE_COMMENT_ID)).doSuspendableAnswer {
            delay(LOAD_DELAY_MS)
            RS_COMMENT
        }
        viewModel.onCommentEdited()

        viewModel.onApproveClicked()
        advanceUntilIdle()

        verify(commentsRsDataSource).updateStatus(site, REMOTE_COMMENT_ID, UNAPPROVED)
    }

    @Test
    fun `onApproveClicked toggles approved comment to unapproved via rs and syncs cache`() = test {
        viewModel.start(site, REMOTE_COMMENT_ID)

        viewModel.onApproveClicked()

        verify(commentsRsDataSource).updateStatus(site, REMOTE_COMMENT_ID, UNAPPROVED)
        verify(commentsStore).moderateCommentLocally(site, REMOTE_COMMENT_ID, UNAPPROVED)
        verify(localCommentCacheUpdateHandler, atLeastOnce()).requestCommentsUpdate()
        assertThat(uiStates.last().status).isEqualTo(UNAPPROVED)
    }

    @Test
    fun `onSpamClicked marks comment as spam via rs and closes screen`() = test {
        viewModel.start(site, REMOTE_COMMENT_ID)

        viewModel.onSpamClicked()

        verify(commentsRsDataSource).updateStatus(site, REMOTE_COMMENT_ID, SPAM)
        assertThat(uiActionEvents).contains(Close)
    }

    @Test
    fun `onTrashClicked trashes comment via the update endpoint and closes screen`() = test {
        viewModel.start(site, REMOTE_COMMENT_ID)

        viewModel.onTrashClicked()

        verify(commentsRsDataSource).updateStatus(site, REMOTE_COMMENT_ID, TRASH)
        assertThat(uiActionEvents).contains(Close)
    }

    @Test
    fun `onDeletePermanentlyClicked deletes comment via rs and closes screen`() = test {
        viewModel.start(site, REMOTE_COMMENT_ID)

        viewModel.onDeletePermanentlyClicked()

        verify(commentsRsDataSource).delete(site, REMOTE_COMMENT_ID)
        verify(commentsStore).removeCommentByRemoteId(site, REMOTE_COMMENT_ID)
        assertThat(uiActionEvents).contains(Close)
    }

    @Test
    fun `moderation reverts status and surfaces the server error message`() = test {
        whenever(commentsRsDataSource.updateStatus(eq(site), eq(REMOTE_COMMENT_ID), any()))
            .thenReturn(RsResult.Error("Sorry, you are not allowed to edit this comment."))
        viewModel.start(site, REMOTE_COMMENT_ID)

        viewModel.onApproveClicked()

        assertThat(snackbarMessages.last().message)
            .isEqualTo(UiStringText("Sorry, you are not allowed to edit this comment."))
        assertThat(uiStates.last().status).isEqualTo(APPROVED)
    }

    @Test
    fun `moderation is ignored while the comment is still loading`() = test {
        whenever(commentsRsDataSource.getComment(site, REMOTE_COMMENT_ID)).doSuspendableAnswer {
            delay(LOAD_DELAY_MS)
            RS_COMMENT
        }
        viewModel.start(site, REMOTE_COMMENT_ID)

        viewModel.onApproveClicked()
        advanceUntilIdle()

        verify(commentsRsDataSource, times(0)).updateStatus(eq(site), eq(REMOTE_COMMENT_ID), any())
    }

    @Test
    fun `moderation shows snackbar and does not call rs when offline`() = test {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)
        viewModel.start(site, REMOTE_COMMENT_ID)

        viewModel.onApproveClicked()

        verify(commentsRsDataSource, times(0)).updateStatus(eq(site), eq(REMOTE_COMMENT_ID), any())
        assertThat(snackbarMessages).isNotEmpty
    }

    @Test
    fun `onLikeClicked likes comment via FluxC and updates state`() = test {
        viewModel.start(site, REMOTE_COMMENT_ID)

        viewModel.onLikeClicked()

        verify(commentsStore).likeComment(site, REMOTE_COMMENT_ID, null, true)
        assertThat(uiStates.last().isLiked).isTrue
    }

    @Test
    fun `onLikeClicked is ignored while the comment is still loading`() = test {
        whenever(commentsRsDataSource.getComment(site, REMOTE_COMMENT_ID)).doSuspendableAnswer {
            delay(LOAD_DELAY_MS)
            RS_COMMENT
        }
        viewModel.start(site, REMOTE_COMMENT_ID)

        viewModel.onLikeClicked()
        advanceUntilIdle()

        verify(commentsStore, times(0)).likeComment(eq(site), eq(REMOTE_COMMENT_ID), eq(null), any())
    }

    @Test
    fun `onLikeClicked ignores a second tap while a like is in flight`() = test {
        whenever(commentsStore.likeComment(eq(site), eq(REMOTE_COMMENT_ID), eq(null), any()))
            .doSuspendableAnswer {
                delay(LOAD_DELAY_MS)
                successPayload()
            }
        viewModel.start(site, REMOTE_COMMENT_ID)

        viewModel.onLikeClicked()
        viewModel.onLikeClicked()
        advanceUntilIdle()

        verify(commentsStore, times(1)).likeComment(site, REMOTE_COMMENT_ID, null, true)
        verify(commentsStore, times(0)).likeComment(site, REMOTE_COMMENT_ID, null, false)
    }

    @Test
    fun `onApproveClicked ignores a second tap while a moderation is in flight`() = test {
        whenever(commentsRsDataSource.updateStatus(eq(site), eq(REMOTE_COMMENT_ID), any()))
            .doSuspendableAnswer {
                delay(LOAD_DELAY_MS)
                RsResult.Success
            }
        viewModel.start(site, REMOTE_COMMENT_ID)

        viewModel.onApproveClicked()
        viewModel.onApproveClicked()
        advanceUntilIdle()

        verify(commentsRsDataSource, times(1)).updateStatus(site, REMOTE_COMMENT_ID, UNAPPROVED)
        verify(commentsRsDataSource, times(0)).updateStatus(site, REMOTE_COMMENT_ID, APPROVED)
    }

    @Test
    fun `a moderation tapped during the auto-approve after a reply is ignored`() = test {
        // Replying to an unapproved comment fires an implicit moderate(APPROVED). A moderation
        // tapped while that (suspended) approve is in flight must not race a second write.
        whenever(commentsRsDataSource.getComment(site, REMOTE_COMMENT_ID)).thenReturn(UNAPPROVED_RS_COMMENT)
        whenever(commentsRsDataSource.updateStatus(eq(site), eq(REMOTE_COMMENT_ID), any()))
            .doSuspendableAnswer {
                delay(LOAD_DELAY_MS)
                RsResult.Success
            }
        viewModel.start(site, REMOTE_COMMENT_ID)

        viewModel.onReplyClicked("nice post")
        // The reply has completed and the auto-approve's moderate(APPROVED) is now suspended.
        viewModel.onApproveClicked()
        advanceUntilIdle()

        verify(commentsRsDataSource, times(1)).updateStatus(site, REMOTE_COMMENT_ID, APPROVED)
        verify(commentsRsDataSource, times(0)).updateStatus(site, REMOTE_COMMENT_ID, UNAPPROVED)
    }

    @Test
    fun `a second moderation is allowed once the first has completed`() = test {
        // Guards against the in-flight flag getting stuck true (e.g. a dropped finally reset), which
        // would silently ignore every later moderation. The first call completes before the second.
        viewModel.start(site, REMOTE_COMMENT_ID)

        viewModel.onApproveClicked()
        advanceUntilIdle()
        viewModel.onApproveClicked()
        advanceUntilIdle()

        verify(commentsRsDataSource).updateStatus(site, REMOTE_COMMENT_ID, UNAPPROVED)
        verify(commentsRsDataSource).updateStatus(site, REMOTE_COMMENT_ID, APPROVED)
    }

    @Test
    fun `onEditClicked emits launch edit event with site comment identifier`() = test {
        viewModel.start(site, REMOTE_COMMENT_ID)

        viewModel.onEditClicked()

        val event = uiActionEvents.last()
        assertThat(event).isInstanceOf(LaunchEditComment::class.java)
        assertThat((event as LaunchEditComment).commentIdentifier)
            .isEqualTo(SiteCommentIdentifier(REMOTE_COMMENT_ID))
    }

    @Test
    fun `onEditClicked shows snackbar and does not open the editor when offline`() = test {
        viewModel.start(site, REMOTE_COMMENT_ID)
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)

        viewModel.onEditClicked()

        assertThat(uiActionEvents.filterIsInstance<LaunchEditComment>()).isEmpty()
        assertThat(snackbarMessages).isNotEmpty
    }

    @Test
    fun `onPostTitleClicked emits open post in reader event`() = test {
        viewModel.start(site, REMOTE_COMMENT_ID)

        viewModel.onPostTitleClicked()

        val event = uiActionEvents.last()
        assertThat(event).isInstanceOf(OpenPostInReader::class.java)
        assertThat((event as OpenPostInReader).blogId).isEqualTo(REMOTE_SITE_ID)
        assertThat(event.postId).isEqualTo(REMOTE_POST_ID)
    }

    @Test
    fun `onPostTitleClicked does nothing on a site without a wpcom blog id`() = test {
        val selfHostedSite = SiteModel().apply { id = LOCAL_SITE_ID }
        whenever(commentsRsDataSource.getComment(selfHostedSite, REMOTE_COMMENT_ID)).thenReturn(RS_COMMENT)
        viewModel.start(selfHostedSite, REMOTE_COMMENT_ID)

        viewModel.onPostTitleClicked()

        assertThat(uiActionEvents).isEmpty()
    }

    @Test
    fun `onReplyClicked creates reply via rs and emits reply sent event`() = test {
        viewModel.start(site, REMOTE_COMMENT_ID)

        viewModel.onReplyClicked("nice post")

        verify(commentsRsDataSource).createReply(site, REMOTE_POST_ID, REMOTE_COMMENT_ID, "nice post")
        verify(localCommentCacheUpdateHandler, atLeastOnce()).requestCommentsUpdate()
        assertThat(uiActionEvents).contains(ReplySent)
    }

    @Test
    fun `onReplyClicked does nothing for blank text`() = test {
        viewModel.start(site, REMOTE_COMMENT_ID)

        viewModel.onReplyClicked("   ")

        verify(commentsRsDataSource, times(0)).createReply(eq(site), any(), any(), any())
    }

    @Test
    fun `onReplyClicked shows snackbar and does not call rs when offline`() = test {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)
        viewModel.start(site, REMOTE_COMMENT_ID)

        viewModel.onReplyClicked("nice post")

        verify(commentsRsDataSource, times(0)).createReply(eq(site), any(), any(), any())
        assertThat(snackbarMessages).isNotEmpty
    }

    @Test
    fun `onReplyClicked shows snackbar and no reply sent event on error`() = test {
        whenever(commentsRsDataSource.createReply(eq(site), any(), any(), any()))
            .thenReturn(RsResult.Error("nope"))
        viewModel.start(site, REMOTE_COMMENT_ID)

        viewModel.onReplyClicked("nice post")

        assertThat(snackbarMessages).isNotEmpty
        assertThat(uiActionEvents).doesNotContain(ReplySent)
    }

    @Test
    fun `replying to an unapproved comment approves it via rs`() = test {
        whenever(commentsRsDataSource.getComment(site, REMOTE_COMMENT_ID)).thenReturn(UNAPPROVED_RS_COMMENT)
        viewModel.start(site, REMOTE_COMMENT_ID)

        viewModel.onReplyClicked("nice post")

        verify(commentsRsDataSource).createReply(site, REMOTE_POST_ID, REMOTE_COMMENT_ID, "nice post")
        verify(commentsRsDataSource).updateStatus(site, REMOTE_COMMENT_ID, APPROVED)
        assertThat(uiStates.last().status).isEqualTo(APPROVED)
    }

    @Test
    fun `onEditClicked uses the note identifier in note mode`() = test {
        viewModel.start(site, REMOTE_COMMENT_ID, NOTE_ID)

        viewModel.onEditClicked()

        val event = uiActionEvents.last()
        assertThat((event as LaunchEditComment).commentIdentifier)
            .isEqualTo(NotificationCommentIdentifier(NOTE_ID, REMOTE_COMMENT_ID))
    }

    @Test
    fun `moderation in note mode refreshes the note and fires commentModerated`() = test {
        viewModel.start(site, REMOTE_COMMENT_ID, NOTE_ID)

        viewModel.onApproveClicked()

        verify(notificationsActionsWrapper).downloadNoteAndUpdateDB(NOTE_ID)
        assertThat(moderatedStatuses).containsExactly(UNAPPROVED)
    }

    @Test
    fun `failed moderation in note mode neither refreshes the note nor fires commentModerated`() = test {
        whenever(commentsRsDataSource.updateStatus(eq(site), eq(REMOTE_COMMENT_ID), any()))
            .thenReturn(RsResult.Error("nope"))
        viewModel.start(site, REMOTE_COMMENT_ID, NOTE_ID)

        viewModel.onApproveClicked()

        verifyNoInteractions(notificationsActionsWrapper)
        assertThat(moderatedStatuses).isEmpty()
    }

    @Test
    fun `moderation without a note does not touch the notification wrappers`() = test {
        viewModel.start(site, REMOTE_COMMENT_ID)

        viewModel.onApproveClicked()

        verifyNoInteractions(notificationsActionsWrapper)
        verifyNoInteractions(notificationsTableWrapper)
        assertThat(moderatedStatuses).isEmpty()
    }

    @Test
    fun `like in note mode refreshes the note`() = test {
        viewModel.start(site, REMOTE_COMMENT_ID, NOTE_ID)

        viewModel.onLikeClicked()

        verify(notificationsActionsWrapper).downloadNoteAndUpdateDB(NOTE_ID)
    }

    @Test
    fun `replying to an unapproved comment in note mode fires commentModerated with approved`() = test {
        whenever(commentsRsDataSource.getComment(site, REMOTE_COMMENT_ID)).thenReturn(UNAPPROVED_RS_COMMENT)
        viewModel.start(site, REMOTE_COMMENT_ID, NOTE_ID)

        viewModel.onReplyClicked("nice post")

        assertThat(moderatedStatuses).containsExactly(APPROVED)
    }

    @Test
    fun `like state falls back to the note when the comment has no cache row`() = test {
        val note = mock<Note>()
        whenever(note.hasLikedComment()).thenReturn(true)
        whenever(notificationsTableWrapper.getNoteById(NOTE_ID)).thenReturn(note)
        whenever(commentsStore.getCommentByLocalSiteAndRemoteId(LOCAL_SITE_ID, REMOTE_COMMENT_ID))
            .thenReturn(emptyList())
        whenever(commentsRsDataSource.fetchPostTitles(site, listOf(REMOTE_POST_ID)))
            .thenReturn(emptyMap())

        viewModel.start(site, REMOTE_COMMENT_ID, NOTE_ID)

        assertThat(uiStates.last().isLiked).isTrue
    }

    @Test
    fun `approving an unapproved comment tracks COMMENT_APPROVED for site comments`() = test {
        whenever(commentsRsDataSource.getComment(site, REMOTE_COMMENT_ID)).thenReturn(UNAPPROVED_RS_COMMENT)
        viewModel.start(site, REMOTE_COMMENT_ID)

        viewModel.onApproveClicked()

        verify(analyticsUtilsWrapper)
            .trackCommentActionWithSiteDetails(Stat.COMMENT_APPROVED, AnalyticsCommentActionSource.SITE_COMMENTS, site)
    }

    @Test
    fun `unapproving an approved comment tracks COMMENT_UNAPPROVED`() = test {
        viewModel.start(site, REMOTE_COMMENT_ID)

        viewModel.onApproveClicked()

        verify(analyticsUtilsWrapper).trackCommentActionWithSiteDetails(
            Stat.COMMENT_UNAPPROVED, AnalyticsCommentActionSource.SITE_COMMENTS, site
        )
    }

    @Test
    fun `spamming tracks COMMENT_SPAMMED and un-spamming tracks COMMENT_UNSPAMMED`() = test {
        viewModel.start(site, REMOTE_COMMENT_ID)
        viewModel.onSpamClicked()
        verify(analyticsUtilsWrapper)
            .trackCommentActionWithSiteDetails(Stat.COMMENT_SPAMMED, AnalyticsCommentActionSource.SITE_COMMENTS, site)

        whenever(commentsRsDataSource.getComment(site, REMOTE_COMMENT_ID)).thenReturn(RS_COMMENT.copy(status = SPAM))
        val spammedViewModel = createViewModel()
        spammedViewModel.start(site, REMOTE_COMMENT_ID)
        spammedViewModel.onSpamClicked()
        verify(analyticsUtilsWrapper)
            .trackCommentActionWithSiteDetails(Stat.COMMENT_UNSPAMMED, AnalyticsCommentActionSource.SITE_COMMENTS, site)
    }

    @Test
    fun `trashing tracks COMMENT_TRASHED and un-trashing tracks COMMENT_UNTRASHED`() = test {
        viewModel.start(site, REMOTE_COMMENT_ID)
        viewModel.onTrashClicked()
        verify(analyticsUtilsWrapper)
            .trackCommentActionWithSiteDetails(Stat.COMMENT_TRASHED, AnalyticsCommentActionSource.SITE_COMMENTS, site)

        whenever(commentsRsDataSource.getComment(site, REMOTE_COMMENT_ID)).thenReturn(RS_COMMENT.copy(status = TRASH))
        val trashedViewModel = createViewModel()
        trashedViewModel.start(site, REMOTE_COMMENT_ID)
        trashedViewModel.onTrashClicked()
        verify(analyticsUtilsWrapper)
            .trackCommentActionWithSiteDetails(Stat.COMMENT_UNTRASHED, AnalyticsCommentActionSource.SITE_COMMENTS, site)
    }

    @Test
    fun `deleting permanently tracks COMMENT_DELETED`() = test {
        viewModel.start(site, REMOTE_COMMENT_ID)

        viewModel.onDeletePermanentlyClicked()

        verify(analyticsUtilsWrapper)
            .trackCommentActionWithSiteDetails(Stat.COMMENT_DELETED, AnalyticsCommentActionSource.SITE_COMMENTS, site)
    }

    @Test
    fun `liking tracks COMMENT_LIKED and unliking tracks COMMENT_UNLIKED`() = test {
        viewModel.start(site, REMOTE_COMMENT_ID)
        viewModel.onLikeClicked()
        verify(analyticsUtilsWrapper)
            .trackCommentActionWithSiteDetails(Stat.COMMENT_LIKED, AnalyticsCommentActionSource.SITE_COMMENTS, site)

        whenever(commentsStore.getCommentByLocalSiteAndRemoteId(LOCAL_SITE_ID, REMOTE_COMMENT_ID))
            .thenReturn(listOf(CACHED_COMMENT.copy(iLike = true)))
        val likedViewModel = createViewModel()
        likedViewModel.start(site, REMOTE_COMMENT_ID)
        likedViewModel.onLikeClicked()
        verify(analyticsUtilsWrapper)
            .trackCommentActionWithSiteDetails(Stat.COMMENT_UNLIKED, AnalyticsCommentActionSource.SITE_COMMENTS, site)
    }

    @Test
    fun `opening the editor tracks COMMENT_EDITOR_OPENED`() = test {
        viewModel.start(site, REMOTE_COMMENT_ID)

        viewModel.onEditClicked()

        verify(analyticsUtilsWrapper).trackCommentActionWithSiteDetails(
            Stat.COMMENT_EDITOR_OPENED, AnalyticsCommentActionSource.SITE_COMMENTS, site
        )
    }

    @Test
    fun `a successful reply tracks the reply with the post and comment ids`() = test {
        viewModel.start(site, REMOTE_COMMENT_ID)

        viewModel.onReplyClicked("nice post")

        verify(analyticsUtilsWrapper).trackCommentReplyWithDetails(
            eq(false),
            eq(site),
            check {
                assertThat(it.remotePostId).isEqualTo(REMOTE_POST_ID)
                assertThat(it.remoteCommentId).isEqualTo(REMOTE_COMMENT_ID)
            },
            eq(AnalyticsCommentActionSource.SITE_COMMENTS)
        )
    }

    @Test
    fun `replying to an unapproved comment tracks the implicit approve`() = test {
        whenever(commentsRsDataSource.getComment(site, REMOTE_COMMENT_ID)).thenReturn(UNAPPROVED_RS_COMMENT)
        viewModel.start(site, REMOTE_COMMENT_ID)

        viewModel.onReplyClicked("nice post")

        verify(analyticsUtilsWrapper).trackCommentActionWithSiteDetails(
            Stat.COMMENT_APPROVED, AnalyticsCommentActionSource.SITE_COMMENTS, site
        )
    }

    @Test
    fun `a note-mode reply tracks with the notifications source`() = test {
        viewModel.start(site, REMOTE_COMMENT_ID, NOTE_ID)

        viewModel.onReplyClicked("nice post")

        verify(analyticsUtilsWrapper).trackCommentReplyWithDetails(
            eq(false), eq(site), any(), eq(AnalyticsCommentActionSource.NOTIFICATIONS)
        )
    }

    @Test
    fun `note-mode actions track with the notifications source`() = test {
        viewModel.start(site, REMOTE_COMMENT_ID, NOTE_ID)

        viewModel.onApproveClicked()

        verify(analyticsUtilsWrapper).trackCommentActionWithSiteDetails(
            Stat.COMMENT_UNAPPROVED, AnalyticsCommentActionSource.NOTIFICATIONS, site
        )
    }

    @Test
    fun `a failed moderation does not track an analytics event`() = test {
        whenever(commentsRsDataSource.updateStatus(eq(site), eq(REMOTE_COMMENT_ID), any()))
            .thenReturn(RsResult.Error("nope"))
        viewModel.start(site, REMOTE_COMMENT_ID)

        viewModel.onApproveClicked()

        verifyNoInteractions(analyticsUtilsWrapper)
    }

    private fun createViewModel() = UnifiedCommentDetailsViewModel(
        mainDispatcher = testDispatcher(),
        bgDispatcher = testDispatcher(),
        commentsRsDataSource = commentsRsDataSource,
        commentsStore = commentsStore,
        localCommentCacheUpdateHandler = localCommentCacheUpdateHandler,
        networkUtilsWrapper = networkUtilsWrapper,
        dateTimeUtilsWrapper = dateTimeUtilsWrapper,
        notificationsActionsWrapper = notificationsActionsWrapper,
        notificationsTableWrapper = notificationsTableWrapper,
        analyticsUtilsWrapper = analyticsUtilsWrapper
    )

    private fun setupObservers() {
        uiStates.clear()
        uiActionEvents.clear()
        snackbarMessages.clear()
        moderatedStatuses.clear()

        viewModel.uiState.observeForever { uiStates.add(it) }
        viewModel.uiActionEvent.observeForever { it.applyIfNotHandled { uiActionEvents.add(this) } }
        viewModel.onSnackbarMessage.observeForever { it.applyIfNotHandled { snackbarMessages.add(this) } }
        viewModel.commentModerated.observeForever { it.applyIfNotHandled { moderatedStatuses.add(this) } }
    }

    private fun successPayload() = CommentsActionPayload(CommentsActionData(emptyList(), 0))

    companion object {
        private const val LOCAL_SITE_ID = 123
        private const val REMOTE_SITE_ID = 456L
        private const val LOCAL_COMMENT_ID = 1000
        private const val REMOTE_COMMENT_ID = 4321L
        private const val REMOTE_POST_ID = 99L
        private const val LOAD_DELAY_MS = 1000L
        private const val NOTE_ID = "note_5555"

        private val RS_COMMENT = RsComment(
            remoteCommentId = REMOTE_COMMENT_ID,
            authorName = "authorName",
            authorAvatarUrl = "",
            dateGmt = Date(0),
            contentHtml = "content",
            url = "",
            postId = REMOTE_POST_ID,
            status = APPROVED
        )

        private val UNAPPROVED_RS_COMMENT = RS_COMMENT.copy(status = UNAPPROVED)

        private val CACHED_COMMENT = CommentEntity(
            id = LOCAL_COMMENT_ID.toLong(),
            remoteCommentId = REMOTE_COMMENT_ID,
            remotePostId = REMOTE_POST_ID,
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
