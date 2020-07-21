package org.wordpress.android.ui.reader.repository.usecases

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.wordpress.android.datasets.ReaderPostTable
import org.wordpress.android.models.ReaderPostList
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.modules.IO_THREAD
import javax.inject.Inject
import javax.inject.Named

class GetPostsForTagUseCase @Inject constructor(
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher
) : ReaderRepositoryDispatchingUseCase(ioDispatcher) {
    suspend fun get(readerTag: ReaderTag): ReaderPostList =
            withContext(coroutineContext) {
                ReaderPostTable.getPostsWithTag(
                        readerTag,
                        MAX_ROWS,
                        EXCLUDE_TEXT_COLUMN
                )
            }
}
