package org.wordpress.android.datasets.wrappers

import dagger.Reusable
import org.wordpress.android.datasets.ReaderTagTable
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.ReaderTagList
import javax.inject.Inject

@Reusable
class ReaderTagTableWrapper @Inject constructor() {
    fun shouldAutoUpdateTag(readerTag: ReaderTag): Boolean =
            ReaderTagTable.shouldAutoUpdateTag(readerTag)

    fun setTagLastUpdated(tag: ReaderTag) = ReaderTagTable.setTagLastUpdated(tag)

    fun getFollowedTags(): ReaderTagList = ReaderTagTable.getFollowedTags()
}
