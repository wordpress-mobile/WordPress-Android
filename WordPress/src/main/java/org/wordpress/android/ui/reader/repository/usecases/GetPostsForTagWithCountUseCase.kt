package org.wordpress.android.ui.reader.repository.usecases

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.wordpress.android.datasets.ReaderPostTable
import org.wordpress.android.models.ReaderPostList
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.modules.BG_THREAD
import javax.inject.Inject
import javax.inject.Named

class GetPostsForTagWithCountUseCase @Inject constructor(
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) : ReaderRepositoryDispatchingUseCase(bgDispatcher) {
    suspend fun fetch(readerTag: ReaderTag): Pair<ReaderPostList, Int> =
            withContext(bgDispatcher) {
                val postsForTagFromLocalDeferred = async {
                    ReaderPostTable.getPostsWithTag(
                            readerTag,
                            MAX_ROWS,
                            EXCLUDE_TEXT_COLUMN
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
