package org.wordpress.android.usecase

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

abstract class FlowFSMUseCase<RESOURCE_PROVIDER, in RUN_LOGIC_PARAMETERS, ACTION, DATA, USE_CASE_TYPE>(
    initialState: StateInterface<RESOURCE_PROVIDER, ACTION, DATA, USE_CASE_TYPE>,
    val resourceProvider: RESOURCE_PROVIDER
) {
    private val _flowChannel = MutableSharedFlow<UseCaseResult<DATA, USE_CASE_TYPE>>()
    private var _internalState: StateInterface<RESOURCE_PROVIDER, ACTION, DATA, USE_CASE_TYPE> = initialState


    fun subscribe(): SharedFlow<UseCaseResult<DATA, USE_CASE_TYPE>> {
        return _flowChannel.asSharedFlow()
    }

    protected abstract suspend fun runLogic(parameters: RUN_LOGIC_PARAMETERS)

    suspend fun manageAction(action: ACTION) {
        _internalState = _internalState.runAction(resourceProvider, action, _flowChannel)
    }

    interface StateInterface<RESOURCE_PROVIDER, TRANSITION_ACTION, RESULT, USE_CASE_TYPE> {
        suspend fun runAction(
            utilsProvider: RESOURCE_PROVIDER,
            action: TRANSITION_ACTION,
            flowChannel: MutableSharedFlow<UseCaseResult<RESULT, USE_CASE_TYPE>>
        ): StateInterface<RESOURCE_PROVIDER, TRANSITION_ACTION, RESULT, USE_CASE_TYPE>
    }
}
