package org.wordpress.android.ui.reader.subfilter

import org.wordpress.android.models.ReaderBlog
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem.ItemType.DIVIDER
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem.ItemType.SECTION_TITLE
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem.ItemType.SITE
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem.ItemType.SITE_ALL
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem.ItemType.TAG
import org.wordpress.android.ui.utils.UiString

sealed class SubfilterListItem(val type: ItemType) {
    open var isSelected: Boolean = false
    open val onClickAction: ((filter: SubfilterListItem) -> Unit)? = null
    open val label: UiString? = null

    enum class ItemType {
        SECTION_TITLE,
        SITE_ALL,
        SITE,
        DIVIDER,
        TAG
    }

    data class SectionTitle(override val label: UiString) : SubfilterListItem(SECTION_TITLE)

    object Divider : SubfilterListItem(DIVIDER)

    data class SiteAll(
        override val label: UiString,
        override var isSelected: Boolean = false,
        override val onClickAction: (filter: SubfilterListItem) -> Unit
    ) : SubfilterListItem(SITE_ALL)

    data class Site(
        override val label: UiString,
        override var isSelected: Boolean = false,
        override val onClickAction: (filter: SubfilterListItem) -> Unit,
        val blog: ReaderBlog
    ) : SubfilterListItem(SITE)

    data class Tag(
        override val label: UiString,
        override var isSelected: Boolean = false,
        override val onClickAction: (filter: SubfilterListItem) -> Unit,
        val tag: ReaderTag
    ) : SubfilterListItem(TAG)
}
