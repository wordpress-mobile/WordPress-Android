package org.wordpress.android.models.usecases

import kotlinx.coroutines.flow.MutableSharedFlow
import org.wordpress.android.fluxc.store.CommentsStore.CommentsData.DontCare
import org.wordpress.android.models.usecases.LocalCommentCacheUpdateUseCase.CommentsUpdateState.Idle
import org.wordpress.android.models.usecases.LocalCommentCacheUpdateUseCase.PropagateCommentsUpdateAction
import org.wordpress.android.models.usecases.LocalCommentCacheUpdateUseCase.PropagateCommentsUpdateAction.UpdatedComments
import org.wordpress.android.usecase.FlowFSMUseCase
import org.wordpress.android.usecase.UseCaseResult
import org.wordpress.android.usecase.UseCaseResult.Success
import javax.inject.Inject

class LocalCommentCacheUpdateUseCase @Inject constructor() : FlowFSMUseCase<Any, Any, PropagateCommentsUpdateAction, Any, CommentsUseCaseType>(
        initialState = Idle,
        DontCare
) {
    override suspend fun runLogic(parameters: Any) {
        manageAction(UpdatedComments)
    }

    sealed class CommentsUpdateState : StateInterface<Any, PropagateCommentsUpdateAction, Any, CommentsUseCaseType> {
        object Idle : CommentsUpdateState() {
            override suspend fun runAction(
                utilsProvider: Any,
                action: PropagateCommentsUpdateAction,
                flowChannel: MutableSharedFlow<UseCaseResult<Any, CommentsUseCaseType>>
            ): StateInterface<Any, PropagateCommentsUpdateAction, Any, CommentsUseCaseType> {
                return when (action) {
                    is UpdatedComments -> {
                        flowChannel.emit(Success(CommentsUseCaseType.PAGINATE_USE_CASE, DontCare))
                        Idle
                    }
                }
            }
        }
    }

    sealed class PropagateCommentsUpdateAction {
        object UpdatedComments : PropagateCommentsUpdateAction()
    }
}
