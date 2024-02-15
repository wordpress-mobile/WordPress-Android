package org.wordpress.android.ui.notifications

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.datasets.wrappers.ReaderPostTableWrapper
import org.wordpress.android.fluxc.store.CommentsStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.models.Note
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.push.GCMMessageHandler
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalOverlayUtil
import org.wordpress.android.ui.notifications.utils.NotificationsUtilsWrapper
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.reader.actions.ReaderActions
import org.wordpress.android.ui.reader.actions.ReaderPostActionsWrapper

private const val REQUEST_BLOG_LISTENER_PARAM_POSITION = 2

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class NotificationsListViewModelTest : BaseUnitTest() {
    @Mock
    private lateinit var appPrefsWrapper: AppPrefsWrapper

    @Mock
    private lateinit var jetpackFeatureRemovalOverlayUtil: JetpackFeatureRemovalOverlayUtil

    @Mock
    private lateinit var gcmMessageHandler: GCMMessageHandler

    @Mock
    private lateinit var notificationsUtilsWrapper: NotificationsUtilsWrapper

    @Mock
    private lateinit var readerPostTableWrapper: ReaderPostTableWrapper

    @Mock
    private lateinit var readerPostActionsWrapper: ReaderPostActionsWrapper

    @Mock
    private lateinit var siteStore: SiteStore

    @Mock
    private lateinit var commentStore: CommentsStore

    @Mock
    private lateinit var action: ActionHandler

    private lateinit var viewModel: NotificationsListViewModel

    @Before
    fun setup() {
        viewModel = NotificationsListViewModel(
            testDispatcher(),
            appPrefsWrapper,
            jetpackFeatureRemovalOverlayUtil,
            gcmMessageHandler,
            notificationsUtilsWrapper,
            readerPostTableWrapper,
            readerPostActionsWrapper,
            siteStore,
            commentStore
        )
    }

    @Test
    fun `WHEN the note cannot be retrieved THEN try opening the detail view`() {
        // Given
        val noteId = "1"
        whenever(notificationsUtilsWrapper.getNoteById(noteId)).thenReturn(null)

        // When
        viewModel.openNote(noteId, action::openInTheReader, action::openDetailView)

        // Then
        verify(action, times(0)).openInTheReader(any(), any(), any())
        verify(action, times(1)).openDetailView()
    }

    @Test
    fun `WHEN the note is not a comment THEN open detail view`() {
        // Given
        val noteId = "1"
        val note = mock<Note>()
        whenever(notificationsUtilsWrapper.getNoteById(noteId)).thenReturn(note)
        whenever(note.isCommentType).thenReturn(false)

        // When
        viewModel.openNote(noteId, action::openInTheReader, action::openDetailView)

        // Then
        verify(action, times(0)).openInTheReader(any(), any(), any())
        verify(action, times(1)).openDetailView()
    }

    @Test
    fun `WHEN the note is a comment that can be moderated THEN open detail view`() {
        // Given
        val noteId = "1"
        val note = mock<Note>()
        whenever(notificationsUtilsWrapper.getNoteById(noteId)).thenReturn(note)
        whenever(note.isCommentType).thenReturn(true)
        whenever(note.canModerate()).thenReturn(true)

        // When
        viewModel.openNote(noteId, action::openInTheReader, action::openDetailView)

        // Then
        verify(action, times(0)).openInTheReader(any(), any(), any())
        verify(action, times(1)).openDetailView()
    }

    @Test
    fun `WHEN the note is a comment that cannot be moderated and the reader post exists THEN open in reader`() {
        // Given
        val noteId = "1"
        val siteId = 1L
        val postId = 2L
        val commentId = 3L
        val note = mock<Note>()
        val readerPost = mock<ReaderPost>()
        whenever(notificationsUtilsWrapper.getNoteById(noteId)).thenReturn(note)
        whenever(note.siteId).thenReturn(siteId.toInt())
        whenever(note.postId).thenReturn(postId.toInt())
        whenever(note.commentId).thenReturn(commentId)
        whenever(note.isCommentType).thenReturn(true)
        whenever(note.canModerate()).thenReturn(false)
        whenever(readerPostTableWrapper.getBlogPost(siteId, postId, false)).thenReturn(readerPost)

        // When
        viewModel.openNote(noteId, action::openInTheReader, action::openDetailView)

        // Then
        verify(action, times(1)).openInTheReader(siteId, postId, commentId)
        verify(action, times(0)).openDetailView()
    }

    @Test
    fun `WHEN the note is a comment that cannot be moderated and the reader post is retrieved THEN open in reader`() {
        // Given
        val noteId = "1"
        val siteId = 1L
        val postId = 2L
        val commentId = 3L
        val note = mock<Note>()
        whenever(notificationsUtilsWrapper.getNoteById(noteId)).thenReturn(note)
        whenever(note.siteId).thenReturn(siteId.toInt())
        whenever(note.postId).thenReturn(postId.toInt())
        whenever(note.commentId).thenReturn(commentId)
        whenever(note.isCommentType).thenReturn(true)
        whenever(note.canModerate()).thenReturn(false)
        whenever(readerPostTableWrapper.getBlogPost(siteId, postId, false)).thenReturn(null)
        whenever(readerPostActionsWrapper.requestBlogPost(any(), any(), any())).then {
            (it.arguments[REQUEST_BLOG_LISTENER_PARAM_POSITION] as ReaderActions.OnRequestListener<*>)
                .onSuccess(null)
        }

        // When
        viewModel.openNote(noteId, action::openInTheReader, action::openDetailView)

        // Then
        verify(action, times(1)).openInTheReader(siteId, postId, commentId)
        verify(action, times(0)).openDetailView()
    }

    @Test
    fun `WHEN the comment note cannot be moderated and the reader post retrieval fails THEN open detail view`() {
        // Given
        val noteId = "1"
        val siteId = 1L
        val postId = 2L
        val note = mock<Note>()
        whenever(notificationsUtilsWrapper.getNoteById(noteId)).thenReturn(note)
        whenever(note.siteId).thenReturn(siteId.toInt())
        whenever(note.postId).thenReturn(postId.toInt())
        whenever(note.isCommentType).thenReturn(true)
        whenever(note.canModerate()).thenReturn(false)
        whenever(readerPostTableWrapper.getBlogPost(siteId, postId, false)).thenReturn(null)
        whenever(readerPostActionsWrapper.requestBlogPost(any(), any(), any())).then {
            (it.arguments[REQUEST_BLOG_LISTENER_PARAM_POSITION] as ReaderActions.OnRequestListener<*>)
                .onFailure(500)
        }

        // When
        viewModel.openNote(noteId, action::openInTheReader, action::openDetailView)

        // Then
        verify(action, times(0)).openInTheReader(any(), any(), any())
        verify(action, times(1)).openDetailView()
    }

    interface ActionHandler {
        fun openInTheReader(siteId: Long, postId: Long, commentId: Long)

        fun openDetailView()
    }
}
