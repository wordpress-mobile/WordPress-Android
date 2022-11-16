package org.wordpress.android.localcontentmigration

import org.wordpress.android.datasets.wrappers.ReaderPostTableWrapper
import org.wordpress.android.localcontentmigration.LocalContentEntityData.ReaderPostsData
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.ReaderTagType.BOOKMARKED
import javax.inject.Inject

class ReaderSavedPostsProviderHelper @Inject constructor(
    private val readerPostTableWrapper: ReaderPostTableWrapper,
): LocalDataProviderHelper {
    override fun getData(localSiteId: Int?, localEntityId: Int?): LocalContentEntityData =
            readerPostTableWrapper.getPostsWithTag(
                    readerTag = ReaderTag("", "", "", "", BOOKMARKED),
                    maxRows = 0,
                    excludeTextColumn = false
            ).let { ReaderPostsData(posts = it) }
}
