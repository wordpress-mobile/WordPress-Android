package org.wordpress.android.models.usecases

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.merge
import org.wordpress.android.models.usecases.LocalCommentCacheUpdateUseCase.PropagateCommentsUpdateAction.UpdatedComments
import org.wordpress.android.modules.BG_THREAD
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class LocalCommentCacheUpdateHandler @Inject constructor(
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val localCommentCacheUpdateUseCase: LocalCommentCacheUpdateUseCase
) {
    private val useCases = listOf(localCommentCacheUpdateUseCase)

    fun subscribe() = useCases.map { it.subscribe() }.merge()

    suspend fun requestCommentsUpdate() = localCommentCacheUpdateUseCase.manageAction(
            UpdatedComments
    )
}
