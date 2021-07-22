package org.wordpress.android.models.usecases

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.merge
import org.wordpress.android.models.usecases.PropagateCommentsUpdateUseCase.PropagateCommentsUpdateAction.UpdatedComments
import org.wordpress.android.modules.BG_THREAD
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class PropagateCommentsUpdateHandler @Inject constructor(
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val paginateCommentsUseCase: PropagateCommentsUpdateUseCase
) {
    private val useCases = listOf(paginateCommentsUseCase)

    fun subscribe() = useCases.map { it.subscribe() }.merge()

    suspend fun requestCommentsUpdate() = paginateCommentsUseCase.manageAction(
            UpdatedComments
    )
}
