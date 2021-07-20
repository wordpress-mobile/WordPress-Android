package org.wordpress.android.models.usecases

import kotlinx.coroutines.flow.MutableSharedFlow
import org.wordpress.android.fluxc.model.CommentStatus
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.comments.CommentsStore
import org.wordpress.android.fluxc.store.comments.CommentsStore.CommentsData
import org.wordpress.android.models.usecases.PaginateCommentsUseCase.PaginateCommentsAction
import org.wordpress.android.models.usecases.PaginateCommentsUseCase.PaginateCommentsAction.OnGetPage
import org.wordpress.android.models.usecases.PaginateCommentsUseCase.PaginateCommentsState.Idle
import org.wordpress.android.models.usecases.PaginateCommentsUseCase.Parameters
import org.wordpress.android.usecase.FlowFSMUseCase
import org.wordpress.android.usecase.UseCaseResult
import org.wordpress.android.usecase.UseCaseResult.Failure
import org.wordpress.android.usecase.UseCaseResult.Loading
import org.wordpress.android.usecase.UseCaseResult.Success
import javax.inject.Inject

class PaginateCommentsUseCase @Inject constructor(
    private val commentsStore: CommentsStore
): FlowFSMUseCase<Parameters, PaginateCommentsAction, CommentsData>(initialState = Idle) {
    override suspend fun runLogic(parameters: Parameters) {
        manageAction(OnGetPage(parameters, commentsStore))
    }

    sealed class PaginateCommentsState: StateInterface<PaginateCommentsAction, CommentsData> {
        object Idle : PaginateCommentsState() {
            override suspend fun runAction(
                action: PaginateCommentsAction,
                flowChannel: MutableSharedFlow<UseCaseResult<CommentsData>>
            ): StateInterface<PaginateCommentsAction, CommentsData> {
                return when (action) {
                    is OnGetPage -> {
                        val parameters = action.parameters
                        val commentsStore = action.commentsStore

                        if (parameters.offset == 0) flowChannel.emit(Loading)

                        val result = commentsStore.fetchComments(
                                site = parameters.site,
                                number = parameters.number,
                                offset = parameters.offset,
                                networkStatusFilter = parameters.networkStatusFilter,
                                cacheStatuses = parameters.cacheStatuses
                        )

                        val data = result.data ?: CommentsData.empty()

                        if (result.isError) {
                            flowChannel.emit(Failure(result.error, data))
                        } else {
                            flowChannel.emit(Success(data))
                        }

                        Idle
                    }
                }
            }
        }

        //object FetchingPage : PaginateCommentsState() {
        //    override fun runAction(
        //        action: PaginateCommentsAction,
        //        flowChannel: MutableSharedFlow<UseCaseResult<List<CommentEntity>>>
        //    ): StateInterface<PaginateCommentsAction, List<CommentEntity>> {
        //        return when (action) {
        //            OnFetchCompleted -> {
//
        //            }
        //            else -> throw IllegalStateException("Illegal Action [$action] attempted on State [$this]")
        //        }
        //    }
        //}

    }

    sealed class PaginateCommentsAction {
        data class OnGetPage(val parameters: Parameters, val commentsStore: CommentsStore): PaginateCommentsAction()
        //data class OnFetchCompleted(val parameters: Parameters): PaginateCommentsAction()
    }

    data class Parameters(
        val site: SiteModel,
        val number: Int,
        val offset: Int,
        val networkStatusFilter: CommentStatus,
        val cacheStatuses: List<CommentStatus>
    )
}
