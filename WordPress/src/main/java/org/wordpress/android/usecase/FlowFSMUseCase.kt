package org.wordpress.android.usecase

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

abstract class FlowFSMUseCase<PARAMETERS, ACTION, RESULT_DATA>(
    initialState: StateInterface<ACTION, RESULT_DATA>
) {
    private val _flowChannel = MutableSharedFlow<RESULT_DATA>()
    private var _internalState: StateInterface<ACTION, RESULT_DATA> = initialState

    fun subscribe(): SharedFlow<RESULT_DATA> {
        return _flowChannel.asSharedFlow()
    }

    protected abstract suspend fun runLogic(parameters: PARAMETERS)

    suspend fun manageAction(action: ACTION) {
        _internalState = _internalState.runAction(action, _flowChannel)
    }

    interface StateInterface<ACTION, RESULT> {
        suspend fun runAction(
            action: ACTION,
            flowChannel: MutableSharedFlow<RESULT>
        ): StateInterface<ACTION, RESULT>
    }
}
