package org.wordpress.android.models.usecases

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.merge
import org.wordpress.android.models.usecases.LocalCommentCacheUpdateUseCase.PropagateCommentsUpdateAction.UpdatedComments
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalCommentCacheUpdateHandler @Inject constructor(
    private val localCommentCacheUpdateUseCase: LocalCommentCacheUpdateUseCase
) {
    private val useCases = listOf(localCommentCacheUpdateUseCase)

    @OptIn(ExperimentalCoroutinesApi::class)
    fun subscribe() = useCases.map { it.subscribe() }.merge()

    suspend fun requestCommentsUpdate() = localCommentCacheUpdateUseCase.manageAction(
            UpdatedComments
    )
}
