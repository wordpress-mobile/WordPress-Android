package org.wordpress.android.ui.reader

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.reader.usecases.ReaderCommentsFollowUseCase
import org.wordpress.android.ui.reader.usecases.ReaderCommentsFollowUseCase.FollowCommentsState
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject
import javax.inject.Named
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import org.wordpress.android.R
import org.wordpress.android.ui.reader.comments.ThreadedCommentsActionSource
import org.wordpress.android.ui.utils.UiString.UiStringRes

class ReaderFollowCommentsHandler @Inject constructor(
    private val readerCommentsFollowUseCase: ReaderCommentsFollowUseCase,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) {
    private val _snackbarEvents = MediatorLiveData<Event<SnackbarMessageHolder>>()
    val snackbarEvents: LiveData<Event<SnackbarMessageHolder>> = _snackbarEvents

    private val _followStatusUpdate = MediatorLiveData<FollowCommentsState>()
    val followStatusUpdate: LiveData<FollowCommentsState> = _followStatusUpdate

    private val _pushNotificationsStatusUpdate = MediatorLiveData<FollowCommentsState.FollowStateChanged>()
    val pushNotificationsStatusUpdate: LiveData<FollowCommentsState.FollowStateChanged> = _pushNotificationsStatusUpdate

    suspend fun handleFollowCommentsClicked(
        blogId: Long,
        postId: Long,
        askSubscribe: Boolean,
        source: ThreadedCommentsActionSource,
        onSuccessSnackbarAction: (() -> Unit)?
    ) {
        readerCommentsFollowUseCase.setMySubscriptionToPost(blogId, postId, askSubscribe, source)
                .flowOn(bgDispatcher).collect { state ->
            manageState(state, onSuccessSnackbarAction)
        }
    }

    suspend fun handleFollowCommentsStatusRequest(blogId: Long, postId: Long, isInit: Boolean) {
        readerCommentsFollowUseCase.getMySubscriptionToPost(blogId, postId, isInit)
                .flowOn(bgDispatcher).collect { state ->
            manageState(state)
        }
    }

    suspend fun handleEnableByPushNotificationsClicked(
        blogId: Long,
        postId: Long,
        askEnable: Boolean,
        source: ThreadedCommentsActionSource,
        onSuccessSnackbarAction: (() -> Unit)? = null
    ) {
        readerCommentsFollowUseCase.setEnableByPushNotifications(blogId, postId, askEnable, source)
                .flowOn(bgDispatcher).collect { state ->
                    manageState(state, onSuccessSnackbarAction)
                }
    }

    private fun manageState(state: FollowCommentsState, onSuccessSnackbarAction: (() -> Unit)? = null) {
        when (state) {
            is FollowCommentsState.FollowStateChanged -> {
                _followStatusUpdate.postValue(state)
                if (state.forcePushNotificationsUpdate) {
                    _pushNotificationsStatusUpdate.postValue(state)
                }
                state.userMessage?.let {
                    _snackbarEvents.postValue(Event(SnackbarMessageHolder(
                            message = it,
                            buttonTitle = onSuccessSnackbarAction?.let {
                                if (state.isReceivingNotifications) {
                                    UiStringRes(R.string.undo)
                                } else {
                                    UiStringRes(R.string.reader_followed_blog_notifications_action)
                                }
                            },
                            buttonAction = onSuccessSnackbarAction ?: {}
                    )))
                }
            }
            is FollowCommentsState.Failure -> {
                _followStatusUpdate.postValue(state)
                _snackbarEvents.postValue(Event(SnackbarMessageHolder(state.error)))
            }
            is FollowCommentsState.Loading -> {
                _followStatusUpdate.postValue(state)
            }
            is FollowCommentsState.FollowCommentsNotAllowed -> {
                _followStatusUpdate.postValue(state)
            }
            is FollowCommentsState.UserNotAuthenticated -> {
                _followStatusUpdate.postValue(state)
            }
            is FollowCommentsState.FlagsMappedState -> Unit // Do nothing
        }
    }
}
