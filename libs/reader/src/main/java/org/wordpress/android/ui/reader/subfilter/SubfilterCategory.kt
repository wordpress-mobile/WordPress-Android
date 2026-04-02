package org.wordpress.android.ui.reader.subfilter

import androidx.annotation.StringRes
import org.wordpress.android.reader.R as ReaderR
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem.ItemType
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem.ItemType.SITE
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem.ItemType.TAG

enum class SubfilterCategory(
    @StringRes val titleRes: Int,
    val type: ItemType
) {
    SITES(ReaderR.string.reader_filter_by_blog_title, SITE),
    TAGS(ReaderR.string.reader_filter_by_tag_title, TAG);
}
