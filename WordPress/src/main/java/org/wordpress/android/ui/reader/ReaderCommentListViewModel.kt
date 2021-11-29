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

@Suppress("TooManyFunctions")
class ReaderCommentListViewModel
@Inject constructor(
    private val followCommentsHandler: ReaderFollowCommentsHandler,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) : ScopedViewModel(mainDispatcher) {
    private var isStarted = false
    private var followStatusGetJob: Job? = null
    private var followStatusSetJob: Job? = null

    private val _showBottomSheetEvent = MutableLiveData<Event<ShowBottomSheetData>>()
    val showBottomSheetEvent: LiveData<Event<ShowBottomSheetData>> = _showBottomSheetEvent

    private val _snackbarEvents = MediatorLiveData<Event<SnackbarMessageHolder>>()
    val snackbarEvents: LiveData<Event<SnackbarMessageHolder>> = _snackbarEvents

    private val _updateFollowStatus = MediatorLiveData<FollowCommentsState>()
    val updateFollowUiState: LiveData<FollowCommentsUiState> =
            _updateFollowStatus.map { state -> buildFollowCommentsUiState(state) }

    private val _pushNotificationsStatusUpdate = MediatorLiveData<FollowStateChanged>()
    val pushNotificationsStatusUpdate: LiveData<Event<Boolean>> =
            _pushNotificationsStatusUpdate.map { state -> buildPushNotificationsUiState(state) }

    private val _scrollTo = MutableLiveData<Event<ScrollPosition>>()
    val scrollTo: LiveData<Event<ScrollPosition>> = _scrollTo.distinct()
    private var blogId: Long = 0
    private var postId: Long = 0

    private var scrollJob: Job? = null

    data class ShowBottomSheetData(val show: Boolean, val isReceivingNotifications: Boolean = false)

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

    fun onFollowTapped() {
        onFollowConversationClicked(true)
    }

    fun onUnfollowTapped() {
        onFollowConversationClicked(false)
        _showBottomSheetEvent.value = Event(ShowBottomSheetData(false))
    }

    fun onChangePushNotificationsRequest(enable: Boolean, fromSnackbar: Boolean = false) {
        followStatusSetJob?.cancel()
        followStatusSetJob = launch(bgDispatcher) {
            followCommentsHandler.handleEnableByPushNotificationsClicked(
                    blogId,
                    postId,
                    enable,
                    getSnackbarAction(fromSnackbar, enable)
            )
        }
    }

    private fun getSnackbarAction(fromSnackbar: Boolean, askingEnable: Boolean): (() -> Unit)? {
        return if (fromSnackbar) {
            if (askingEnable) {
                ::disablePushNotificationsFromSnackbarAction
            } else {
                ::enablePushNotificationsFromSnackbarAction
            }
        } else {
            null
        }
    }

    private fun enablePushNotificationsFromSnackbarAction() {
        onChangePushNotificationsRequest(true, true)
    }

    private fun disablePushNotificationsFromSnackbarAction() {
        onChangePushNotificationsRequest(false, true)
    }

    fun onManageNotificationsTapped() {
        val currentState = updateFollowUiState.value
        val currentPushNotificationsValue = currentState?.isReceivingNotifications ?: false
        _showBottomSheetEvent.value = Event(ShowBottomSheetData(true, currentPushNotificationsValue))
    }

    private fun init() {
        _snackbarEvents.addSource(followCommentsHandler.snackbarEvents) { event ->
            _snackbarEvents.value = event
        }

        _updateFollowStatus.addSource(followCommentsHandler.followStatusUpdate) { event ->
            _updateFollowStatus.value = event
        }

        _pushNotificationsStatusUpdate.addSource(followCommentsHandler.pushNotificationsStatusUpdate) { event ->
            _pushNotificationsStatusUpdate.value = event
        }

        getFollowConversationStatus(blogId, postId, true)
    }

    private fun onFollowConversationClicked(askSubscribe: Boolean) {
        followStatusSetJob?.cancel()
        followStatusSetJob = launch(bgDispatcher) {
            followCommentsHandler.handleFollowCommentsClicked(
                    blogId,
                    postId,
                    askSubscribe,
                    if (askSubscribe) ::enablePushNotificationsFromSnackbarAction else null
            )
        }
    }

    private fun getFollowConversationStatus(blogId: Long, postId: Long, isInit: Boolean) {
        followStatusGetJob?.cancel()
        followStatusGetJob = launch(bgDispatcher) {
            followCommentsHandler.handleFollowCommentsStatusRequest(blogId, postId, isInit)
        }
    }

    private fun mapToStateType(followCommentsState: FollowCommentsState) = when (followCommentsState) {
        Loading -> LOADING
        is FollowStateChanged -> VISIBLE_WITH_STATE
        is Failure, FollowCommentsNotAllowed -> DISABLED
        UserNotAuthenticated -> GONE
    }

    private fun buildFollowCommentsUiState(followCommentsState: FollowCommentsState): FollowCommentsUiState {
        val stateType = mapToStateType(followCommentsState)
        val isFollowing = if (followCommentsState is FollowStateChanged) followCommentsState.isFollowing else false

        return FollowCommentsUiState(
                type = stateType,
                showFollowButton = followCommentsState !is UserNotAuthenticated,
                isFollowing = isFollowing,
                animate = if (followCommentsState is FollowStateChanged) !followCommentsState.isInit else false,
                onFollowButtonClick = if (followCommentsState !is UserNotAuthenticated) {
                    ::onFollowConversationClicked
                } else {
                    null
                },
                isReceivingNotifications = if (followCommentsState is FollowStateChanged) {
                    followCommentsState.isReceivingNotifications
                } else {
                    false
                },
                isMenuEnabled = stateType != DISABLED && stateType != LOADING,
                showMenuShimmer = stateType == LOADING,
                isBellMenuVisible = if (stateType != VISIBLE_WITH_STATE) false else isFollowing,
                isFollowMenuVisible = when (stateType) {
                    DISABLED, LOADING -> true
                    GONE -> false
                    VISIBLE_WITH_STATE -> !isFollowing
                },
                onFollowTapped = if (listOf(DISABLED, LOADING).contains(stateType)) null else ::onFollowTapped,
                onManageNotificationsTapped = ::onManageNotificationsTapped
        )
    }

    private fun buildPushNotificationsUiState(followStateChanged: FollowStateChanged): Event<Boolean> {
        return Event(followStateChanged.isReceivingNotifications)
    }

    override fun onCleared() {
        super.onCleared()
        followStatusGetJob?.cancel()
        followStatusSetJob?.cancel()
    }
}
