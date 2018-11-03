package org.wordpress.android.ui.stats.refresh

import android.support.v7.widget.RecyclerView.Adapter
import android.view.ViewGroup
import org.wordpress.android.ui.stats.refresh.BlockItemViewHolder.BarChartViewHolder
import org.wordpress.android.ui.stats.refresh.BlockItemViewHolder.ColumnsViewHolder
import org.wordpress.android.ui.stats.refresh.BlockItemViewHolder.EmptyViewHolder
import org.wordpress.android.ui.stats.refresh.BlockItemViewHolder.ItemViewHolder
import org.wordpress.android.ui.stats.refresh.BlockItemViewHolder.LinkViewHolder
import org.wordpress.android.ui.stats.refresh.BlockItemViewHolder.TextViewHolder
import org.wordpress.android.ui.stats.refresh.BlockItemViewHolder.TitleViewHolder
import org.wordpress.android.ui.stats.refresh.BlockListItem.BarChartItem
import org.wordpress.android.ui.stats.refresh.BlockListItem.Columns
import org.wordpress.android.ui.stats.refresh.BlockListItem.Item
import org.wordpress.android.ui.stats.refresh.BlockListItem.Link
import org.wordpress.android.ui.stats.refresh.BlockListItem.Text
import org.wordpress.android.ui.stats.refresh.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.BlockListItem.Type.BAR_CHART
import org.wordpress.android.ui.stats.refresh.BlockListItem.Type.COLUMNS
import org.wordpress.android.ui.stats.refresh.BlockListItem.Type.EMPTY
import org.wordpress.android.ui.stats.refresh.BlockListItem.Type.ITEM
import org.wordpress.android.ui.stats.refresh.BlockListItem.Type.LINK
import org.wordpress.android.ui.stats.refresh.BlockListItem.Type.TEXT
import org.wordpress.android.ui.stats.refresh.BlockListItem.Type.TITLE
import org.wordpress.android.ui.stats.refresh.BlockListItem.Type.values

class BlockListAdapter : Adapter<BlockItemViewHolder>() {
    private var items: List<BlockListItem> = listOf()
    fun update(newItems: List<BlockListItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BlockItemViewHolder {
        return when (values()[viewType]) {
            TITLE -> TitleViewHolder(parent)
            ITEM -> ItemViewHolder(parent)
            EMPTY -> EmptyViewHolder(parent)
            TEXT -> TextViewHolder(parent)
            COLUMNS -> ColumnsViewHolder(parent)
            LINK -> LinkViewHolder(parent)
            BAR_CHART -> BarChartViewHolder(parent)
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
            is TextViewHolder -> holder.bind(item as Text)
            is ColumnsViewHolder -> holder.bind(item as Columns)
            is LinkViewHolder -> holder.bind(item as Link)
            is BarChartViewHolder -> holder.bind(item as BarChartItem)
        }
    }
}
