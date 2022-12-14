package org.wordpress.android.ui.reader

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.reader.comments.ThreadedCommentsActionSource.READER_THREADED_COMMENTS
import org.wordpress.android.ui.reader.usecases.ReaderCommentsFollowUseCase
import org.wordpress.android.ui.reader.usecases.ReaderCommentsFollowUseCase.FollowCommentsState
import org.wordpress.android.ui.reader.usecases.ReaderCommentsFollowUseCase.FollowCommentsState.FollowStateChanged
import org.wordpress.android.ui.utils.UiString.UiStringText

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class ReaderFollowCommentsHandlerTest : BaseUnitTest() {
    @Mock lateinit var readerCommentsFollowUseCase: ReaderCommentsFollowUseCase

    private lateinit var followCommentsHandler: ReaderFollowCommentsHandler
    private val blogId = 100L
    private val postId = 1000L
    private var uiState: FollowCommentsState? = null
    private var holder: SnackbarMessageHolder? = null
    private var followStateChanged: FollowStateChanged? = null

    @Before
    fun setup() {
        followCommentsHandler = ReaderFollowCommentsHandler(
                readerCommentsFollowUseCase,
                testDispatcher()
        )
    }

    @Test
    fun `handleFollowCommentsStatusRequest collects expected state`() = test {
        val userMessage = UiStringText("handleFollowCommentsStatusRequest")
        val state = FollowCommentsState.FollowStateChanged(
                blogId = blogId,
                postId = postId,
                isFollowing = true,
                isReceivingNotifications = false,
                isInit = false,
                userMessage = userMessage
        )

        whenever(readerCommentsFollowUseCase.getMySubscriptionToPost(blogId, postId, false)).thenReturn(
                flow { emit(state) }
        )

        setupObservers()

        followCommentsHandler.handleFollowCommentsStatusRequest(blogId, postId, false)

        requireNotNull(uiState).let {
            assertThat(it).isEqualTo(state)
        }

        requireNotNull(holder).let {
            assertThat(it.message).isEqualTo(userMessage)
        }
    }

    @Test
    fun `handleFollowCommentsClicked collects expected state`() = test {
        val userMessage = UiStringText("handleFollowCommentsClicked")
        val state = FollowCommentsState.FollowStateChanged(
                blogId = blogId,
                postId = postId,
                isFollowing = true,
                isReceivingNotifications = false,
                isInit = false,
                userMessage = userMessage
        )

        whenever(readerCommentsFollowUseCase.setMySubscriptionToPost(
                blogId,
                postId,
                true,
                READER_THREADED_COMMENTS
        )).thenReturn(
                flow { emit(state) }
        )

        setupObservers()

        followCommentsHandler.handleFollowCommentsClicked(blogId, postId, true, READER_THREADED_COMMENTS, null)

        requireNotNull(uiState).let {
            assertThat(it).isEqualTo(state)
        }

        requireNotNull(holder).let {
            assertThat(it.message).isEqualTo(userMessage)
        }
    }

    @Test
    fun `handleFollowCommentsClicked adds a snackbar with action`() = test {
        val userMessage = UiStringText("handleFollowCommentsClicked")
        val snackbarAction = {}
        val state = FollowCommentsState.FollowStateChanged(
                blogId = blogId,
                postId = postId,
                isFollowing = true,
                isReceivingNotifications = false,
                isInit = false,
                userMessage = userMessage
        )

        whenever(readerCommentsFollowUseCase.setMySubscriptionToPost(
                blogId,
                postId,
                true,
                READER_THREADED_COMMENTS
        )).thenReturn(
                flow { emit(state) }
        )

        setupObservers()

        followCommentsHandler.handleFollowCommentsClicked(
                blogId,
                postId,
                true,
                READER_THREADED_COMMENTS,
                snackbarAction
        )

        requireNotNull(uiState).let {
            assertThat(it).isEqualTo(state)
        }

        requireNotNull(holder).let {
            assertThat(it.message).isEqualTo(userMessage)
            assertThat(it.buttonAction == snackbarAction).isTrue()
        }
    }

    @Test
    fun `handleEnableByPushNotificationsClicked triggers an update for push status`() = test {
        val userMessage = UiStringText("handleFollowCommentsClicked")
        val snackbarAction = {}
        val state = FollowCommentsState.FollowStateChanged(
                blogId = blogId,
                postId = postId,
                isFollowing = true,
                isReceivingNotifications = true,
                isInit = false,
                userMessage = userMessage,
                forcePushNotificationsUpdate = true
        )

        whenever(readerCommentsFollowUseCase.setEnableByPushNotifications(
                blogId,
                postId,
                true,
                READER_THREADED_COMMENTS
        )).thenReturn(
                flow { emit(state) }
        )

        setupObservers()

        followCommentsHandler.handleEnableByPushNotificationsClicked(
                blogId,
                postId,
                true,
                READER_THREADED_COMMENTS,
                snackbarAction
        )

        requireNotNull(uiState).let {
            assertThat(it).isEqualTo(state)
        }

        requireNotNull(followStateChanged).let {
            assertThat(it.isReceivingNotifications).isTrue()
        }
    }

    private fun setupObservers() {
        uiState = null

        followCommentsHandler.followStatusUpdate.observeForever {
            uiState = it
        }

        holder = null
        followCommentsHandler.snackbarEvents.observeForever { event ->
            event.applyIfNotHandled {
                holder = this
            }
        }

        followStateChanged = null
        followCommentsHandler.pushNotificationsStatusUpdate.observeForever {
            followStateChanged = it
        }
    }
}
