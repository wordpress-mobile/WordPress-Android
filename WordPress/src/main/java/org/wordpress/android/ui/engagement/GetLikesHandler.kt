package org.wordpress.android.ui.engagement

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.engagement.GetLikesUseCase.FailureType.NO_NETWORK
import org.wordpress.android.ui.engagement.GetLikesUseCase.GetLikesState
import org.wordpress.android.ui.engagement.GetLikesUseCase.GetLikesState.Failure
import org.wordpress.android.ui.engagement.GetLikesUseCase.GetLikesState.InitialLoading
import org.wordpress.android.ui.engagement.GetLikesUseCase.GetLikesState.LikesData
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject
import javax.inject.Named

class GetLikesHandler @Inject constructor(
    private val getLikesUseCase: GetLikesUseCase,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) {
    private val _snackbarEvents = MediatorLiveData<Event<SnackbarMessageHolder>>()
    val snackbarEvents: LiveData<Event<SnackbarMessageHolder>> = _snackbarEvents

    private val _likesStatusUpdate = MediatorLiveData<GetLikesState>()
    val likesStatusUpdate: LiveData<GetLikesState> = _likesStatusUpdate

    suspend fun handleGetLikesForPost(siteId: Long, postId: Long) {
        getLikesUseCase.getLikesForPost(siteId, postId).flowOn(bgDispatcher).collect { state ->
            manageState(state)
        }
    }

    suspend fun handleGetLikesForComment(siteId: Long, commentId: Long) {
        getLikesUseCase.getLikesForComment(siteId, commentId).flowOn(bgDispatcher).collect { state ->
            manageState(state)
        }
    }

    fun clear() {
        getLikesUseCase.clear()
    }

    private fun manageState(state: GetLikesState) {
        when (state) {
            InitialLoading,
            is LikesData -> _likesStatusUpdate.postValue(state)
            is Failure -> {
                _likesStatusUpdate.postValue(state)
                if (state.failureType != NO_NETWORK || !state.emptyStateData.showEmptyState) {
                    _snackbarEvents.postValue(Event(SnackbarMessageHolder(state.error)))
                }
            }
        }
    }
}
