package org.wordpress.android.ui.reader.repository.usecases

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.wordpress.android.datasets.ReaderPostTable
import org.wordpress.android.models.ReaderPostList
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.modules.IO_THREAD
import javax.inject.Inject
import javax.inject.Named

class GetPostsForTagWithCountUseCase @Inject constructor(
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher
) : ReaderRepositoryDispatchingUseCase(ioDispatcher) {
    suspend fun get(
        readerTag: ReaderTag,
        maxRows: Int = 0,
        excludeTextColumns: Boolean = true
    ): Pair<ReaderPostList, Int> =
            withContext(coroutineContext) {
                val postsForTagFromLocalDeferred = async {
                    ReaderPostTable.getPostsWithTag(
                            readerTag,
                            maxRows,
                            excludeTextColumns
                    )
                }

                val totalPostsForTagFromLocalDeferred = async {
                    ReaderPostTable.getNumPostsWithTag(readerTag)
                }

                val readerPostList = postsForTagFromLocalDeferred.await()
                val totalEntriesForTag = totalPostsForTagFromLocalDeferred.await()

                Pair(readerPostList, totalEntriesForTag)
            }
}
