package org.wordpress.android.ui.stats.refresh

import android.support.v7.widget.RecyclerView.Adapter
import android.view.ViewGroup
import org.wordpress.android.ui.stats.refresh.BlockItemViewHolder.BarChartViewHolder
import org.wordpress.android.ui.stats.refresh.BlockItemViewHolder.ColumnsViewHolder
import org.wordpress.android.ui.stats.refresh.BlockItemViewHolder.DividerViewHolder
import org.wordpress.android.ui.stats.refresh.BlockItemViewHolder.EmptyViewHolder
import org.wordpress.android.ui.stats.refresh.BlockItemViewHolder.ExpandableItemViewHolder
import org.wordpress.android.ui.stats.refresh.BlockItemViewHolder.InformationViewHolder
import org.wordpress.android.ui.stats.refresh.BlockItemViewHolder.ItemViewHolder
import org.wordpress.android.ui.stats.refresh.BlockItemViewHolder.LabelViewHolder
import org.wordpress.android.ui.stats.refresh.BlockItemViewHolder.LinkViewHolder
import org.wordpress.android.ui.stats.refresh.BlockItemViewHolder.ListItemViewHolder
import org.wordpress.android.ui.stats.refresh.BlockItemViewHolder.TabsViewHolder
import org.wordpress.android.ui.stats.refresh.BlockItemViewHolder.TextViewHolder
import org.wordpress.android.ui.stats.refresh.BlockItemViewHolder.TitleViewHolder
import org.wordpress.android.ui.stats.refresh.BlockItemViewHolder.UserItemViewHolder
import org.wordpress.android.ui.stats.refresh.BlockListItem.BarChartItem
import org.wordpress.android.ui.stats.refresh.BlockListItem.Columns
import org.wordpress.android.ui.stats.refresh.BlockListItem.ExpandableItem
import org.wordpress.android.ui.stats.refresh.BlockListItem.Information
import org.wordpress.android.ui.stats.refresh.BlockListItem.Item
import org.wordpress.android.ui.stats.refresh.BlockListItem.Label
import org.wordpress.android.ui.stats.refresh.BlockListItem.Link
import org.wordpress.android.ui.stats.refresh.BlockListItem.ListItem
import org.wordpress.android.ui.stats.refresh.BlockListItem.TabsItem
import org.wordpress.android.ui.stats.refresh.BlockListItem.Text
import org.wordpress.android.ui.stats.refresh.BlockListItem.Title
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
import org.wordpress.android.ui.stats.refresh.BlockListItem.Type.values
import org.wordpress.android.ui.stats.refresh.BlockListItem.UserItem
import org.wordpress.android.util.image.ImageManager

class BlockListAdapter(val imageManager: ImageManager) : Adapter<BlockItemViewHolder>() {
    private var items: List<BlockListItem> = listOf()
    fun update(newItems: List<BlockListItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BlockItemViewHolder {
        return when (values()[viewType]) {
            TITLE -> TitleViewHolder(parent)
            ITEM -> ItemViewHolder(parent, imageManager)
            USER_ITEM -> UserItemViewHolder(parent, imageManager)
            LIST_ITEM -> ListItemViewHolder(parent)
            EMPTY -> EmptyViewHolder(parent)
            DIVIDER -> DividerViewHolder(parent)
            TEXT -> TextViewHolder(parent)
            COLUMNS -> ColumnsViewHolder(parent)
            LINK -> LinkViewHolder(parent)
            BAR_CHART -> BarChartViewHolder(parent)
            TABS -> TabsViewHolder(parent, imageManager)
            INFO -> InformationViewHolder(parent)
            LABEL -> LabelViewHolder(parent)
            EXPANDABLE_ITEM -> ExpandableItemViewHolder(parent, imageManager)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return items[position].type.ordinal
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: BlockItemViewHolder, position: Int) {
        val item = items[position]
        when (holder) {
            is TitleViewHolder -> holder.bind(item as Title)
            is ItemViewHolder -> holder.bind(item as Item)
            is UserItemViewHolder -> holder.bind(item as UserItem)
            is ListItemViewHolder -> holder.bind(item as ListItem)
            is TextViewHolder -> holder.bind(item as Text)
            is ColumnsViewHolder -> holder.bind(item as Columns)
            is LinkViewHolder -> holder.bind(item as Link)
            is BarChartViewHolder -> holder.bind(item as BarChartItem)
            is TabsViewHolder -> holder.bind(item as TabsItem)
            is InformationViewHolder -> holder.bind(item as Information)
            is LabelViewHolder -> holder.bind(item as Label)
            is ExpandableItemViewHolder -> holder.bind(item as ExpandableItem)
        }
    }
}
