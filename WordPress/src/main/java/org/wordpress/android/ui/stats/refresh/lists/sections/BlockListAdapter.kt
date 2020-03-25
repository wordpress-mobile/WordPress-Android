package org.wordpress.android.ui.stats.refresh.lists.sections

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView.Adapter
import org.wordpress.android.ui.stats.refresh.BlockDiffCallback
import org.wordpress.android.ui.stats.refresh.BlockDiffCallback.BlockListPayload.EXPAND_CHANGED
import org.wordpress.android.ui.stats.refresh.BlockDiffCallback.BlockListPayload.TAB_CHANGED
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ActivityItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.BarChartItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.BigTitle
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ChartLegend
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Columns
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.DialogButtons
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Empty
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ExpandableItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Header
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ImageItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Information
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Link
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.LoadingItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.MapItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.MapLegend
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.QuickScanItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ReferredItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.TabsItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Tag
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Text
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
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
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.values
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ValueItem
import org.wordpress.android.ui.stats.refresh.lists.sections.viewholders.ActivityViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.viewholders.BarChartViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.viewholders.BigTitleViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.viewholders.BlockListItemViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.viewholders.ChartLegendViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.viewholders.DialogButtonsViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.viewholders.DividerViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.viewholders.EmptyViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.viewholders.ExpandableItemViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.viewholders.FourColumnsViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.viewholders.HeaderViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.viewholders.ImageItemViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.viewholders.InformationViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.viewholders.LinkViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.viewholders.ListItemViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.viewholders.ListItemWithIconViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.viewholders.LoadingItemViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.viewholders.MapLegendViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.viewholders.MapViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.viewholders.QuickScanItemViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.viewholders.ReferredItemViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.viewholders.TabsViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.viewholders.TagViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.viewholders.TextViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.viewholders.TitleViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.viewholders.ValueViewHolder
import org.wordpress.android.util.image.ImageManager

class BlockListAdapter(val imageManager: ImageManager) : Adapter<BlockListItemViewHolder>() {
    private var items: List<BlockListItem> = listOf()
    fun update(newItems: List<BlockListItem>) {
        val diffResult = DiffUtil.calculateDiff(
                BlockDiffCallback(
                        items,
                        newItems
                )
        )
        items = newItems
        diffResult.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, itemType: Int): BlockListItemViewHolder {
        return when (values()[itemType]) {
            TITLE -> TitleViewHolder(parent)
            BIG_TITLE -> BigTitleViewHolder(parent)
            TAG_ITEM -> TagViewHolder(parent)
            IMAGE_ITEM -> ImageItemViewHolder(parent, imageManager)
            LIST_ITEM_WITH_ICON -> ListItemWithIconViewHolder(parent, imageManager)
            LIST_ITEM -> ListItemViewHolder(parent)
            EMPTY -> EmptyViewHolder(parent)
            TEXT -> TextViewHolder(parent)
            COLUMNS -> FourColumnsViewHolder(parent)
            LINK -> LinkViewHolder(parent)
            BAR_CHART -> BarChartViewHolder(parent)
            CHART_LEGEND -> ChartLegendViewHolder(parent)
            TABS -> TabsViewHolder(parent, imageManager)
            INFO -> InformationViewHolder(parent)
            HEADER -> HeaderViewHolder(parent)
            EXPANDABLE_ITEM -> ExpandableItemViewHolder(parent, imageManager)
            DIVIDER -> DividerViewHolder(parent)
            LOADING_ITEM -> LoadingItemViewHolder(parent)
            MAP -> MapViewHolder(parent)
            MAP_LEGEND -> MapLegendViewHolder(parent)
            VALUE_ITEM -> ValueViewHolder(parent)
            ACTIVITY_ITEM -> ActivityViewHolder(parent)
            REFERRED_ITEM -> ReferredItemViewHolder(parent)
            QUICK_SCAN_ITEM -> QuickScanItemViewHolder(parent)
            DIALOG_BUTTONS -> DialogButtonsViewHolder(parent)
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
            is BigTitleViewHolder -> holder.bind(item as BigTitle)
            is TagViewHolder -> holder.bind(item as Tag)
            is ImageItemViewHolder -> holder.bind(item as ImageItem)
            is ValueViewHolder -> holder.bind(item as ValueItem)
            is ListItemWithIconViewHolder -> holder.bind(item as ListItemWithIcon)
            is ListItemViewHolder -> holder.bind(item as ListItem)
            is TextViewHolder -> holder.bind(item as Text)
            is FourColumnsViewHolder -> holder.bind(item as Columns, payloads)
            is LinkViewHolder -> holder.bind(item as Link)
            is BarChartViewHolder -> holder.bind(item as BarChartItem)
            is ChartLegendViewHolder -> holder.bind(item as ChartLegend)
            is TabsViewHolder -> holder.bind(item as TabsItem, payloads.contains(TAB_CHANGED))
            is InformationViewHolder -> holder.bind(item as Information)
            is HeaderViewHolder -> holder.bind(item as Header)
            is ExpandableItemViewHolder -> holder.bind(
                    item as ExpandableItem,
                    payloads.contains(EXPAND_CHANGED)
            )
            is MapViewHolder -> holder.bind(item as MapItem)
            is MapLegendViewHolder -> holder.bind(item as MapLegend)
            is EmptyViewHolder -> holder.bind(item as Empty)
            is ActivityViewHolder -> holder.bind(item as ActivityItem)
            is LoadingItemViewHolder -> holder.bind(item as LoadingItem)
            is ReferredItemViewHolder -> holder.bind(item as ReferredItem)
            is QuickScanItemViewHolder -> holder.bind(item as QuickScanItem)
            is DialogButtonsViewHolder -> holder.bind(item as DialogButtons)
        }
    }

    override fun onBindViewHolder(holder: BlockListItemViewHolder, position: Int) {
        onBindViewHolder(holder, position, listOf())
    }
}
