package org.wordpress.android.ui.reader.subfilter.viewholders

import androidx.recyclerview.widget.DiffUtil.Callback
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem.ItemType.DIVIDER
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem.ItemType.SECTION_TITLE
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem.ItemType.SITE
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem.ItemType.SITE_ALL
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem.ItemType.TAG
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem.Site
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem.Tag

class SubFilterDiffCallback(
    private val oldList: List<SubfilterListItem>,
    private val newList: List<SubfilterListItem>
) : Callback() {
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val newItem = newList[newItemPosition]
        val oldItem = oldList[oldItemPosition]

        return if (oldItem.type == newItem.type) {
            when (oldItem.type) {
                SECTION_TITLE -> oldItem.label == newItem.label
                SITE -> (oldItem as Site).blog.isSameBlogOrFeedAs((newItem as Site).blog)
                TAG -> (oldItem as Tag).tag == (newItem as Tag).tag
                SITE_ALL,
                DIVIDER -> true
            }
        } else {
            false
        }
    }

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areContentsTheSame(
        oldItemPosition: Int,
        newItemPosition: Int
    ): Boolean = oldList[oldItemPosition].isSelected == newList[newItemPosition].isSelected
}
