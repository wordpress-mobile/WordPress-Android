package org.wordpress.android.ui.stats.refresh

import androidx.recyclerview.widget.DiffUtil.Callback
import org.wordpress.android.ui.stats.refresh.BlockDiffCallback.BlockListPayload.COLUMNS_VALUE_CHANGED
import org.wordpress.android.ui.stats.refresh.BlockDiffCallback.BlockListPayload.EXPAND_CHANGED
import org.wordpress.android.ui.stats.refresh.BlockDiffCallback.BlockListPayload.SELECTED_BAR_CHANGED
import org.wordpress.android.ui.stats.refresh.BlockDiffCallback.BlockListPayload.SELECTED_COLUMN_CHANGED
import org.wordpress.android.ui.stats.refresh.BlockDiffCallback.BlockListPayload.TAB_CHANGED
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.BarChartItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Columns
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ExpandableItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.TabsItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.ACTIVITY_ITEM
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.BAR_CHART
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.BIG_TITLE
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.CHART_LEGEND
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.COLUMNS
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.DIALOG_BUTTONS
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.DIVIDER
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.EMPTY
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.EXPANDABLE_ITEM
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.HEADER
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.IMAGE_ITEM
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.INFO
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.LINK
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.LIST_ITEM
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.LIST_ITEM_WITH_ICON
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.LOADING_ITEM
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.MAP
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.MAP_LEGEND
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.QUICK_SCAN_ITEM
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.REFERRED_ITEM
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.TABS
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.TAG_ITEM
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.TEXT
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.TITLE
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.VALUE_ITEM

class BlockDiffCallback(
    private val oldList: List<BlockListItem>,
    private val newList: List<BlockListItem>
) : Callback() {
    enum class BlockListPayload {
        EXPAND_CHANGED, TAB_CHANGED, SELECTED_COLUMN_CHANGED, SELECTED_BAR_CHANGED, COLUMNS_VALUE_CHANGED
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val newItem = newList[newItemPosition]
        val oldItem = oldList[oldItemPosition]
        return if (oldItem.type == newItem.type) {
            when (oldItem.type) {
                LIST_ITEM_WITH_ICON,
                EXPANDABLE_ITEM,
                TABS,
                COLUMNS,
                BAR_CHART,
                ACTIVITY_ITEM,
                LIST_ITEM -> oldItem.itemId == newItem.itemId
                LINK,
                TEXT,
                INFO,
                HEADER,
                TITLE,
                BIG_TITLE,
                VALUE_ITEM,
                DIVIDER,
                LOADING_ITEM,
                MAP,
                MAP_LEGEND,
                CHART_LEGEND,
                REFERRED_ITEM,
                QUICK_SCAN_ITEM,
                DIALOG_BUTTONS,
                TAG_ITEM,
                IMAGE_ITEM,
                EMPTY -> oldItem == newItem
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

    override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
        val newItem = newList[newItemPosition]
        val oldItem = oldList[oldItemPosition]
        return when {
            newItem is ExpandableItem && oldItem is ExpandableItem && oldItem.isExpanded != newItem.isExpanded ->
                EXPAND_CHANGED
            newItem is TabsItem && oldItem is TabsItem && oldItem.selectedTabPosition != newItem.selectedTabPosition ->
                TAB_CHANGED
            newItem is Columns && oldItem is Columns && oldItem.selectedColumn != newItem.selectedColumn ->
                return SELECTED_COLUMN_CHANGED
            newItem is Columns && oldItem is Columns && oldItem.columns != newItem.columns ->
                return COLUMNS_VALUE_CHANGED
            newItem is BarChartItem && oldItem is BarChartItem && oldItem.selectedItem != newItem.selectedItem ->
                return SELECTED_BAR_CHANGED
            else -> null
        }
    }
}
