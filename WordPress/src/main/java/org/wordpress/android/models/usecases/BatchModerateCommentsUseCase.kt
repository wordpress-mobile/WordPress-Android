package org.wordpress.android.models.usecases

import kotlinx.coroutines.flow.MutableSharedFlow
import org.wordpress.android.fluxc.model.CommentStatus
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.CommentStore.CommentError
import org.wordpress.android.fluxc.store.CommentsStore.CommentsData.DontCare
import org.wordpress.android.models.usecases.CommentsUseCaseType.MODERATE_USE_CASE
import org.wordpress.android.models.usecases.BatchModerateCommentsUseCase.ModerateCommentsAction
import org.wordpress.android.models.usecases.BatchModerateCommentsUseCase.ModerateCommentsState.Idle
import org.wordpress.android.usecase.FlowFSMUseCase
import org.wordpress.android.usecase.FlowFSMUseCase.StateInterface
import org.wordpress.android.usecase.UseCaseResult
import org.wordpress.android.usecase.UseCaseResult.Failure
import org.wordpress.android.usecase.UseCaseResult.Loading
import org.wordpress.android.usecase.UseCaseResult.Success
import org.wordpress.android.models.usecases.BatchModerateCommentsUseCase.ModerateCommentsAction.OnModerateComment
import org.wordpress.android.models.usecases.BatchModerateCommentsUseCase.Parameters.ModerateCommentParameters
import javax.inject.Inject

class BatchModerateCommentsUseCase @Inject constructor(
    moderateCommentsResourceProvider: ModerateCommentsResourceProvider
) : FlowFSMUseCase<ModerateCommentsResourceProvider, ModerateCommentParameters, ModerateCommentsAction, DontCare, CommentsUseCaseType, CommentError>(
        resourceProvider = moderateCommentsResourceProvider,
        initialState = Idle
) {
    override suspend fun runLogic(parameters: ModerateCommentParameters) {
        manageAction(OnModerateComment(parameters))
    }

    sealed class ModerateCommentsState
        : StateInterface<ModerateCommentsResourceProvider, ModerateCommentsAction, DontCare, CommentsUseCaseType, CommentError> {
        object Idle : ModerateCommentsState() {
            override suspend fun runAction(
                resourceProvider: ModerateCommentsResourceProvider,
                action: ModerateCommentsAction,
                flowChannel: MutableSharedFlow<UseCaseResult<CommentsUseCaseType, CommentError, DontCare>>
            ): StateInterface<ModerateCommentsResourceProvider, ModerateCommentsAction, DontCare, CommentsUseCaseType, CommentError> {
                return when (action) {
                    is OnModerateComment -> {
                        val parameters = action.parameters
                        val commentsStore = resourceProvider.commentsStore
                        flowChannel.emit(Loading(MODERATE_USE_CASE))

                        parameters.remoteCommentIds.forEach {
                            val localModerationResult = commentsStore.moderateCommentLocally(
                                    site = parameters.site,
                                    remoteCommentId = it,
                                    newStatus = parameters.newStatus
                            )

                            if (localModerationResult.isError) {
                                flowChannel.emit(Failure(MODERATE_USE_CASE, localModerationResult.error, DontCare))
                                return Idle
                            }

                            val result = commentsStore.pushLocalCommentByRemoteId(
                                    site = parameters.site,
                                    remoteCommentId = it
                            )

                            if (result.isError) {
                                flowChannel.emit(Failure(MODERATE_USE_CASE, result.error, DontCare))
                                return Idle
                            }
                        }
                        flowChannel.emit(Success(MODERATE_USE_CASE, DontCare))
                        Idle
                    }
                }
            }
        }
    }

    // sealed class ModerationResult {
    //     object ModerationInProgress : ModerationResult()
    //     data class ModerationFailure(val error: CommentError) : ModerationResult()
    //     object ModerationSuccess : ModerationResult()
    //     object Idle : ModerationResult()
    // }

    sealed class ModerateCommentsAction {
        data class OnModerateComment(
            val parameters: ModerateCommentParameters
        ) : ModerateCommentsAction()
    }

    sealed class Parameters {
        data class ModerateCommentParameters(
            val site: SiteModel,
            val remoteCommentIds: List<Long>,
            val newStatus: CommentStatus,
        )
    }

}
