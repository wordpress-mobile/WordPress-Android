package org.wordpress.android.models.usecases

import kotlinx.coroutines.flow.MutableSharedFlow
import org.wordpress.android.fluxc.model.CommentStatus
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.persistence.comments.CommentsDao.CommentEntity
import org.wordpress.android.fluxc.store.CommentStore.CommentError
import org.wordpress.android.fluxc.store.CommentsStore.CommentsData.DoNotCare
import org.wordpress.android.usecase.FlowFSMUseCase
import org.wordpress.android.usecase.UseCaseResult
import org.wordpress.android.usecase.UseCaseResult.Failure
import org.wordpress.android.usecase.UseCaseResult.Success
import org.wordpress.android.models.usecases.CommentsUseCaseType.MODERATE_USE_CASE
import org.wordpress.android.models.usecases.ModerateCommentUseCase.ModerateCommentsAction
import org.wordpress.android.models.usecases.ModerateCommentUseCase.ModerateCommentsAction.OnModerateComment
import org.wordpress.android.models.usecases.ModerateCommentUseCase.ModerateCommentsAction.OnUndoModerateComment
import org.wordpress.android.models.usecases.ModerateCommentUseCase.ModerateCommentsState.Idle
import org.wordpress.android.models.usecases.ModerateCommentUseCase.Parameters.ModerateCommentParameters
import javax.inject.Inject

class ModerateCommentUseCase @Inject constructor(
    moderateCommentsResourceProvider: ModerateCommentsResourceProvider
) : FlowFSMUseCase<ModerateCommentsResourceProvider, ModerateCommentParameters, ModerateCommentsAction, Any, CommentsUseCaseType, CommentError>(
        resourceProvider = moderateCommentsResourceProvider,
        initialState = Idle
) {
    override suspend fun runLogic(parameters: ModerateCommentParameters) {
        manageAction(OnModerateComment(parameters))
    }

    sealed class ModerateCommentsState
        : StateInterface<ModerateCommentsResourceProvider, ModerateCommentsAction, Any, CommentsUseCaseType, CommentError> {
        data class PendingPush(val originalComment: CommentEntity) : ModerateCommentsState() {
            override suspend fun runAction(
                resourceProvider: ModerateCommentsResourceProvider,
                action: ModerateCommentsAction,
                flowChannel: MutableSharedFlow<UseCaseResult<CommentsUseCaseType, CommentError, Any>>
            ): StateInterface<ModerateCommentsResourceProvider, ModerateCommentsAction, Any, CommentsUseCaseType, CommentError> {
                val commentsStore = resourceProvider.commentsStore
                return when (action) {
                    // pushing already moderated comment to remote
                    is OnModerateComment -> {
                        val parameters = action.parameters
                        val result = commentsStore.pushLocalCommentByRemoteId(
                                site = parameters.site,
                                remoteCommentId = parameters.remoteCommentId
                        )

                        if (result.isError) {
                            // revert local moderation
                            commentsStore.moderateCommentLocally(
                                    site = parameters.site,
                                    remoteCommentId = parameters.remoteCommentId,
                                    newStatus = CommentStatus.fromString(originalComment.status)
                            )
                            flowChannel.emit(Failure(MODERATE_USE_CASE, result.error, DoNotCare))
                        }
                        flowChannel.emit(Success(MODERATE_USE_CASE, DoNotCare))
                        resourceProvider.localCommentCacheUpdateHandler.requestCommentsUpdate()
                        Idle
                    }
                    is OnUndoModerateComment -> {
                        val parameters = action.parameters
                        commentsStore.moderateCommentLocally(
                                site = parameters.site,
                                remoteCommentId = parameters.remoteCommentId,
                                newStatus = CommentStatus.fromString(originalComment.status)
                        )
                        flowChannel.emit(Success(MODERATE_USE_CASE, DoNotCare))
                        resourceProvider.localCommentCacheUpdateHandler.requestCommentsUpdate()
                        Idle
                    }
                }
            }
        }

        object Idle : ModerateCommentsState() {
            override suspend fun runAction(
                resourceProvider: ModerateCommentsResourceProvider,
                action: ModerateCommentsAction,
                flowChannel: MutableSharedFlow<UseCaseResult<CommentsUseCaseType, CommentError, Any>>
            ): StateInterface<ModerateCommentsResourceProvider, ModerateCommentsAction, Any, CommentsUseCaseType, CommentError> {
                val commentsStore = resourceProvider.commentsStore
                return when (action) {
                    is OnModerateComment -> {
                        val parameters = action.parameters
                        val commentBeforeModeration = commentsStore.getCommentByLocalSiteAndRemoteId(
                                parameters.site.id,
                                parameters.remoteCommentId
                        ).first()

                        val localModerationResult = commentsStore.moderateCommentLocally(
                                site = parameters.site,
                                remoteCommentId = parameters.remoteCommentId,
                                newStatus = parameters.newStatus
                        )

                        if (localModerationResult.isError) {
                            flowChannel.emit(Failure(MODERATE_USE_CASE, localModerationResult.error, DoNotCare))
                        } else {
                            flowChannel.emit(
                                    Success(
                                            MODERATE_USE_CASE,
                                            SingleCommentModerationResult(
                                                    parameters.remoteCommentId,
                                                    parameters.newStatus,
                                                    CommentStatus.fromString(commentBeforeModeration.status)
                                            )
                                    )
                            )
                            resourceProvider.localCommentCacheUpdateHandler.requestCommentsUpdate()
                        }
                        PendingPush(commentBeforeModeration)
                    }
                    else -> {
                        Idle
                    } // noop
                }
            }
        }
    }

    data class SingleCommentModerationResult(
        val remoteCommentId: Long,
        val newStatus: CommentStatus,
        val oldStatus: CommentStatus
    )

    sealed class ModerateCommentsAction {
        data class OnModerateComment(
            val parameters: ModerateCommentParameters
        ) : ModerateCommentsAction()

        data class OnUndoModerateComment(
            val parameters: ModerateCommentParameters
        ) : ModerateCommentsAction()
    }

    sealed class Parameters {
        data class ModerateCommentParameters(
            val site: SiteModel,
            val remoteCommentId: Long,
            val newStatus: CommentStatus
        )
    }
}
