package org.wordpress.android.ui.reader.subfilter

import androidx.annotation.StringRes
import org.wordpress.android.R
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem.ItemType
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem.ItemType.SITE
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem.ItemType.TAG

enum class SubfilterCategory(@StringRes val titleRes: Int, val type: ItemType) {
    SITES(R.string.reader_filter_by_blog_title, SITE),
    TAGS(R.string.reader_filter_by_tag_title, TAG);
}
