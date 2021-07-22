package org.wordpress.android.models.usecases

import kotlinx.coroutines.flow.MutableSharedFlow
import org.wordpress.android.fluxc.model.CommentStatus
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.CommentStore.CommentError
import org.wordpress.android.fluxc.store.comments.CommentsStore
import org.wordpress.android.models.usecases.BatchModerateCommentsUseCase.ModerateCommentsAction
import org.wordpress.android.models.usecases.BatchModerateCommentsUseCase.ModerateCommentsAction.ModerateComment
import org.wordpress.android.models.usecases.BatchModerateCommentsUseCase.ModerateCommentsState.Idle
import org.wordpress.android.models.usecases.BatchModerateCommentsUseCase.ModerateCommentParameters
import org.wordpress.android.models.usecases.BatchModerateCommentsUseCase.BatchModerationState
import org.wordpress.android.models.usecases.BatchModerateCommentsUseCase.BatchModerationState.Failure
import org.wordpress.android.models.usecases.BatchModerateCommentsUseCase.BatchModerationState.InProgress
import org.wordpress.android.models.usecases.BatchModerateCommentsUseCase.BatchModerationState.Success
import org.wordpress.android.usecase.FlowFSMUseCase
import javax.inject.Inject

class BatchModerateCommentsUseCase @Inject constructor(
    private val commentsStore: CommentsStore
) : FlowFSMUseCase<ModerateCommentParameters, ModerateCommentsAction, BatchModerationState>(initialState = Idle) {
    override suspend fun runLogic(parameters: ModerateCommentParameters) {
        manageAction(ModerateComment(parameters, commentsStore))
    }

    sealed class ModerateCommentsState : StateInterface<ModerateCommentsAction, BatchModerationState> {
        object Idle : ModerateCommentsState() {
            override suspend fun runAction(
                action: ModerateCommentsAction,
                flowChannel: MutableSharedFlow<BatchModerationState>
            ): StateInterface<ModerateCommentsAction, BatchModerationState> {
                return when (action) {
                    is ModerateComment -> {
                        val parameters = action.moderateCommentParameters
                        val commentsStore = action.commentsStore
                        flowChannel.emit(InProgress)

                        parameters.remoteCommentIds.forEach {
                            val localModerationResult = commentsStore.moderateCommentLocally(
                                    site = parameters.site,
                                    remoteCommentId = it,
                                    newStatus = parameters.newStatus
                            )

                            if (localModerationResult.isError) {
                                flowChannel.emit(Failure(localModerationResult.error))
                            }

                            val result = commentsStore.pushComment(
                                    site = parameters.site,
                                    remoteCommentId = it
                            )

                            if (result.isError) {
//                                flowChannel.emit(Failure(result.error))
                            }
                        }
                        flowChannel.emit(Success)
                        Idle
                    }
                }
            }
        }
    }

    sealed class BatchModerationState {
        object InProgress : BatchModerationState()
        data class Failure(val error: CommentError) : BatchModerationState()
        object Success : BatchModerationState()
    }

    sealed class ModerateCommentsAction {
        data class ModerateComment(
            val moderateCommentParameters: ModerateCommentParameters,
            val commentsStore: CommentsStore
        ) :
                ModerateCommentsAction()
    }

    data class ModerateCommentParameters(
        val site: SiteModel,
        val remoteCommentIds: List<Long>,
        val newStatus: CommentStatus,
    )
}
