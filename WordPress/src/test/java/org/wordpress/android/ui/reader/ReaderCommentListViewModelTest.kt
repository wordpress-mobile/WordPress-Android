package org.wordpress.android.ui.reader

import androidx.lifecycle.MutableLiveData
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.eq
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
import org.wordpress.android.test
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.reader.FollowCommentsUiStateType.VISIBLE_WITH_STATE
import org.wordpress.android.ui.reader.ReaderCommentListViewModel.ScrollPosition
import org.wordpress.android.ui.reader.usecases.ReaderCommentsFollowUseCase.FollowCommentsState
import org.wordpress.android.ui.reader.usecases.ReaderCommentsFollowUseCase.FollowCommentsState.FollowStateChanged
import org.wordpress.android.viewmodel.Event

@InternalCoroutinesApi
class ReaderCommentListViewModelTest : BaseUnitTest() {
    @Mock lateinit var followCommentsHandler: ReaderFollowCommentsHandler

    private lateinit var viewModel: ReaderCommentListViewModel
    private val blogId = 100L
    private val postId = 1000L
    private var snackbarEvents = MutableLiveData<Event<SnackbarMessageHolder>>()
    private var followStatusUpdate = MutableLiveData<FollowCommentsState>()
    private var uiState: FollowCommentsUiState? = null

    @Before
    fun setUp() {
        whenever(followCommentsHandler.snackbarEvents).thenReturn(snackbarEvents)
        whenever(followCommentsHandler.followStatusUpdate).thenReturn(followStatusUpdate)

        viewModel = ReaderCommentListViewModel(
                followCommentsHandler,
                TEST_DISPATCHER,
                TEST_DISPATCHER
        )
    }

    @Test
    fun `emits scroll event on scroll`() {
        var scrollEvent: Event<ScrollPosition>? = null
        viewModel.scrollTo.observeForever {
            scrollEvent = it
        }

        val expectedPosition = 10
        val isSmooth = true

        viewModel.scrollToPosition(expectedPosition, isSmooth)

        val scrollPosition = scrollEvent?.getContentIfNotHandled()!!

        assertThat(scrollPosition.isSmooth).isEqualTo(isSmooth)
        assertThat(scrollPosition.position).isEqualTo(expectedPosition)
    }

    @Test
    fun `follow ui state is DISABLED on start`() {
        setupObserversAndStart()

        assertThat(uiState).isNotNull
        assertThat(uiState!!.type).isEqualTo(FollowCommentsUiStateType.DISABLED)
    }

    @Test
    fun `onSwipeToRefresh updates follow conversation status`() = test {
        var stateChanged = FollowStateChanged(blogId, postId, true, false, true)
        doAnswer {
            followStatusUpdate.postValue(stateChanged)
        }.whenever(followCommentsHandler).handleFollowCommentsStatusRequest(anyLong(), anyLong(), anyBoolean())

        setupObserversAndStart()

        requireNotNull(uiState).let {
            assertThat(it.type).isEqualTo(VISIBLE_WITH_STATE)
            assertThat(it.animate).isFalse()
        }

        stateChanged = FollowStateChanged(blogId, postId, true, false)
        doAnswer {
            followStatusUpdate.postValue(stateChanged)
        }.whenever(followCommentsHandler).handleFollowCommentsStatusRequest(anyLong(), anyLong(), anyBoolean())

        viewModel.onSwipeToRefresh()

        requireNotNull(uiState).let {
            assertThat(it.type).isEqualTo(VISIBLE_WITH_STATE)
            assertThat(it.animate).isTrue()
        }
    }

    @Test
    fun `onFollowConversationClicked toggles follow button status`() = test {
        var stateChanged = FollowStateChanged(blogId, postId, true, false)
        doAnswer {
            followStatusUpdate.postValue(stateChanged)
        }.whenever(followCommentsHandler).handleFollowCommentsStatusRequest(anyLong(), anyLong(), anyBoolean())

        doAnswer {
            stateChanged = FollowStateChanged(blogId, postId, false, false)
            followStatusUpdate.postValue(stateChanged)
        }.whenever(followCommentsHandler).handleFollowCommentsClicked(eq(blogId), eq(postId), eq(false), anyOrNull())

        setupObserversAndStart()

        requireNotNull(uiState).let {
            assertThat(it.type).isEqualTo(VISIBLE_WITH_STATE)
            assertThat(it.isFollowing).isTrue()
            it.onFollowButtonClick?.invoke(false)
        }

        requireNotNull(uiState).let {
            assertThat(it.type).isEqualTo(VISIBLE_WITH_STATE)
            assertThat(it.isFollowing).isFalse()
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
        }.whenever(followCommentsHandler).handleFollowCommentsClicked(eq(blogId), eq(postId), eq(true), anyOrNull())

        setupObserversAndStart()

        requireNotNull(uiState).let {
            assertThat(it.type).isEqualTo(VISIBLE_WITH_STATE)
            assertThat(it.isFollowing).isFalse()
            it.onFollowTapped?.invoke()
        }

        requireNotNull(uiState).let {
            assertThat(it.type).isEqualTo(VISIBLE_WITH_STATE)
            assertThat(it.isFollowing).isTrue()
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
        }.whenever(followCommentsHandler).handleFollowCommentsClicked(blogId, postId, false, null)

        setupObserversAndStart()

        requireNotNull(uiState).let {
            assertThat(it.type).isEqualTo(VISIBLE_WITH_STATE)
            assertThat(it.isFollowing).isTrue()
            viewModel.onUnfollowTapped()
        }

        requireNotNull(uiState).let {
            assertThat(it.type).isEqualTo(VISIBLE_WITH_STATE)
            assertThat(it.isFollowing).isFalse()
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
        }.whenever(followCommentsHandler).handleEnableByPushNotificationsClicked(blogId, postId, true, null)

        setupObserversAndStart()

        requireNotNull(uiState).let {
            assertThat(it.type).isEqualTo(VISIBLE_WITH_STATE)
            assertThat(it.isReceivingNotifications).isFalse()
            viewModel.onChangePushNotificationsRequest(true)
        }

        requireNotNull(uiState).let {
            assertThat(it.type).isEqualTo(VISIBLE_WITH_STATE)
            assertThat(it.isReceivingNotifications).isTrue()
        }
    }

    private fun setupObserversAndStart() {
        viewModel.updateFollowUiState.observeForever {
            uiState = it
        }

        viewModel.start(blogId, postId)
    }
}
