package org.wordpress.android.ui.reader.usecases

import dagger.Reusable
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.wordpress.android.datasets.ReaderTagTable
import org.wordpress.android.models.ReaderTagList
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.reader.utils.ReaderUtils
import javax.inject.Inject
import javax.inject.Named

/**
 * Loads list of tags that should be displayed as tabs in the entry-point Reader screen.
 */
@Reusable
class LoadReaderTabsUseCase @Inject constructor(
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) {
    suspend fun loadTabs(): ReaderTagList {
        return withContext(bgDispatcher) {
            val tagList = ReaderTagTable.getDefaultTags()

            tagList.addAll(ReaderTagTable.getBookmarkTags()) // Add "Saved" tab manually

            ReaderUtils.getOrderedTagsList(tagList, ReaderUtils.getDefaultTagInfo()) // TODO do we need to call this?
        }
    }
}
