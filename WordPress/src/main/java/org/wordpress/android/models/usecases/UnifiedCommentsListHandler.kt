package org.wordpress.android.models.usecases

import kotlinx.coroutines.flow.merge
import org.wordpress.android.models.usecases.ModerateCommentsUseCase.ModerateCommentsAction.OnModerateComment
import org.wordpress.android.models.usecases.ModerateCommentsUseCase.Parameters.ModerateCommentParameters
import org.wordpress.android.models.usecases.PaginateCommentsUseCase.PaginateCommentsAction.OnGetPage
import org.wordpress.android.models.usecases.PaginateCommentsUseCase.PaginateCommentsAction.OnReloadFromCache
import org.wordpress.android.models.usecases.PaginateCommentsUseCase.Parameters.GetPageParameters
import org.wordpress.android.models.usecases.PaginateCommentsUseCase.Parameters.ReloadFromCacheParameters
import javax.inject.Inject
import javax.inject.Named

class UnifiedCommentsListHandler @Inject constructor(
    //@Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val paginateCommentsUseCase: PaginateCommentsUseCase,
    private val moderateCommentsUseCase: ModerateCommentsUseCase
) {
    private val useCases = listOf(paginateCommentsUseCase, moderateCommentsUseCase)

    fun subscribe() = useCases.map { it.subscribe() }.merge()

    suspend fun requestPage(parameters: GetPageParameters) = paginateCommentsUseCase.manageAction(
            OnGetPage(parameters)
    )

    suspend fun moderateComments(parameters: ModerateCommentParameters) = moderateCommentsUseCase.manageAction(
            OnModerateComment(parameters)
    )

    suspend fun refreshFromCache(parameters: ReloadFromCacheParameters) = paginateCommentsUseCase.manageAction(
            OnReloadFromCache(parameters)
    )
}
