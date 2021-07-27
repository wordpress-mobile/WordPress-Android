package org.wordpress.android.models.usecases

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.CommentStatus
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.CommentStore.CommentError
import org.wordpress.android.fluxc.store.CommentsStore.CommentsData.DoNotCare
import org.wordpress.android.fluxc.store.CommentStore.CommentErrorType.GENERIC_ERROR
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
import org.wordpress.android.ui.comments.unified.UnifiedCommentListViewModel.SelectedComment
import org.wordpress.android.usecase.UseCaseResult.Success
import javax.inject.Inject

class BatchModerateCommentsUseCase @Inject constructor(
    moderateCommentsResourceProvider: ModerateCommentsResourceProvider
) : FlowFSMUseCase<ModerateCommentsResourceProvider, ModerateCommentParameters, ModerateCommentsAction, DoNotCare, CommentsUseCaseType, CommentError>(
        resourceProvider = moderateCommentsResourceProvider,
        initialState = Idle
) {
    override suspend fun runLogic(parameters: ModerateCommentParameters) {
        manageAction(OnModerateComment(parameters))
    }

    sealed class ModerateCommentsState
        : StateInterface<ModerateCommentsResourceProvider, ModerateCommentsAction, DoNotCare, CommentsUseCaseType, CommentError> {
        object Idle : ModerateCommentsState() {
            override suspend fun runAction(
                resourceProvider: ModerateCommentsResourceProvider,
                action: ModerateCommentsAction,
                flowChannel: MutableSharedFlow<UseCaseResult<CommentsUseCaseType, CommentError, DoNotCare>>
            ): StateInterface<ModerateCommentsResourceProvider, ModerateCommentsAction, DoNotCare, CommentsUseCaseType, CommentError> {
                return when (action) {
                    is OnModerateComment -> {
                        val parameters = action.parameters
                        val commentsStore = resourceProvider.commentsStore

                        withContext(resourceProvider.bgDispatcher) {
                            val deferredList = parameters.selectedComments.map {
                                async {
                                    val localModerationResult = commentsStore.moderateCommentLocally(
                                            site = parameters.site,
                                            remoteCommentId = it.remoteCommentId,
                                            newStatus = parameters.newStatus
                                    )

                                    if (localModerationResult.isError) {
                                        return@async localModerationResult
                                    } else {
                                        flowChannel.emit(Success(MODERATE_USE_CASE, DoNotCare))
                                    }

                                    val result = commentsStore.pushLocalCommentByRemoteId(
                                            site = parameters.site,
                                            remoteCommentId = it.remoteCommentId
                                    )

                                    if (result.isError) {
                                        // revert local moderation
                                        commentsStore.moderateCommentLocally(
                                                site = parameters.site,
                                                remoteCommentId = it.remoteCommentId,
                                                newStatus = it.status
                                        )
                                        flowChannel.emit(Success(MODERATE_USE_CASE, DoNotCare))
                                    }
                                    return@async result
                                }
                            }.awaitAll()

                            if (deferredList.any { it.isError }) {
                                flowChannel.emit(
                                        Failure(
                                                MODERATE_USE_CASE,
                                                CommentError(GENERIC_ERROR, "Failed to moderate one or more comments."),
                                                DoNotCare
                                        )
                                )
                            }
                        }
                        Idle
                    }
                }
            }
        }
    }

    sealed class ModerateCommentsAction {
        data class OnModerateComment(
            val parameters: ModerateCommentParameters
        ) : ModerateCommentsAction()
    }

    sealed class Parameters {
        data class ModerateCommentParameters(
            val site: SiteModel,
            val selectedComments: List<SelectedComment>,
            val newStatus: CommentStatus
        )
    }

}
