package org.wordpress.android.ui.reader.repository

import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.datasets.ReaderPostTable
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.modules.BG_THREAD
import javax.inject.Inject
import javax.inject.Named

class FetchNumPostsForTagUseCase @Inject constructor(
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) : ReaderRepositoryDispatchingUseCase(bgDispatcher) {
    fun fetch(readerTag: ReaderTag): Int {
        return ReaderPostTable.getNumPostsWithTag(readerTag)
    }
}