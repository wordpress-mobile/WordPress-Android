package org.wordpress.android.models.usecases

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.CommentStatus
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.CommentStore.CommentError
import org.wordpress.android.fluxc.store.CommentStore.CommentErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.CommentsStore.CommentsData.DoNotCare
import org.wordpress.android.models.usecases.BatchModerateCommentsUseCase.ModerateCommentsAction
import org.wordpress.android.models.usecases.BatchModerateCommentsUseCase.ModerateCommentsAction.OnModerateComments
import org.wordpress.android.models.usecases.BatchModerateCommentsUseCase.ModerateCommentsState.Idle
import org.wordpress.android.models.usecases.BatchModerateCommentsUseCase.Parameters.ModerateCommentsParameters
import org.wordpress.android.models.usecases.CommentsUseCaseType.BATCH_MODERATE_USE_CASE
import org.wordpress.android.usecase.FlowFSMUseCase
import org.wordpress.android.usecase.UseCaseResult
import org.wordpress.android.usecase.UseCaseResult.Failure
import org.wordpress.android.usecase.UseCaseResult.Success
import javax.inject.Inject

class BatchModerateCommentsUseCase @Inject constructor(
    moderateCommentsResourceProvider: ModerateCommentsResourceProvider
) : FlowFSMUseCase<ModerateCommentsResourceProvider, ModerateCommentsParameters, ModerateCommentsAction, DoNotCare,
        CommentsUseCaseType, CommentError>(
        resourceProvider = moderateCommentsResourceProvider,
        initialState = Idle
) {
    override suspend fun runInitLogic(parameters: ModerateCommentsParameters) {
        manageAction(OnModerateComments(parameters))
    }

    sealed class ModerateCommentsState
        : StateInterface<ModerateCommentsResourceProvider, ModerateCommentsAction, DoNotCare, CommentsUseCaseType,
            CommentError> {
        object Idle : ModerateCommentsState() {
            override suspend fun runAction(
                resourceProvider: ModerateCommentsResourceProvider,
                action: ModerateCommentsAction,
                flowChannel: MutableSharedFlow<UseCaseResult<CommentsUseCaseType, CommentError, DoNotCare>>
            ): StateInterface<ModerateCommentsResourceProvider, ModerateCommentsAction, DoNotCare, CommentsUseCaseType,
                    CommentError> {
                val commentsStore = resourceProvider.commentsStore
                return when (action) {
                    is OnModerateComments -> {
                        val parameters = action.parameters
                        withContext(resourceProvider.bgDispatcher) {
                            val deferredList = parameters.remoteCommentIds.map {
                                async {
                                    val commentBeforeModeration = commentsStore.getCommentByLocalSiteAndRemoteId(
                                            parameters.site.id,
                                            it
                                    ).first()

                                    val localModerationResult = commentsStore.moderateCommentLocally(
                                            site = parameters.site,
                                            remoteCommentId = it,
                                            newStatus = parameters.newStatus
                                    )

                                    if (localModerationResult.isError) {
                                        return@async localModerationResult
                                    } else {
                                        flowChannel.emit(Success(BATCH_MODERATE_USE_CASE, DoNotCare))
                                        resourceProvider.localCommentCacheUpdateHandler.requestCommentsUpdate()
                                    }

                                    val result = commentsStore.pushLocalCommentByRemoteId(
                                            site = parameters.site,
                                            remoteCommentId = it
                                    )

                                    if (result.isError) {
                                        // revert local moderation
                                        commentsStore.moderateCommentLocally(
                                                site = parameters.site,
                                                remoteCommentId = it,
                                                newStatus = CommentStatus.fromString(commentBeforeModeration.status)
                                        )
                                        flowChannel.emit(Success(BATCH_MODERATE_USE_CASE, DoNotCare))
                                        resourceProvider.localCommentCacheUpdateHandler.requestCommentsUpdate()
                                    }
                                    return@async result
                                }
                            }.awaitAll()

                            if (deferredList.any { it.isError }) {
                                flowChannel.emit(
                                        Failure(
                                                BATCH_MODERATE_USE_CASE,
                                                CommentError(GENERIC_ERROR, ""),
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
        data class OnModerateComments(
            val parameters: ModerateCommentsParameters
        ) : ModerateCommentsAction()
    }

    sealed class Parameters {
        data class ModerateCommentsParameters(
            val site: SiteModel,
            val remoteCommentIds: List<Long>,
            val newStatus: CommentStatus
        )
    }
}
