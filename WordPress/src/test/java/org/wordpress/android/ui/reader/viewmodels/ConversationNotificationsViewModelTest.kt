package org.wordpress.android.ui.reader.viewmodels

import androidx.lifecycle.MutableLiveData
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.datasets.wrappers.ReaderPostTableWrapper
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.test
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.reader.FollowCommentsUiStateType
import org.wordpress.android.ui.reader.FollowCommentsUiStateType.VISIBLE_WITH_STATE
import org.wordpress.android.ui.reader.FollowConversationUiState
import org.wordpress.android.ui.reader.ReaderFollowCommentsHandler
import org.wordpress.android.ui.reader.comments.ThreadedCommentsActionSource.READER_THREADED_COMMENTS
import org.wordpress.android.ui.reader.usecases.ReaderCommentsFollowUseCase.FollowCommentsState
import org.wordpress.android.ui.reader.usecases.ReaderCommentsFollowUseCase.FollowCommentsState.FollowStateChanged
import org.wordpress.android.viewmodel.Event

@InternalCoroutinesApi
class ConversationNotificationsViewModelTest : BaseUnitTest() {
    @Mock lateinit var followCommentsHandler: ReaderFollowCommentsHandler
    @Mock lateinit var readerPostTableWrapper: ReaderPostTableWrapper

    private lateinit var viewModel: ConversationNotificationsViewModel
    private val blogId = 100L
    private val postId = 1000L
    private var snackbarEvents = MutableLiveData<Event<SnackbarMessageHolder>>()
    private var followStatusUpdate = MutableLiveData<FollowCommentsState>()
    private var uiState: FollowConversationUiState? = null

    private val internalPost = ReaderPost().also { it.isExternal = false }
    private val externalPost = ReaderPost().also { it.isExternal = true }

    @Before
    fun setUp() {
        whenever(followCommentsHandler.snackbarEvents).thenReturn(snackbarEvents)
        whenever(followCommentsHandler.followStatusUpdate).thenReturn(followStatusUpdate)
        whenever(readerPostTableWrapper.getBlogPost(anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(internalPost)

        viewModel = ConversationNotificationsViewModel(
                followCommentsHandler,
                readerPostTableWrapper,
                TEST_DISPATCHER
        )
    }

    @Test
    fun `follow ui state is DISABLED on start`() {
        setupObserversAndStart()

        assertThat(uiState).isNotNull
        assertThat(uiState!!.flags.type).isEqualTo(FollowCommentsUiStateType.DISABLED)
    }

    @Test
    fun `onSwipeToRefresh updates follow conversation status`() = test {
        var stateChanged = FollowStateChanged(blogId, postId, true, false, true)
        doAnswer {
            followStatusUpdate.postValue(stateChanged)
        }.whenever(followCommentsHandler).handleFollowCommentsStatusRequest(anyLong(), anyLong(), anyBoolean())

        setupObserversAndStart()

        requireNotNull(uiState).let {
            assertThat(it.flags.type).isEqualTo(VISIBLE_WITH_STATE)
        }

        stateChanged = FollowStateChanged(blogId, postId, true, false)
        doAnswer {
            followStatusUpdate.postValue(stateChanged)
        }.whenever(followCommentsHandler).handleFollowCommentsStatusRequest(anyLong(), anyLong(), anyBoolean())

        viewModel.onRefresh()

        requireNotNull(uiState).let {
            assertThat(it.flags.type).isEqualTo(VISIBLE_WITH_STATE)
        }
    }

    @Test
    fun `onFollowTapped toggles follow status`() = test {
        var stateChanged = FollowStateChanged(blogId, postId, false, false)
        doAnswer {
            followStatusUpdate.postValue(stateChanged)
        }.whenever(followCommentsHandler).handleFollowCommentsStatusRequest(anyLong(), anyLong(), anyBoolean())

        doAnswer {
            stateChanged = FollowStateChanged(blogId, postId, true, false)
            followStatusUpdate.postValue(stateChanged)
        }.whenever(followCommentsHandler).handleFollowCommentsClicked(
                eq(blogId),
                eq(postId),
                eq(true),
                eq(READER_THREADED_COMMENTS),
                anyOrNull()
        )

        setupObserversAndStart()

        requireNotNull(uiState).let {
            assertThat(it.flags.type).isEqualTo(VISIBLE_WITH_STATE)
            assertThat(it.flags.isFollowing).isFalse()
            it.onFollowTapped?.invoke()
        }

        requireNotNull(uiState).let {
            assertThat(it.flags.type).isEqualTo(VISIBLE_WITH_STATE)
            assertThat(it.flags.isFollowing).isTrue()
        }
    }

    @Test
    fun `onUnfollowTapped toggles follow status`() = test {
        var stateChanged = FollowStateChanged(blogId, postId, true, false)
        doAnswer {
            followStatusUpdate.postValue(stateChanged)
        }.whenever(followCommentsHandler).handleFollowCommentsStatusRequest(anyLong(), anyLong(), anyBoolean())

        doAnswer {
            stateChanged = FollowStateChanged(blogId, postId, false, false)
            followStatusUpdate.postValue(stateChanged)
        }.whenever(followCommentsHandler).handleFollowCommentsClicked(
                blogId,
                postId,
                false,
                READER_THREADED_COMMENTS,
                null
        )

        setupObserversAndStart()

        requireNotNull(uiState).let {
            assertThat(it.flags.type).isEqualTo(VISIBLE_WITH_STATE)
            assertThat(it.flags.isFollowing).isTrue()
            viewModel.onUnfollowTapped()
        }

        requireNotNull(uiState).let {
            assertThat(it.flags.type).isEqualTo(VISIBLE_WITH_STATE)
            assertThat(it.flags.isFollowing).isFalse()
        }
    }

    @Test
    fun `onChangePushNotificationsRequest toggles push notifications status`() = test {
        var stateChanged = FollowStateChanged(blogId, postId, true, false)
        doAnswer {
            followStatusUpdate.postValue(stateChanged)
        }.whenever(followCommentsHandler).handleFollowCommentsStatusRequest(anyLong(), anyLong(), anyBoolean())

        doAnswer {
            stateChanged = FollowStateChanged(blogId, postId, true, true)
            followStatusUpdate.postValue(stateChanged)
        }.whenever(followCommentsHandler).handleEnableByPushNotificationsClicked(
                blogId,
                postId,
                true,
                READER_THREADED_COMMENTS,
                null
        )

        setupObserversAndStart()

        requireNotNull(uiState).let {
            assertThat(it.flags.type).isEqualTo(VISIBLE_WITH_STATE)
            assertThat(it.flags.isReceivingNotifications).isFalse()
            viewModel.onChangePushNotificationsRequest(true)
        }

        requireNotNull(uiState).let {
            assertThat(it.flags.type).isEqualTo(VISIBLE_WITH_STATE)
            assertThat(it.flags.isReceivingNotifications).isTrue()
        }
    }

    @Test
    fun `followCommentsHandler is not called for external posts`() = test {
        whenever(readerPostTableWrapper.getBlogPost(anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(externalPost)
        setupObserversAndStart()

        verify(followCommentsHandler, never()).handleFollowCommentsStatusRequest(anyOrNull(), anyOrNull(), anyOrNull())
    }

    private fun setupObserversAndStart() {
        viewModel.updateFollowUiState.observeForever {
            uiState = it
        }

        viewModel.start(blogId, postId, READER_THREADED_COMMENTS)
    }
}
