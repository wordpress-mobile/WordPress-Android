package org.wordpress.android.models.usecases

import kotlinx.coroutines.flow.merge
import org.wordpress.android.models.usecases.BatchModerateCommentsUseCase.ModerateCommentsAction.OnModerateComments
import org.wordpress.android.models.usecases.BatchModerateCommentsUseCase.Parameters.ModerateCommentsParameters
import org.wordpress.android.models.usecases.ModerateCommentUseCase.ModerateCommentsAction.OnModerateComment
import org.wordpress.android.models.usecases.ModerateCommentUseCase.ModerateCommentsAction.OnUndoModerateComment
import org.wordpress.android.models.usecases.ModerateCommentUseCase.Parameters.ModerateCommentParameters
import org.wordpress.android.models.usecases.PaginateCommentsUseCase.PaginateCommentsAction.OnGetPage
import org.wordpress.android.models.usecases.PaginateCommentsUseCase.PaginateCommentsAction.OnReloadFromCache
import org.wordpress.android.models.usecases.PaginateCommentsUseCase.Parameters.GetPageParameters
import org.wordpress.android.models.usecases.PaginateCommentsUseCase.Parameters.ReloadFromCacheParameters
import javax.inject.Inject

class UnifiedCommentsListHandler @Inject constructor(
        // @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val paginateCommentsUseCase: PaginateCommentsUseCase,
    val batchModerationUseCase: BatchModerateCommentsUseCase,
    val moderationUseCase: ModerateCommentUseCase
) {
    private val useCases = listOf(paginateCommentsUseCase, batchModerationUseCase, moderationUseCase)

    fun subscribe() = useCases.map { it.subscribe() }.merge()

    suspend fun requestPage(parameters: GetPageParameters) = paginateCommentsUseCase.manageAction(
            OnGetPage(parameters)
    )

    suspend fun moderateComments(parameters: ModerateCommentsParameters) = batchModerationUseCase.manageAction(
            OnModerateComments(parameters)
    )

    suspend fun moderateComment(parameters: ModerateCommentParameters) = moderationUseCase.manageAction(
            OnModerateComment(parameters)
    )

    suspend fun undoCommentModeration(parameters: ModerateCommentParameters) = moderationUseCase.manageAction(
            OnUndoModerateComment(parameters)
    )

    suspend fun refreshFromCache(parameters: ReloadFromCacheParameters) = paginateCommentsUseCase.manageAction(
            OnReloadFromCache(parameters)
    )
}
