package org.wordpress.android.models.usecases

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.merge
import org.wordpress.android.fluxc.store.comments.CommentsStore
import org.wordpress.android.models.usecases.ModerateCommentsUseCase.ModerateCommentParameters
import org.wordpress.android.models.usecases.ModerateCommentsUseCase.ModerateCommentsAction.ModerateComment
import org.wordpress.android.models.usecases.PaginateCommentsUseCase.PaginateCommentsAction.OnGetPage
import org.wordpress.android.models.usecases.PaginateCommentsUseCase.PaginateCommentsAction.ReloadFromCache
import org.wordpress.android.models.usecases.PaginateCommentsUseCase.Parameters
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.comments.unified.UnrepliedCommentsUtils
import javax.inject.Inject
import javax.inject.Named

class UnifiedCommentsListHandler @Inject constructor(
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val commentsStore: CommentsStore,
    private val paginateCommentsUseCase: PaginateCommentsUseCase,
    private val moderateCommentsUseCase: ModerateCommentsUseCase,
        private val unrepliedCommentsUtils: UnrepliedCommentsUtils
) {
    private val useCases = listOf(paginateCommentsUseCase)

    fun subscribe() = useCases.map { it.subscribe() }.merge()

    suspend fun requestPage(parameters: Parameters) = paginateCommentsUseCase.manageAction(
            OnGetPage(parameters, commentsStore, unrepliedCommentsUtils)
    )

    suspend fun moderateComments(parameters: ModerateCommentParameters) = moderateCommentsUseCase.manageAction(
            ModerateComment(parameters, commentsStore)
    )

    suspend fun refreshFromCache(parameters: Parameters) = paginateCommentsUseCase.manageAction(
            ReloadFromCache(parameters, commentsStore)
    )
}
