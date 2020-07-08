package org.wordpress.android.ui.reader.repository.usecases

import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.datasets.ReaderPostTable
import org.wordpress.android.models.ReaderPostList
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.modules.BG_THREAD
import javax.inject.Inject
import javax.inject.Named

class GetPostsForTagUseCase @Inject constructor(
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) : ReaderRepositoryDispatchingUseCase(bgDispatcher) {
    fun fetch(readerTag: ReaderTag): ReaderPostList {
        return ReaderPostTable.getPostsWithTag(
                readerTag,
                MAX_ROWS,
                EXCLUDE_TEXT_COLUMN
        )
    }
}
