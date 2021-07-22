package org.wordpress.android.models.usecases

import kotlinx.coroutines.flow.MutableSharedFlow
import org.wordpress.android.models.usecases.PropagateCommentsUpdateUseCase.CommentsUpdateState.Idle
import org.wordpress.android.models.usecases.PropagateCommentsUpdateUseCase.PropagateCommentsUpdateAction
import org.wordpress.android.models.usecases.PropagateCommentsUpdateUseCase.PropagateCommentsUpdateAction.UpdatedComments
import org.wordpress.android.models.usecases.PropagateCommentsUpdateUseCase.PropagateCommentsUpdateResult.PropagateCommentsUpdateSuccess
import org.wordpress.android.usecase.FlowFSMUseCase
import javax.inject.Inject

class PropagateCommentsUpdateUseCase @Inject constructor() : FlowFSMUseCase<Any, PropagateCommentsUpdateAction, PropagateCommentsUpdateSuccess>(
        initialState = Idle
) {
    override suspend fun runLogic(parameters: Any) {
        manageAction(UpdatedComments)
    }

    sealed class CommentsUpdateState : StateInterface<PropagateCommentsUpdateAction, PropagateCommentsUpdateSuccess> {
        object Idle : CommentsUpdateState() {
            override suspend fun runAction(
                action: PropagateCommentsUpdateAction,
                flowChannel: MutableSharedFlow<PropagateCommentsUpdateSuccess>
            ): StateInterface<PropagateCommentsUpdateAction, PropagateCommentsUpdateSuccess> {
                return when (action) {
                    is UpdatedComments -> {
                        flowChannel.emit(PropagateCommentsUpdateSuccess)
                        Idle
                    }
                }
            }
        }
    }

    sealed class PropagateCommentsUpdateResult {
        object PropagateCommentsUpdateSuccess : PropagateCommentsUpdateResult()
    }

    sealed class PropagateCommentsUpdateAction {
        object UpdatedComments : PropagateCommentsUpdateAction()
    }
}
