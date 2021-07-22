package org.wordpress.android.models.usecases

import kotlinx.coroutines.flow.MutableSharedFlow
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.CommentStore.CommentError
import org.wordpress.android.fluxc.store.comments.CommentsStore
import org.wordpress.android.fluxc.store.comments.CommentsStore.CommentsData
import org.wordpress.android.models.usecases.PaginateCommentsUseCase.PaginateCommentsAction
import org.wordpress.android.models.usecases.PaginateCommentsUseCase.PaginateCommentsAction.OnGetPage
import org.wordpress.android.models.usecases.PaginateCommentsUseCase.PaginateCommentsAction.ReloadFromCache
import org.wordpress.android.models.usecases.PaginateCommentsUseCase.PaginateCommentsState.Idle
import org.wordpress.android.models.usecases.PaginateCommentsUseCase.PaginationResult
import org.wordpress.android.models.usecases.PaginateCommentsUseCase.PaginationResult.PaginationFailure
import org.wordpress.android.models.usecases.PaginateCommentsUseCase.PaginationResult.PaginationLoading
import org.wordpress.android.models.usecases.PaginateCommentsUseCase.PaginationResult.PaginationSuccess
import org.wordpress.android.models.usecases.PaginateCommentsUseCase.Parameters
import org.wordpress.android.ui.comments.unified.CommentFilter
import org.wordpress.android.ui.comments.unified.CommentFilter.UNREPLIED
import org.wordpress.android.ui.comments.unified.UnrepliedCommentsUtils
import org.wordpress.android.usecase.FlowFSMUseCase
import javax.inject.Inject

class PaginateCommentsUseCase @Inject constructor(
    private val commentsStore: CommentsStore,
    private val unrepliedCommentsUtils: UnrepliedCommentsUtils
) : FlowFSMUseCase<Parameters, PaginateCommentsAction, PaginationResult>(initialState = Idle) {
    override suspend fun runLogic(parameters: Parameters) {
        manageAction(OnGetPage(parameters, commentsStore, unrepliedCommentsUtils))
    }

    sealed class PaginateCommentsState : StateInterface<PaginateCommentsAction, PaginationResult> {
        object Idle : PaginateCommentsState() {
            override suspend fun runAction(
                action: PaginateCommentsAction,
                flowChannel: MutableSharedFlow<PaginationResult>
            ): StateInterface<PaginateCommentsAction, PaginationResult> {
                return when (action) {
                    is OnGetPage -> {
                        val parameters = action.parameters
                        val commentsStore = action.commentsStore
                        val unrepliedCommentsUtils = action.unrepliedCommentsUtils
                        if (parameters.offset == 0) flowChannel.emit(PaginationLoading)

                        val result = commentsStore.fetchComments(
                                site = parameters.site,
                                number = parameters.number,
                                offset = parameters.offset,
                                networkStatusFilter = parameters.commentFilter.toCommentStatus(),
                                cacheStatuses = parameters.commentFilter.toCommentCacheStatuses()
                        )

                        val data = result.data ?: CommentsData.empty()
                        if (parameters.commentFilter == UNREPLIED) {
                            data.comments = unrepliedCommentsUtils.getUnrepliedComments(data.comments)
                        }

                        if (result.isError) {
                            flowChannel.emit(PaginationFailure(result.error, data))
                        } else {
                            flowChannel.emit(PaginationSuccess(data))
                        }

                        Idle
                    }
                    is ReloadFromCache -> {
                        val parameters = action.parameters
                        val commentsStore = action.commentsStore

                        val result = commentsStore.getCachedComments(
                                site = parameters.site,
                                number = parameters.number,
                                offset = parameters.offset,
                                networkStatusFilter = parameters.commentFilter.toCommentStatus(),
                                cacheStatuses = parameters.commentFilter.toCommentCacheStatuses()
                        )

                        val data = result.data ?: CommentsData.empty()

                        data.hasMore = parameters.hasMore
                        flowChannel.emit(PaginationSuccess(data))
                        Idle
                    }
                }
            }
        }
    }

    sealed class PaginateCommentsAction {
        data class OnGetPage(
            val parameters: Parameters,
            val commentsStore: CommentsStore,
            val unrepliedCommentsUtils: UnrepliedCommentsUtils
        ) : PaginateCommentsAction()

        data class ReloadFromCache(val parameters: Parameters, val commentsStore: CommentsStore) :
                PaginateCommentsAction()
    }

    sealed class PaginationResult {
        object PaginationLoading : PaginationResult()
        data class PaginationFailure(val error: CommentError, val cachedData: CommentsData) : PaginationResult()
        data class PaginationSuccess(val data: CommentsData) : PaginationResult()
    }

    data class Parameters(
        val site: SiteModel,
        val number: Int,
        val offset: Int,
        val commentFilter: CommentFilter,
        val hasMore: Boolean = false
    )
}
