package org.wordpress.android.ui.people

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.flowOn
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.people.InviteLinksUseCase.InviteLinksState
import org.wordpress.android.ui.people.InviteLinksUseCase.InviteLinksState.InviteLinksData
import org.wordpress.android.ui.people.InviteLinksUseCase.InviteLinksState.InviteLinksError
import org.wordpress.android.ui.people.InviteLinksUseCase.InviteLinksState.InviteLinksLoading
import org.wordpress.android.ui.people.InviteLinksUseCase.InviteLinksState.InviteLinksNotAllowed
import org.wordpress.android.ui.people.InviteLinksUseCase.InviteLinksState.InviteLinksUserChangedRole
import org.wordpress.android.ui.people.InviteLinksUseCase.InviteLinksState.UserNotAuthenticated
import org.wordpress.android.ui.people.InviteLinksUseCase.UseCaseScenarioContext
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject
import javax.inject.Named

class InviteLinksHandler @Inject constructor(
    private val inviteLinksUseCase: InviteLinksUseCase,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) {
    private val _snackbarEvents = MediatorLiveData<Event<SnackbarMessageHolder>>()
    val snackbarEvents: LiveData<Event<SnackbarMessageHolder>> = _snackbarEvents

    private val _inviteLinksState = MediatorLiveData<InviteLinksState>()
    val inviteLinksState: LiveData<InviteLinksState> = _inviteLinksState

    suspend fun handleInviteLinksStatusRequest(blogId: Long, scenarioContext: UseCaseScenarioContext) {
        inviteLinksUseCase.getInviteLinksStatus(blogId, scenarioContext)
            .flowOn(bgDispatcher).collect { state ->
                manageState(state)
            }
    }

    suspend fun handleGenerateLinks(blogId: Long) {
        inviteLinksUseCase.generateLinks(blogId)
            .flowOn(bgDispatcher).collect { state ->
                manageState(state)
            }
    }

    suspend fun handleDisableLinks(blogId: Long) {
        inviteLinksUseCase.disableLinks(blogId)
            .flowOn(bgDispatcher).collect { state ->
                manageState(state)
            }
    }

    private fun manageState(state: InviteLinksState) {
        when (state) {
            InviteLinksNotAllowed,
            UserNotAuthenticated,
            is InviteLinksData,
            is InviteLinksLoading -> {
                _inviteLinksState.postValue(state)
            }
            is InviteLinksError -> {
                _inviteLinksState.postValue(state)
                _snackbarEvents.postValue(Event(SnackbarMessageHolder(state.error)))
            }
            is InviteLinksUserChangedRole -> {}
        }
    }
}
