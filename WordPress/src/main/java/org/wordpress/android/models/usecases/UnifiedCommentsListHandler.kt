package org.wordpress.android.models.usecases

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.merge
import org.wordpress.android.models.usecases.BatchModerateCommentsUseCase.ModerateCommentsAction.OnModerateComments
import org.wordpress.android.models.usecases.BatchModerateCommentsUseCase.Parameters.ModerateCommentsParameters
import org.wordpress.android.models.usecases.ModerateCommentWithUndoUseCase.ModerateCommentsAction.OnModerateComment
import org.wordpress.android.models.usecases.ModerateCommentWithUndoUseCase.ModerateCommentsAction.OnPushComment
import org.wordpress.android.models.usecases.ModerateCommentWithUndoUseCase.ModerateCommentsAction.OnUndoModerateComment
import org.wordpress.android.models.usecases.ModerateCommentWithUndoUseCase.Parameters.ModerateCommentParameters
import org.wordpress.android.models.usecases.ModerateCommentWithUndoUseCase.Parameters.ModerateWithFallbackParameters
import org.wordpress.android.models.usecases.PaginateCommentsUseCase.PaginateCommentsAction.OnGetPage
import org.wordpress.android.models.usecases.PaginateCommentsUseCase.PaginateCommentsAction.OnReloadFromCache
import org.wordpress.android.models.usecases.PaginateCommentsUseCase.Parameters.GetPageParameters
import org.wordpress.android.models.usecases.PaginateCommentsUseCase.Parameters.ReloadFromCacheParameters
import javax.inject.Inject

class UnifiedCommentsListHandler @Inject constructor(
    private val paginateCommentsUseCase: PaginateCommentsUseCase,
    private val batchModerationUseCase: BatchModerateCommentsUseCase,
    private val moderationWithUndoUseCase: ModerateCommentWithUndoUseCase
) {
    private val useCases = listOf(paginateCommentsUseCase, batchModerationUseCase, moderationWithUndoUseCase)

    @OptIn(ExperimentalCoroutinesApi::class)
    fun subscribe() = useCases.map { it.subscribe() }.merge()

    suspend fun requestPage(parameters: GetPageParameters) = paginateCommentsUseCase.manageAction(
        OnGetPage(parameters)
    )

    suspend fun moderateComments(parameters: ModerateCommentsParameters) = batchModerationUseCase.manageAction(
        OnModerateComments(parameters)
    )

    suspend fun preModerateWithUndo(parameters: ModerateCommentParameters) = moderationWithUndoUseCase.manageAction(
        OnModerateComment(parameters)
    )

    suspend fun moderateAfterUndo(parameters: ModerateWithFallbackParameters) = moderationWithUndoUseCase.manageAction(
        OnPushComment(parameters)
    )

    suspend fun undoCommentModeration(parameters: ModerateWithFallbackParameters) =
        moderationWithUndoUseCase.manageAction(
            OnUndoModerateComment(parameters)
        )

    suspend fun refreshFromCache(parameters: ReloadFromCacheParameters) = paginateCommentsUseCase.manageAction(
        OnReloadFromCache(parameters)
    )
}
