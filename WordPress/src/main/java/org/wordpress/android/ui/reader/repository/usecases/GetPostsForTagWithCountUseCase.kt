package org.wordpress.android.ui.reader.repository.usecases

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.wordpress.android.datasets.wrappers.ReaderPostTableWrapper
import org.wordpress.android.models.ReaderPostList
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.modules.IO_THREAD
import javax.inject.Inject
import javax.inject.Named

class GetPostsForTagWithCountUseCase @Inject constructor(
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher,
    private val readerPostTableWrapper: ReaderPostTableWrapper
) {
    suspend fun get(
        readerTag: ReaderTag,
        maxRows: Int = 0,
        excludeTextColumns: Boolean = true
    ): Pair<ReaderPostList, Int> =
            withContext(ioDispatcher) {
                val postsForTagFromLocalDeferred = async {
                    readerPostTableWrapper.getPostsWithTag(
                            readerTag,
                            maxRows,
                            excludeTextColumns
                    )
                }

                val totalPostsForTagFromLocalDeferred = async {
                    readerPostTableWrapper.getNumPostsWithTag(readerTag)
                }

                val readerPostList = postsForTagFromLocalDeferred.await()
                val totalEntriesForTag = totalPostsForTagFromLocalDeferred.await()

                Pair(readerPostList, totalEntriesForTag)
            }
}
