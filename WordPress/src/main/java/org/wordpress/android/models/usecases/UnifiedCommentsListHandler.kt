package org.wordpress.android.models.usecases

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.merge
import org.wordpress.android.fluxc.store.comments.CommentsStore
import org.wordpress.android.models.usecases.PaginateCommentsUseCase.PaginateCommentsAction.OnGetPage
import org.wordpress.android.models.usecases.PaginateCommentsUseCase.Parameters
import org.wordpress.android.modules.BG_THREAD
import javax.inject.Inject
import javax.inject.Named

class UnifiedCommentsListHandler @Inject constructor(
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val commentsStore: CommentsStore,
    private val paginateCommentsUseCase: PaginateCommentsUseCase
)
{
    private val useCases = listOf(paginateCommentsUseCase)

    fun subscribe() = useCases.map { it.subscribe() }.merge()

    suspend fun requestPage(parameters: Parameters) = paginateCommentsUseCase.manageAction(
            OnGetPage(parameters, commentsStore)
    )
}
