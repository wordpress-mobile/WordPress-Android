package org.wordpress.android.models.usecases

import kotlinx.coroutines.flow.merge
import org.wordpress.android.models.usecases.BatchModerateCommentsUseCase.ModerateCommentsAction.OnModerateComment
import org.wordpress.android.models.usecases.BatchModerateCommentsUseCase.Parameters.ModerateCommentParameters
import org.wordpress.android.models.usecases.PaginateCommentsUseCase.PaginateCommentsAction.OnGetPage
import org.wordpress.android.models.usecases.PaginateCommentsUseCase.PaginateCommentsAction.OnReloadFromCache
import org.wordpress.android.models.usecases.PaginateCommentsUseCase.Parameters.GetPageParameters
import org.wordpress.android.models.usecases.PaginateCommentsUseCase.Parameters.ReloadFromCacheParameters
import javax.inject.Inject

class UnifiedCommentsListHandler @Inject constructor(
        //@Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val paginateCommentsUseCase: PaginateCommentsUseCase,
     val batchModerationUseCase: BatchModerateCommentsUseCase
) {
    private val useCases = listOf(paginateCommentsUseCase, batchModerationUseCase)

    fun subscribe() = useCases.map { it.subscribe() }.merge()

    suspend fun requestPage(parameters: GetPageParameters) = paginateCommentsUseCase.manageAction(
            OnGetPage(parameters)
    )

    suspend fun moderateComments(parameters: ModerateCommentParameters) = batchModerationUseCase.manageAction(
            OnModerateComment(parameters)
    )

    suspend fun refreshFromCache(parameters: ReloadFromCacheParameters) = paginateCommentsUseCase.manageAction(
            OnReloadFromCache(parameters)
    )
}
