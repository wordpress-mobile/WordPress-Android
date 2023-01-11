package org.wordpress.android.models.usecases

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.CommentStore.CommentError
import org.wordpress.android.fluxc.store.CommentStore.CommentErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.CommentsStore.CommentsActionPayload
import org.wordpress.android.fluxc.store.CommentsStore.CommentsData.PagingData
import org.wordpress.android.models.usecases.CommentsUseCaseType.PAGINATE_USE_CASE
import org.wordpress.android.models.usecases.PaginateCommentsUseCase.PaginateCommentsAction
import org.wordpress.android.models.usecases.PaginateCommentsUseCase.PaginateCommentsAction.OnGetPage
import org.wordpress.android.models.usecases.PaginateCommentsUseCase.PaginateCommentsAction.OnReloadFromCache
import org.wordpress.android.models.usecases.PaginateCommentsUseCase.PaginateCommentsState.Idle
import org.wordpress.android.models.usecases.PaginateCommentsUseCase.Parameters.GetPageParameters
import org.wordpress.android.models.usecases.PaginateCommentsUseCase.Parameters.ReloadFromCacheParameters
import org.wordpress.android.ui.comments.unified.CommentFilter
import org.wordpress.android.ui.comments.unified.CommentFilter.UNREPLIED
import org.wordpress.android.usecase.FlowFSMUseCase
import org.wordpress.android.usecase.UseCaseResult
import org.wordpress.android.usecase.UseCaseResult.Failure
import org.wordpress.android.usecase.UseCaseResult.Loading
import org.wordpress.android.usecase.UseCaseResult.Success
import javax.inject.Inject

class PaginateCommentsUseCase @Inject constructor(
    paginateCommentsResourceProvider: PaginateCommentsResourceProvider
) : FlowFSMUseCase<PaginateCommentsResourceProvider, PaginateCommentsAction, PagingData,
        CommentsUseCaseType, CommentError>(
    resourceProvider = paginateCommentsResourceProvider,
    initialState = Idle
) {
    @Suppress("LongMethod") // temporary suppress until we come up with better architecture for use cases
    sealed class PaginateCommentsState : StateInterface<PaginateCommentsResourceProvider, PaginateCommentsAction,
            PagingData, CommentsUseCaseType,
            CommentError> {
        object Idle : PaginateCommentsState() {
            override suspend fun runAction(
                utilsProvider: PaginateCommentsResourceProvider,
                action: PaginateCommentsAction,
                flowChannel: MutableSharedFlow<UseCaseResult<CommentsUseCaseType, CommentError, PagingData>>
            ): StateInterface<PaginateCommentsResourceProvider, PaginateCommentsAction, PagingData, CommentsUseCaseType,
                    CommentError> {
                val unrepliedCommentsUtils = utilsProvider.unrepliedCommentsUtils
                return when (action) {
                    is OnGetPage -> {
                        val parameters = action.parameters
                        val commentsStore = utilsProvider.commentsStore
                        if (parameters.offset == 0) {
                            flowChannel.emit(Loading(PAGINATE_USE_CASE))
                            delay(LOADING_STATE_DELAY)
                        }
                        val result = if (!utilsProvider.networkUtilsWrapper.isNetworkAvailable()) {
                            val cachedComments = if (parameters.offset > 0) {
                                commentsStore.getCommentsForSite(
                                    site = parameters.site,
                                    orderByDateAscending = false,
                                    limit = parameters.offset,
                                    statuses = parameters.commentFilter.toCommentCacheStatuses().toTypedArray()
                                )
                            } else {
                                listOf()
                            }
                            CommentsActionPayload(
                                CommentError(
                                    GENERIC_ERROR,
                                    utilsProvider.resourceProvider.getString(R.string.no_network_message)
                                ), PagingData(
                                    comments = cachedComments,
                                    hasMore = cachedComments.isNotEmpty()
                                )
                            )
                        } else {
                            commentsStore.fetchCommentsPage(
                                site = parameters.site,
                                number = parameters.number,
                                offset = parameters.offset,
                                networkStatusFilter = parameters.commentFilter.toCommentStatus(),
                                cacheStatuses = parameters.commentFilter.toCommentCacheStatuses()
                            )
                        }

                        val data = (result.data ?: PagingData.empty()).let {
                            if (parameters.commentFilter == UNREPLIED) {
                                it.copy(comments = unrepliedCommentsUtils.getUnrepliedComments(it.comments))
                            } else it
                        }

                        if (result.isError) {
                            flowChannel.emit(Failure(PAGINATE_USE_CASE, result.error, data))
                        } else {
                            flowChannel.emit(Success(PAGINATE_USE_CASE, data))
                        }

                        Idle
                    }
                    is OnReloadFromCache -> {
                        val parameters = action.parameters
                        val commentsStore = utilsProvider.commentsStore

                        val result = commentsStore.getCachedComments(
                            site = parameters.pagingParameters.site,
                            cacheStatuses = parameters.pagingParameters.commentFilter.toCommentCacheStatuses(),
                            imposeHasMore = parameters.hasMore
                        )

                        val data = (result.data ?: PagingData.empty()).let {
                            if (parameters.pagingParameters.commentFilter == UNREPLIED) {
                                it.copy(comments = unrepliedCommentsUtils.getUnrepliedComments(it.comments))
                            } else it
                        }

                        if (result.isError) {
                            flowChannel.emit(Failure(PAGINATE_USE_CASE, result.error, data))
                        } else {
                            flowChannel.emit(Success(PAGINATE_USE_CASE, data))
                        }

                        Idle
                    }
                }
            }
        }
    }

    sealed class PaginateCommentsAction {
        data class OnGetPage(
            val parameters: GetPageParameters
        ) : PaginateCommentsAction()

        data class OnReloadFromCache(
            val parameters: ReloadFromCacheParameters
        ) : PaginateCommentsAction()
    }

    sealed class Parameters {
        data class GetPageParameters(
            val site: SiteModel,
            val number: Int,
            val offset: Int,
            val commentFilter: CommentFilter
        ) : Parameters()

        data class ReloadFromCacheParameters(
            val pagingParameters: GetPageParameters,
            val hasMore: Boolean
        ) : Parameters()
    }

    companion object {
        const val LOADING_STATE_DELAY = 500L
    }
}
