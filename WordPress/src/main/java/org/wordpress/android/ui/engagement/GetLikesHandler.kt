package org.wordpress.android.ui.engagement

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.flowOn
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.engagement.GetLikesUseCase.FailureType.NO_NETWORK
import org.wordpress.android.ui.engagement.GetLikesUseCase.GetLikesState
import org.wordpress.android.ui.engagement.GetLikesUseCase.GetLikesState.Failure
import org.wordpress.android.ui.engagement.GetLikesUseCase.GetLikesState.LikesData
import org.wordpress.android.ui.engagement.GetLikesUseCase.GetLikesState.Loading
import org.wordpress.android.ui.engagement.GetLikesUseCase.LikeGroupFingerPrint
import org.wordpress.android.ui.engagement.GetLikesUseCase.PaginationParams
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

    suspend fun handleGetLikesForPost(
        fingerPrint: LikeGroupFingerPrint,
        requestNextPage: Boolean,
        pageLength: Int = LIKES_PER_PAGE_DEFAULT
    ) {
        // Safety net in case page length is computed rather than a constant in the future.
        require(pageLength != 0) { "The page length for likes cannot be 0." }
        getLikesUseCase.getLikesForPost(
            fingerPrint,
            PaginationParams(
                requestNextPage,
                pageLength
            )
        ).flowOn(bgDispatcher).collect { state ->
            manageState(state)
        }
    }

    suspend fun handleGetLikesForComment(
        fingerPrint: LikeGroupFingerPrint,
        requestNextPage: Boolean,
        pageLength: Int = LIKES_PER_PAGE_DEFAULT
    ) {
        // Safety net in case page length is computed rather than a constant in the future.
        require(pageLength != 0) { "The page length for likes cannot be 0" }
        getLikesUseCase.getLikesForComment(
            fingerPrint,
            PaginationParams(
                requestNextPage,
                pageLength
            )
        ).flowOn(bgDispatcher).collect { state ->
            manageState(state)
        }
    }

    fun clear() {
        getLikesUseCase.clear()
    }

    private fun manageState(state: GetLikesState) {
        when (state) {
            Loading,
            is LikesData -> {
                _likesStatusUpdate.postValue(state)
            }
            is Failure -> {
                _likesStatusUpdate.postValue(state)
                if (state.failureType != NO_NETWORK || !state.emptyStateData.showEmptyState) {
                    _snackbarEvents.postValue(Event(SnackbarMessageHolder(state.error)))
                }
            }
        }
    }

    companion object {
        private const val LIKES_PER_PAGE_DEFAULT = 20
    }
}
