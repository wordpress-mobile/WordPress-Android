package org.wordpress.android.ui.stats.refresh.lists.sections

import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView.Adapter
import android.view.ViewGroup
import org.wordpress.android.ui.stats.refresh.BlockDiffCallback
import org.wordpress.android.ui.stats.refresh.BlockDiffCallback.BlockListPayload.EXPAND_CHANGED
import org.wordpress.android.ui.stats.refresh.BlockDiffCallback.BlockListPayload.SELECTED_BAR_CHANGED
import org.wordpress.android.ui.stats.refresh.BlockDiffCallback.BlockListPayload.TAB_CHANGED
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.BackgroundInformation
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.BarChartItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Columns
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ExpandableItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Header
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Information
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Link
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.MapItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.TabsItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Text
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.BACKGROUND_INFO
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.BAR_CHART
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.CENTERED_COLUMNS
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.DIVIDER
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.EMPTY
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.EXPANDABLE_ITEM
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.HEADER
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.INFO
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.LEFT_COLUMNS
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.LINK
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.LIST_ITEM
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.LIST_ITEM_WITH_ICON
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.MAP
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.TABS
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.TEXT
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.TITLE
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.values
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItemViewHolder.BackgroundInformationViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItemViewHolder.BarChartViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItemViewHolder.CenteredColumnsViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItemViewHolder.DividerViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItemViewHolder.EmptyViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItemViewHolder.ExpandableItemViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItemViewHolder.HeaderViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItemViewHolder.InformationViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItemViewHolder.LeftColumnsViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItemViewHolder.LinkViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItemViewHolder.ListItemViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItemViewHolder.ListItemWithIconViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItemViewHolder.MapViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItemViewHolder.TabsViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItemViewHolder.TextViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItemViewHolder.TitleViewHolder
import org.wordpress.android.util.image.ImageManager

class BlockListAdapter(val imageManager: ImageManager) : Adapter<BlockListItemViewHolder>() {
    private var items: List<BlockListItem> = listOf()
    fun update(newItems: List<BlockListItem>) {
        // We're using a nested recycler view here. When we try to update a block with new data, there are 2 animations
        // happening at the same time. The outer recycler view is animating the block height change and the inner
        // recycler view is animating items being added or removed. The problem is that only the inner animation
        // is actually animating, the block size happens immediately. This looks OK for adding new items to the list
        // or when the size of the list doesn't change. In that case the block height increases immediately and the
        // inner list changes are animated.
        // However, when we want to remove items, the block height decreases immediately so the bottom items disappear
        // and then the animation happens and they slowly slide back into view. This doesn't look good. We couldn't
        // find a solution that handles this case well so we're falling back to notifying the whole list instead of
        // animating changes when we decrease the number of items. When we do that, the outer animation happens and
        // the items are replaced immediately.
        if (newItems.size >= items.size) {
            val diffResult = DiffUtil.calculateDiff(
                    BlockDiffCallback(
                            items,
                            newItems
                    )
            )
            items = newItems
            diffResult.dispatchUpdatesTo(this)
        } else {
            items = newItems
            notifyDataSetChanged()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, itemType: Int): BlockListItemViewHolder {
        return when (values()[itemType]) {
            TITLE -> TitleViewHolder(parent)
            LIST_ITEM_WITH_ICON -> ListItemWithIconViewHolder(parent, imageManager)
            LIST_ITEM -> ListItemViewHolder(parent)
            EMPTY -> EmptyViewHolder(parent)
            TEXT -> TextViewHolder(parent)
            CENTERED_COLUMNS -> CenteredColumnsViewHolder(parent)
            LEFT_COLUMNS -> LeftColumnsViewHolder(parent)
            LINK -> LinkViewHolder(parent)
            BAR_CHART -> BarChartViewHolder(parent)
            TABS -> TabsViewHolder(parent, imageManager)
            INFO -> InformationViewHolder(parent)
            BACKGROUND_INFO -> BackgroundInformationViewHolder(parent)
            HEADER -> HeaderViewHolder(parent)
            EXPANDABLE_ITEM -> ExpandableItemViewHolder(parent, imageManager)
            DIVIDER -> DividerViewHolder(parent)
            MAP -> MapViewHolder(parent)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return items[position].type.ordinal
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: BlockListItemViewHolder, position: Int, payloads: List<Any>) {
        val item = items[position]
        when (holder) {
            is TitleViewHolder -> holder.bind(item as Title)
            is ListItemWithIconViewHolder -> holder.bind(item as ListItemWithIcon)
            is ListItemViewHolder -> holder.bind(item as ListItem)
            is TextViewHolder -> holder.bind(item as Text)
            is CenteredColumnsViewHolder -> holder.bind(item as Columns, payloads)
            is LeftColumnsViewHolder -> holder.bind(item as Columns, payloads)
            is LinkViewHolder -> holder.bind(item as Link)
            is BarChartViewHolder -> holder.bind(item as BarChartItem, payloads.contains(SELECTED_BAR_CHANGED))
            is TabsViewHolder -> holder.bind(item as TabsItem, payloads.contains(TAB_CHANGED))
            is InformationViewHolder -> holder.bind(item as Information)
            is BackgroundInformationViewHolder -> holder.bind(item as BackgroundInformation)
            is HeaderViewHolder -> holder.bind(item as Header)
            is ExpandableItemViewHolder -> holder.bind(
                    item as ExpandableItem,
                    payloads.contains(EXPAND_CHANGED))
            is MapViewHolder -> holder.bind(item as MapItem)
        }
    }

    override fun onBindViewHolder(holder: BlockListItemViewHolder, position: Int) {
        onBindViewHolder(holder, position, listOf())
    }
}
