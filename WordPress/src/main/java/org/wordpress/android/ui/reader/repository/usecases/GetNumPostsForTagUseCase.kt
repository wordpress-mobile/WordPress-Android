package org.wordpress.android.ui.reader.repository.usecases

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.wordpress.android.datasets.ReaderPostTable
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.modules.BG_THREAD
import javax.inject.Inject
import javax.inject.Named

class GetNumPostsForTagUseCase @Inject constructor(
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) : ReaderRepositoryDispatchingUseCase(bgDispatcher) {
    suspend fun get(readerTag: ReaderTag): Int =
        withContext(coroutineContext) {
            ReaderPostTable.getNumPostsWithTag(readerTag)
        }
}
