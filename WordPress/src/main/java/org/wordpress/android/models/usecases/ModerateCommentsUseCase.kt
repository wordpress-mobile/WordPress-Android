package org.wordpress.android.models.usecases

import kotlinx.coroutines.flow.MutableSharedFlow
import org.wordpress.android.fluxc.model.CommentStatus
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.CommentStore.CommentError
import org.wordpress.android.fluxc.store.comments.CommentsStore
import org.wordpress.android.models.usecases.ModerateCommentsUseCase.ModerateCommentsAction
import org.wordpress.android.models.usecases.ModerateCommentsUseCase.ModerateCommentsAction.ModerateComment
import org.wordpress.android.models.usecases.ModerateCommentsUseCase.ModerateCommentsState.Idle
import org.wordpress.android.models.usecases.ModerateCommentsUseCase.ModerateCommentParameters
import org.wordpress.android.models.usecases.ModerateCommentsUseCase.ModerationResult
import org.wordpress.android.models.usecases.ModerateCommentsUseCase.ModerationResult.ModerationFailure
import org.wordpress.android.models.usecases.ModerateCommentsUseCase.ModerationResult.ModerationInProgress
import org.wordpress.android.models.usecases.ModerateCommentsUseCase.ModerationResult.ModerationSuccess
import org.wordpress.android.usecase.FlowFSMUseCase
import javax.inject.Inject

class ModerateCommentsUseCase @Inject constructor(
    private val commentsStore: CommentsStore
) : FlowFSMUseCase<ModerateCommentParameters, ModerateCommentsAction, ModerationResult>(initialState = Idle) {
    override suspend fun runLogic(parameters: ModerateCommentParameters) {
        manageAction(ModerateComment(parameters, commentsStore))
    }

    sealed class ModerateCommentsState : StateInterface<ModerateCommentsAction, ModerationResult> {
        object Idle : ModerateCommentsState() {
            override suspend fun runAction(
                action: ModerateCommentsAction,
                flowChannel: MutableSharedFlow<ModerationResult>
            ): StateInterface<ModerateCommentsAction, ModerationResult> {
                return when (action) {
                    is ModerateComment -> {
                        val parameters = action.moderateCommentParameters
                        val commentsStore = action.commentsStore
                        flowChannel.emit(ModerationInProgress)

                        parameters.remoteCommentIds.forEach {
                            val localModerationResult = commentsStore.moderateCommentLocally(
                                    site = parameters.site,
                                    remoteCommentId = it,
                                    newStatus = parameters.newStatus
                            )

                            if (localModerationResult.isError) {
                                flowChannel.emit(ModerationFailure(localModerationResult.error))
                                return Idle
                            }

                            val result = commentsStore.pushComment(
                                    site = parameters.site,
                                    remoteCommentId = it
                            )

                            if (result.isError) {
                                flowChannel.emit(ModerationFailure(result.error))
                                return Idle
                            }
                        }
                        flowChannel.emit(ModerationSuccess)
                        Idle
                    }
                }
            }
        }
    }

    sealed class ModerationResult {
        object ModerationInProgress : ModerationResult()
        data class ModerationFailure(val error: CommentError) : ModerationResult()
        object ModerationSuccess : ModerationResult()
        object Idle : ModerationResult()
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
