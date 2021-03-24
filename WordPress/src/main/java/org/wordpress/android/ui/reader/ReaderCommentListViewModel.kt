package org.wordpress.android.ui.reader

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.reader.FollowCommentsUiStateType.DISABLED
import org.wordpress.android.ui.reader.FollowCommentsUiStateType.GONE
import org.wordpress.android.ui.reader.FollowCommentsUiStateType.LOADING
import org.wordpress.android.ui.reader.FollowCommentsUiStateType.VISIBLE_WITH_STATE
import org.wordpress.android.ui.reader.usecases.ReaderCommentsFollowUseCase.FollowCommentsState
import org.wordpress.android.ui.reader.usecases.ReaderCommentsFollowUseCase.FollowCommentsState.Failure
import org.wordpress.android.ui.reader.usecases.ReaderCommentsFollowUseCase.FollowCommentsState.FollowCommentsNotAllowed
import org.wordpress.android.ui.reader.usecases.ReaderCommentsFollowUseCase.FollowCommentsState.FollowStateChanged
import org.wordpress.android.ui.reader.usecases.ReaderCommentsFollowUseCase.FollowCommentsState.Loading
import org.wordpress.android.ui.reader.usecases.ReaderCommentsFollowUseCase.FollowCommentsState.UserNotAuthenticated
import org.wordpress.android.util.distinct
import org.wordpress.android.util.map
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

class ReaderCommentListViewModel
@Inject constructor(
    private val followCommentsHandler: ReaderFollowCommentsHandler,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) : ScopedViewModel(mainDispatcher) {
    private var isStarted = false
    private var followStatusGetJob: Job? = null
    private var followStatusSetJob: Job? = null

    private val _snackbarEvents = MediatorLiveData<Event<SnackbarMessageHolder>>()
    val snackbarEvents: LiveData<Event<SnackbarMessageHolder>> = _snackbarEvents

    private val _updateFollowStatus = MediatorLiveData<FollowCommentsState>()
    val updateFollowUiState: LiveData<FollowCommentsUiState> =
            _updateFollowStatus.map { state -> buildFollowCommentsUiState(state) }

    private val _scrollTo = MutableLiveData<Event<ScrollPosition>>()
    val scrollTo: LiveData<Event<ScrollPosition>> = _scrollTo.distinct()
    private var blogId: Long = 0
    private var postId: Long = 0

    private var scrollJob: Job? = null

    fun scrollToPosition(position: Int, isSmooth: Boolean) {
        scrollJob?.cancel()
        scrollJob = launch {
            delay(300)
            _scrollTo.postValue(Event(ScrollPosition(position, isSmooth)))
        }
    }

    data class ScrollPosition(val position: Int, val isSmooth: Boolean)

    fun start(blogId: Long, postId: Long) {
        if (isStarted) return
        isStarted = true

        this.blogId = blogId
        this.postId = postId

        _updateFollowStatus.value = FollowCommentsNotAllowed

        init()
    }

    fun onSwipeToRefresh() {
        getFollowConversationStatus(blogId, postId, false)
    }

    private fun init() {
        _snackbarEvents.addSource(followCommentsHandler.snackbarEvents) { event ->
            _snackbarEvents.value = event
        }

        _updateFollowStatus.addSource(followCommentsHandler.followStatusUpdate) { event ->
            _updateFollowStatus.value = event
        }

        getFollowConversationStatus(blogId, postId, true)
    }

    private fun onFollowConversationClicked(askSubscribe: Boolean) {
        followStatusSetJob?.cancel()
        followStatusSetJob = launch(bgDispatcher) {
            followCommentsHandler.handleFollowCommentsClicked(blogId, postId, askSubscribe)
        }
    }

    private fun getFollowConversationStatus(blogId: Long, postId: Long, isInit: Boolean) {
        followStatusGetJob?.cancel()
        followStatusGetJob = launch(bgDispatcher) {
            followCommentsHandler.handleFollowCommentsStatusRequest(blogId, postId, isInit)
        }
    }

    private fun buildFollowCommentsUiState(followCommentsState: FollowCommentsState): FollowCommentsUiState {
        return FollowCommentsUiState(
                    type = when (followCommentsState) {
                        Loading -> LOADING
                        is FollowStateChanged -> VISIBLE_WITH_STATE
                        is Failure, FollowCommentsNotAllowed -> DISABLED
                        UserNotAuthenticated -> GONE
                    },
                    showFollowButton = followCommentsState !is UserNotAuthenticated,
                    isFollowing = if (followCommentsState is FollowStateChanged) {
                        followCommentsState.isFollowing
                    } else {
                        false
                    },
                    animate = if (followCommentsState is FollowStateChanged) {
                        !followCommentsState.isInit
                    } else {
                        false
                    },
                    onFollowButtonClick = if (followCommentsState !is UserNotAuthenticated) {
                        ::onFollowConversationClicked
                    } else {
                        null
                    }
        )
    }

    override fun onCleared() {
        super.onCleared()
        followStatusGetJob?.cancel()
        followStatusSetJob?.cancel()
    }
}
