package org.wordpress.android.ui.reader.usecases

import dagger.Reusable
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.wordpress.android.R
import org.wordpress.android.datasets.ReaderTagTable
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.ReaderTagList
import org.wordpress.android.models.ReaderTagType
import org.wordpress.android.models.containsFollowingTag
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.reader.utils.ReaderUtils
import org.wordpress.android.ui.reader.utils.ReaderUtilsWrapper
import org.wordpress.android.util.StringProvider
import javax.inject.Inject
import javax.inject.Named

/**
 * Loads list of items that should be displayed in the Reader dropdown menu.
 */
@Reusable
class LoadReaderItemsUseCase @Inject constructor(
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val readerUtilsWrapper: ReaderUtilsWrapper,
    private val stringProvider: StringProvider,
) {
    suspend fun load(): ReaderTagList {
        return withContext(bgDispatcher) {
            val tagList = ReaderTagTable.getDefaultTags()

            /* Creating custom tag lists isn't supported anymore. However, we need to keep the support here
            for users who created custom lists in the past.*/
            tagList.addAll(ReaderTagTable.getCustomListTags())

            tagList.addAll(ReaderTagTable.getBookmarkTags()) // Add "Saved" item manually

            // Add "Tags" item manually
            tagList.add(ReaderTag(
                "",
                stringProvider.getString(R.string.reader_tags_display_name),
                stringProvider.getString(R.string.reader_tags_display_name),
                "",
                ReaderTagType.TAGS
            ))

            // Add "Subscriptions" item manually when on self-hosted site
            if (!tagList.containsFollowingTag()) {
                tagList.add(readerUtilsWrapper.getDefaultTagFromDbOrCreateInMemory())
            }

            ReaderUtils.getOrderedTagsList(tagList, ReaderUtils.getDefaultTagInfo())
        }
    }
}
