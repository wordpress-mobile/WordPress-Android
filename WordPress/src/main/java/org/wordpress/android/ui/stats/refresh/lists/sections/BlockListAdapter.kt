package org.wordpress.android.ui.stats.refresh.lists.sections

import android.support.v7.widget.RecyclerView.Adapter
import android.view.ViewGroup
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItemViewHolder.BarChartViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItemViewHolder.ColumnsViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItemViewHolder.EmptyViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItemViewHolder.ExpandableItemViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItemViewHolder.InformationViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItemViewHolder.ListItemWithIconViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItemViewHolder.LabelViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItemViewHolder.LinkViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItemViewHolder.ListItemViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItemViewHolder.TabsViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItemViewHolder.TextViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItemViewHolder.TitleViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItemViewHolder.UserItemViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.BarChartItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Columns
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ExpandableItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Information
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Label
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Link
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.TabsItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Text
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.BAR_CHART
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.COLUMNS
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.EMPTY
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.EXPANDABLE_ITEM
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.INFO
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.LIST_ITEM_WITH_ICON
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.LABEL
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.LINK
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.LIST_ITEM
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.TABS
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.TEXT
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.TITLE
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.USER_ITEM
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.values
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.UserItem
import org.wordpress.android.util.image.ImageManager

class BlockListAdapter(val imageManager: ImageManager) : Adapter<BlockListItemViewHolder>() {
    private var items: List<BlockListItem> = listOf()
    fun update(newItems: List<BlockListItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BlockListItemViewHolder {
        return when (values()[viewType]) {
            TITLE -> TitleViewHolder(parent)
            LIST_ITEM_WITH_ICON -> ListItemWithIconViewHolder(parent, imageManager)
            USER_ITEM -> UserItemViewHolder(parent, imageManager)
            LIST_ITEM -> ListItemViewHolder(parent)
            EMPTY -> EmptyViewHolder(parent)
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

    override fun onBindViewHolder(holder: BlockListItemViewHolder, position: Int) {
        val item = items[position]
        when (holder) {
            is TitleViewHolder -> holder.bind(item as Title)
            is ListItemWithIconViewHolder -> holder.bind(item as ListItemWithIcon)
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
