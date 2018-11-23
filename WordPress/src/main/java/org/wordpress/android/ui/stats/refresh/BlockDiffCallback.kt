package org.wordpress.android.ui.stats.refresh

import android.support.v7.util.DiffUtil.Callback
import org.wordpress.android.ui.stats.refresh.BlockListItem.Type.BAR_CHART
import org.wordpress.android.ui.stats.refresh.BlockListItem.Type.COLUMNS
import org.wordpress.android.ui.stats.refresh.BlockListItem.Type.DIVIDER
import org.wordpress.android.ui.stats.refresh.BlockListItem.Type.EMPTY
import org.wordpress.android.ui.stats.refresh.BlockListItem.Type.EXPANDABLE_ITEM
import org.wordpress.android.ui.stats.refresh.BlockListItem.Type.INFO
import org.wordpress.android.ui.stats.refresh.BlockListItem.Type.ITEM
import org.wordpress.android.ui.stats.refresh.BlockListItem.Type.LABEL
import org.wordpress.android.ui.stats.refresh.BlockListItem.Type.LINK
import org.wordpress.android.ui.stats.refresh.BlockListItem.Type.LIST_ITEM
import org.wordpress.android.ui.stats.refresh.BlockListItem.Type.TABS
import org.wordpress.android.ui.stats.refresh.BlockListItem.Type.TEXT
import org.wordpress.android.ui.stats.refresh.BlockListItem.Type.TITLE
import org.wordpress.android.ui.stats.refresh.BlockListItem.Type.USER_ITEM

class BlockDiffCallback(
    private val oldList: List<BlockListItem>,
    private val newList: List<BlockListItem>
) : Callback() {
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val newItem = newList[newItemPosition]
        val oldItem = oldList[oldItemPosition]
        return if (oldItem.type == newItem.type) {
            val type = oldItem.type
            when(type) {
                ITEM,
                USER_ITEM,
                EXPANDABLE_ITEM,
                LIST_ITEM -> oldItem.itemId == newItem.itemId
                BAR_CHART,
                LINK,
                TEXT,
                COLUMNS,
                INFO,
                TABS,
                LABEL,
                TITLE,
                DIVIDER,
                EMPTY -> true
            }
        } else {
            false
        }
    }

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}
