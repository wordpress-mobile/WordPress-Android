package org.wordpress.android.ui.reader.subfilter

import org.wordpress.android.R
import org.wordpress.android.models.ReaderBlog
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem.ItemType.DIVIDER
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem.ItemType.SECTION_TITLE
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem.ItemType.SITE
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem.ItemType.SITE_ALL
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem.ItemType.TAG
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText

sealed class SubfilterListItem(val type: ItemType, val isTrackedItem: Boolean = false) {
    open var isSelected: Boolean = false
    open val onClickAction: ((filter: SubfilterListItem) -> Unit)? = null
    open val label: UiString? = null

    fun isSameItem(otherItem: SubfilterListItem?): Boolean {
        if (otherItem == null) return false

        return if (type == otherItem.type) {
            when (type) {
                SECTION_TITLE -> label == otherItem.label
                SITE -> (this as Site).blog.isSameBlogOrFeedAs((otherItem as Site).blog)
                TAG -> (this as Tag).tag == (otherItem as Tag).tag
                SITE_ALL,
                DIVIDER -> true
            }
        } else {
            false
        }
    }

    enum class ItemType constructor(val value: Int) {
        SECTION_TITLE(0),
        SITE_ALL(1),
        SITE(2),
        DIVIDER(3),
        TAG(4);

        companion object {
            fun fromInt(value: Int): ItemType? = values().firstOrNull { it.value == value }
        }
    }

    data class SectionTitle(override val label: UiString) : SubfilterListItem(SECTION_TITLE)

    object Divider : SubfilterListItem(DIVIDER)

    data class SiteAll(
        override var isSelected: Boolean = false,
        override val onClickAction: (filter: SubfilterListItem) -> Unit
    ) : SubfilterListItem(SITE_ALL) {
        override val label: UiString = UiStringRes(R.string.reader_filter_cta)
    }

    data class Site(
        override var isSelected: Boolean = false,
        override val onClickAction: (filter: SubfilterListItem) -> Unit,
        val blog: ReaderBlog
    ) : SubfilterListItem(SITE, true) {
        override val label: UiString = if (blog.name.isNotEmpty()) {
            UiStringText(blog.name)
        } else {
            UiStringRes(R.string.reader_untitled_post)
        }
    }

    data class Tag(
        override var isSelected: Boolean = false,
        override val onClickAction: (filter: SubfilterListItem) -> Unit,
        val tag: ReaderTag
    ) : SubfilterListItem(TAG, true) {
        override val label: UiString = UiStringText(tag.tagTitle)
    }
}
