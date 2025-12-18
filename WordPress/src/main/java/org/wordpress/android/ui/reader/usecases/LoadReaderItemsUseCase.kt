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

            val orderedList = ReaderUtils.getOrderedTagsList(tagList, ReaderUtils.getDefaultTagInfo())

            // Add "Freshly Pressed" after ordering to ensure it appears after Discover in the list
            if (orderedList.none { it.isFreshlyPressed }) {
                // Find Discover index and insert Freshly Pressed right after it
                val discoverIndex = orderedList.indexOfFirst { it.isDiscover }
                val insertIndex = if (discoverIndex >= 0) discoverIndex + 1 else orderedList.size
                orderedList.add(insertIndex, ReaderTag(
                    ReaderTag.TAG_SLUG_FRESHLY_PRESSED,
                    ReaderTag.TAG_TITLE_FRESHLY_PRESSED,
                    ReaderTag.TAG_TITLE_FRESHLY_PRESSED,
                    ReaderTag.FRESHLY_PRESSED_PATH,
                    ReaderTagType.DEFAULT
                ))
            }

            orderedList
        }
    }
}
