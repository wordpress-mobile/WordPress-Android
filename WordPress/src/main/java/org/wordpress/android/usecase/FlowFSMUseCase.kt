package org.wordpress.android.usecase

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

open class FlowFSMUseCase<RESOURCE_PROVIDER, ACTION_TYPE, DATA, USE_CASE_TYPE, ERROR>(
    initialState: StateInterface<RESOURCE_PROVIDER, ACTION_TYPE, DATA, USE_CASE_TYPE, ERROR>,
    val resourceProvider: RESOURCE_PROVIDER
) {
    private val _flowChannel = MutableSharedFlow<UseCaseResult<USE_CASE_TYPE, ERROR, DATA>>()
    private var _internalState: StateInterface<RESOURCE_PROVIDER, ACTION_TYPE, DATA, USE_CASE_TYPE, ERROR> =
        initialState

    fun subscribe(): SharedFlow<UseCaseResult<USE_CASE_TYPE, ERROR, DATA>> {
        return _flowChannel.asSharedFlow()
    }

    suspend fun manageAction(action: ACTION_TYPE) {
        _internalState = _internalState.runAction(resourceProvider, action, _flowChannel)
    }

    interface StateInterface<RESOURCE_PROVIDER, TRANSITION_ACTION, RESULT, USE_CASE_TYPE, ERROR> {
        suspend fun runAction(
            utilsProvider: RESOURCE_PROVIDER,
            action: TRANSITION_ACTION,
            flowChannel: MutableSharedFlow<UseCaseResult<USE_CASE_TYPE, ERROR, RESULT>>
        ): StateInterface<RESOURCE_PROVIDER, TRANSITION_ACTION, RESULT, USE_CASE_TYPE, ERROR>
    }
}
