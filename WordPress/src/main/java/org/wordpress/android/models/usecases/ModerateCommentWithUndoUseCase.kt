package org.wordpress.android.models.usecases

import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import org.wordpress.android.fluxc.model.CommentStatus
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.CommentStore.CommentError
import org.wordpress.android.fluxc.store.CommentsStore.CommentsData.DoNotCare
import org.wordpress.android.usecase.FlowFSMUseCase
import org.wordpress.android.usecase.UseCaseResult
import org.wordpress.android.usecase.UseCaseResult.Failure
import org.wordpress.android.usecase.UseCaseResult.Success
import org.wordpress.android.models.usecases.CommentsUseCaseType.MODERATE_USE_CASE
import org.wordpress.android.models.usecases.ModerateCommentWithUndoUseCase.ModerateCommentsAction
import org.wordpress.android.models.usecases.ModerateCommentWithUndoUseCase.ModerateCommentsAction.OnModerateComment
import org.wordpress.android.models.usecases.ModerateCommentWithUndoUseCase.ModerateCommentsAction.OnPushComment
import org.wordpress.android.models.usecases.ModerateCommentWithUndoUseCase.ModerateCommentsAction.OnUndoModerateComment
import org.wordpress.android.models.usecases.ModerateCommentWithUndoUseCase.ModerateCommentsState.Idle
import org.wordpress.android.models.usecases.ModerateCommentWithUndoUseCase.Parameters.ModerateCommentParameters
import org.wordpress.android.models.usecases.ModerateCommentWithUndoUseCase.Parameters.ModerateWithFallbackParameters
import javax.inject.Inject

class ModerateCommentWithUndoUseCase @Inject constructor(
    moderateCommentsResourceProvider: ModerateCommentsResourceProvider
) : FlowFSMUseCase<ModerateCommentsResourceProvider, ModerateCommentParameters, ModerateCommentsAction, Any,
        CommentsUseCaseType, CommentError>(
        resourceProvider = moderateCommentsResourceProvider,
        initialState = Idle
) {
    override suspend fun runInitLogic(parameters: ModerateCommentParameters) {
        manageAction(OnModerateComment(parameters))
    }

    sealed class ModerateCommentsState
        : StateInterface<ModerateCommentsResourceProvider, ModerateCommentsAction, Any, CommentsUseCaseType,
            CommentError> {
        object Idle : ModerateCommentsState() {
            override suspend fun runAction(
                resourceProvider: ModerateCommentsResourceProvider,
                action: ModerateCommentsAction,
                flowChannel: MutableSharedFlow<UseCaseResult<CommentsUseCaseType, CommentError, Any>>
            ): StateInterface<ModerateCommentsResourceProvider, ModerateCommentsAction, Any, CommentsUseCaseType,
                    CommentError> {
                val commentsStore = resourceProvider.commentsStore
                return when (action) {
                    is OnModerateComment -> {
                        val parameters = action.parameters
                        Log.v("MODERATION", "Mod: Start moderating locally: ${parameters.remoteCommentId}")
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
                            Log.v("MODERATION", "Mod: Failed local moderation:: ${parameters.remoteCommentId}")
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
                        Log.v("MODERATION", "Mod: Finished moderating locally: ${parameters.remoteCommentId}")
                        Idle
                    }
                    is OnPushComment -> {
                        val parameters = action.parameters
                        Log.v("MODERATION", "Mod: Start pushing comment: ${parameters.remoteCommentId}")

                        // we need to try and moderate comment locally again, since user might have refresh the list
                        // while moderation was not finalized yet
                        commentsStore.moderateCommentLocally(
                                site = parameters.site,
                                remoteCommentId = parameters.remoteCommentId,
                                newStatus = parameters.newStatus
                        )
                        resourceProvider.localCommentCacheUpdateHandler.requestCommentsUpdate()

                        val result = commentsStore.pushLocalCommentByRemoteId(
                                site = parameters.site,
                                remoteCommentId = parameters.remoteCommentId
                        )

                        if (result.isError) {
                            // revert local moderation
                            commentsStore.moderateCommentLocally(
                                    site = parameters.site,
                                    remoteCommentId = parameters.remoteCommentId,
                                    newStatus = parameters.fallbackStatus
                            )
                            flowChannel.emit(Failure(MODERATE_USE_CASE, result.error, DoNotCare))
                        }
                        flowChannel.emit(Success(MODERATE_USE_CASE, DoNotCare))
                        resourceProvider.localCommentCacheUpdateHandler.requestCommentsUpdate()
                        Log.v("MODERATION", "Mod: Finished pushing comment: ${parameters.remoteCommentId}")
                        Idle
                    }
                    is OnUndoModerateComment -> {
                        val parameters = action.parameters
                        commentsStore.moderateCommentLocally(
                                site = parameters.site,
                                remoteCommentId = parameters.remoteCommentId,
                                newStatus = parameters.fallbackStatus
                        )
                        flowChannel.emit(Success(MODERATE_USE_CASE, DoNotCare))
                        resourceProvider.localCommentCacheUpdateHandler.requestCommentsUpdate()
                        Idle
                    }
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

        data class OnPushComment(
            val parameters: ModerateWithFallbackParameters
        ) : ModerateCommentsAction()

        data class OnUndoModerateComment(
            val parameters: ModerateWithFallbackParameters
        ) : ModerateCommentsAction()
    }

    sealed class Parameters {
        data class ModerateCommentParameters(
            val site: SiteModel,
            val remoteCommentId: Long,
            val newStatus: CommentStatus
        )

        data class ModerateWithFallbackParameters(
            val site: SiteModel,
            val remoteCommentId: Long,
            val newStatus: CommentStatus,
            val fallbackStatus: CommentStatus
        )
    }
}
