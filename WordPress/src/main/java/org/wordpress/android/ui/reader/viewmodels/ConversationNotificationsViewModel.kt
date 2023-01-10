package org.wordpress.android.ui.reader.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import org.wordpress.android.datasets.wrappers.ReaderPostTableWrapper
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.reader.FollowCommentsUiStateType.DISABLED
import org.wordpress.android.ui.reader.FollowCommentsUiStateType.GONE
import org.wordpress.android.ui.reader.FollowCommentsUiStateType.LOADING
import org.wordpress.android.ui.reader.FollowCommentsUiStateType.VISIBLE_WITH_STATE
import org.wordpress.android.ui.reader.FollowConversationStatusFlags
import org.wordpress.android.ui.reader.FollowConversationUiState
import org.wordpress.android.ui.reader.ReaderFollowCommentsHandler
import org.wordpress.android.ui.reader.comments.ThreadedCommentsActionSource
import org.wordpress.android.ui.reader.comments.ThreadedCommentsActionSource.UNKNOWN
import org.wordpress.android.ui.reader.usecases.ReaderCommentsFollowUseCase.FollowCommentsState
import org.wordpress.android.ui.reader.usecases.ReaderCommentsFollowUseCase.FollowCommentsState.Failure
import org.wordpress.android.ui.reader.usecases.ReaderCommentsFollowUseCase.FollowCommentsState.FlagsMappedState
import org.wordpress.android.ui.reader.usecases.ReaderCommentsFollowUseCase.FollowCommentsState.FollowCommentsNotAllowed
import org.wordpress.android.ui.reader.usecases.ReaderCommentsFollowUseCase.FollowCommentsState.FollowStateChanged
import org.wordpress.android.ui.reader.usecases.ReaderCommentsFollowUseCase.FollowCommentsState.Loading
import org.wordpress.android.ui.reader.usecases.ReaderCommentsFollowUseCase.FollowCommentsState.UserNotAuthenticated
import org.wordpress.android.util.map
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

class ConversationNotificationsViewModel @Inject constructor(
    private val followCommentsHandler: ReaderFollowCommentsHandler,
    private val readerPostTableWrapper: ReaderPostTableWrapper,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) : ScopedViewModel(bgDispatcher) {
    private var isStarted = false
    private var followStatusGetJob: Job? = null
    private var followStatusSetJob: Job? = null

    private val _showBottomSheetEvent = MutableLiveData<Event<ShowBottomSheetData>>()
    val showBottomSheetEvent: LiveData<Event<ShowBottomSheetData>> = _showBottomSheetEvent

    private val _snackbarEvents = MediatorLiveData<Event<SnackbarMessageHolder>>()
    val snackbarEvents: LiveData<Event<SnackbarMessageHolder>> = _snackbarEvents

    private val _updateFollowStatus = MediatorLiveData<FollowCommentsState>()
    val updateFollowUiState: LiveData<FollowConversationUiState> =
        _updateFollowStatus.map { state -> buildFollowCommentsUiState(state) }

    private val _pushNotificationsStatusUpdate = MediatorLiveData<FollowStateChanged>()
    val pushNotificationsStatusUpdate: LiveData<Event<Boolean>> =
        _pushNotificationsStatusUpdate.map { state -> buildPushNotificationsUiState(state) }

    private var blogId: Long = 0
    private var postId: Long = 0
    private var source: ThreadedCommentsActionSource = UNKNOWN

    data class ShowBottomSheetData(val show: Boolean, val isReceivingNotifications: Boolean = false)

    fun start(blogId: Long, postId: Long, source: ThreadedCommentsActionSource) {
        if (isStarted) return
        isStarted = true

        this.blogId = blogId
        this.postId = postId
        this.source = source

        _updateFollowStatus.value = FollowCommentsNotAllowed

        init()
    }

    fun onRefresh() {
        getFollowConversationStatus(false)
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
                source,
                getSnackbarAction(fromSnackbar, enable)
            )
        }
    }

    fun onManageNotificationsTapped() {
        val currentState = updateFollowUiState.value
        val currentPushNotificationsValue = currentState?.flags?.isReceivingNotifications ?: false
        _showBottomSheetEvent.value = Event(ShowBottomSheetData(true, currentPushNotificationsValue))
    }

    fun onUpdatePost(blogId: Long, postId: Long) {
        this.blogId = blogId
        this.postId = postId
    }

    fun onUserNavigateFromComments(flags: FollowConversationStatusFlags) {
        _updateFollowStatus.value = FlagsMappedState(flags)
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

        getFollowConversationStatus(true)
    }

    private fun onFollowConversationClicked(askSubscribe: Boolean) {
        followStatusSetJob?.cancel()
        followStatusSetJob = launch(bgDispatcher) {
            followCommentsHandler.handleFollowCommentsClicked(
                blogId,
                postId,
                askSubscribe,
                source,
                if (askSubscribe) ::enablePushNotificationsFromSnackbarAction else null
            )
        }
    }

    private fun getFollowConversationStatus(isInit: Boolean) {
        followStatusGetJob?.cancel()
        followStatusGetJob = launch(bgDispatcher) {
            val post = readerPostTableWrapper.getBlogPost(
                blogId,
                postId,
                true
            )

            post?.let {
                if (!post.isExternal) {
                    followCommentsHandler.handleFollowCommentsStatusRequest(blogId, postId, isInit)
                }
            }
        }
    }

    private fun mapToStateType(followCommentsState: FollowCommentsState) = when (followCommentsState) {
        Loading -> LOADING
        is FollowStateChanged -> VISIBLE_WITH_STATE
        is Failure, FollowCommentsNotAllowed -> DISABLED
        UserNotAuthenticated -> GONE
        is FlagsMappedState -> followCommentsState.flags.type
    }

    private fun buildFollowCommentsUiState(followCommentsState: FollowCommentsState): FollowConversationUiState {
        if (followCommentsState is FlagsMappedState) {
            return FollowConversationUiState(
                flags = followCommentsState.flags,
                onFollowTapped = if (listOf(DISABLED, LOADING).contains(followCommentsState.flags.type)) {
                    null
                } else {
                    ::onFollowTapped
                },
                onManageNotificationsTapped = ::onManageNotificationsTapped
            )
        } else {
            val stateType = mapToStateType(followCommentsState)
            val isFollowing = if (followCommentsState is FollowStateChanged) followCommentsState.isFollowing else false

            return FollowConversationUiState(
                FollowConversationStatusFlags(
                    type = stateType,
                    isFollowing = isFollowing,
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
                    }
                ),
                onFollowTapped = if (listOf(DISABLED, LOADING).contains(stateType)) null else ::onFollowTapped,
                onManageNotificationsTapped = ::onManageNotificationsTapped
            )
        }
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
