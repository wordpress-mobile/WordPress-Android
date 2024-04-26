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
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ChartLegendsBlue
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ChartLegendsPurple
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Chips
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Columns
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.DialogButtons
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Empty
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ExpandableItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Header
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ImageItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Information
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.LineChartItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Link
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemActionCard
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemGuideCard
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithImage
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.LoadingItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.MapItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.MapLegend
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.PieChartItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.QuickScanItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ReferredItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.SubscribersChartItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.TabsItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Tag
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Text
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.TitleWithMore
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.ACTION_CARD
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.ACTIVITY_ITEM
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.BAR_CHART
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.BIG_TITLE
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.CHART_LEGEND
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.CHART_LEGENDS_BLUE
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.CHART_LEGENDS_PURPLE
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.CHIPS
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.COLUMNS
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.DIALOG_BUTTONS
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.DIVIDER
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.EMPTY
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.EXPANDABLE_ITEM
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.GUIDE_CARD
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.HEADER
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.IMAGE_ITEM
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.INFO
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.LINE_CHART
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.LINK
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.LIST_ITEM
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.LIST_ITEM_WITH_ICON
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.LIST_ITEM_WITH_IMAGE
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.LOADING_ITEM
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.MAP
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.MAP_LEGEND
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.PIE_CHART
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.QUICK_SCAN_ITEM
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.REFERRED_ITEM
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.SUBSCRIBERS_CHART
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.TABS
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.TAG_ITEM
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.TEXT
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.TITLE
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.TITLE_WITH_MORE
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.VALUES_ITEM
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.VALUE_ITEM
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.VALUE_WITH_CHART_ITEM
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ValueItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ValueWithChartItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ValuesItem
import org.wordpress.android.ui.stats.refresh.lists.sections.viewholders.ActionCardViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.viewholders.ActivityViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.viewholders.BarChartViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.viewholders.BigTitleViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.viewholders.BlockListItemViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.viewholders.ChartLegendViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.viewholders.ChartLegendsBlueViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.viewholders.ChartLegendsPurpleViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.viewholders.ChipsViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.viewholders.DialogButtonsViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.viewholders.DividerViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.viewholders.EmptyViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.viewholders.ExpandableItemViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.viewholders.FourColumnsViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.viewholders.GuideCardViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.viewholders.HeaderViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.viewholders.ImageItemViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.viewholders.InformationViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.viewholders.LineChartViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.viewholders.LinkViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.viewholders.ListItemViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.viewholders.ListItemWithIconViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.viewholders.ListItemWithImageViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.viewholders.LoadingItemViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.viewholders.MapLegendViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.viewholders.MapViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.viewholders.PieChartViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.viewholders.QuickScanItemViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.viewholders.ReferredItemViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.viewholders.SubscribersChartViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.viewholders.TabsViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.viewholders.TagViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.viewholders.TextViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.viewholders.TitleViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.viewholders.TitleWithMoreViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.viewholders.ValueViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.viewholders.ValueWithChartViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.viewholders.ValuesViewHolder
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
        return when (BlockListItem.Type.entries[itemType]) {
            TITLE -> TitleViewHolder(parent)
            TITLE_WITH_MORE -> TitleWithMoreViewHolder(parent)
            BIG_TITLE -> BigTitleViewHolder(parent)
            TAG_ITEM -> TagViewHolder(parent)
            IMAGE_ITEM -> ImageItemViewHolder(parent, imageManager)
            LIST_ITEM_WITH_IMAGE -> ListItemWithImageViewHolder(parent, imageManager = imageManager)
            LIST_ITEM_WITH_ICON -> ListItemWithIconViewHolder(parent, imageManager)
            LIST_ITEM -> ListItemViewHolder(parent)
            EMPTY -> EmptyViewHolder(parent)
            TEXT -> TextViewHolder(parent)
            COLUMNS -> FourColumnsViewHolder(parent)
            CHIPS -> ChipsViewHolder(parent)
            LINK -> LinkViewHolder(parent)
            BAR_CHART -> BarChartViewHolder(parent)
            PIE_CHART -> PieChartViewHolder(parent)
            LINE_CHART -> LineChartViewHolder(parent)
            SUBSCRIBERS_CHART -> SubscribersChartViewHolder(parent)
            CHART_LEGEND -> ChartLegendViewHolder(parent)
            CHART_LEGENDS_BLUE -> ChartLegendsBlueViewHolder(parent)
            CHART_LEGENDS_PURPLE -> ChartLegendsPurpleViewHolder(parent)
            TABS -> TabsViewHolder(parent, imageManager)
            INFO -> InformationViewHolder(parent)
            HEADER -> HeaderViewHolder(parent)
            EXPANDABLE_ITEM -> ExpandableItemViewHolder(parent, imageManager)
            DIVIDER -> DividerViewHolder(parent)
            LOADING_ITEM -> LoadingItemViewHolder(parent)
            MAP -> MapViewHolder(parent)
            MAP_LEGEND -> MapLegendViewHolder(parent)
            VALUE_ITEM -> ValueViewHolder(parent)
            VALUE_WITH_CHART_ITEM -> ValueWithChartViewHolder(parent)
            VALUES_ITEM -> ValuesViewHolder(parent)
            ACTIVITY_ITEM -> ActivityViewHolder(parent)
            REFERRED_ITEM -> ReferredItemViewHolder(parent)
            QUICK_SCAN_ITEM -> QuickScanItemViewHolder(parent)
            DIALOG_BUTTONS -> DialogButtonsViewHolder(parent)
            ACTION_CARD -> ActionCardViewHolder(parent)
            GUIDE_CARD -> GuideCardViewHolder(parent)
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
            is TitleWithMoreViewHolder -> holder.bind(item as TitleWithMore)
            is BigTitleViewHolder -> holder.bind(item as BigTitle)
            is TagViewHolder -> holder.bind(item as Tag)
            is ImageItemViewHolder -> holder.bind(item as ImageItem)
            is ValueViewHolder -> holder.bind(item as ValueItem)
            is ValueWithChartViewHolder -> holder.bind(item as ValueWithChartItem)
            is ValuesViewHolder -> holder.bind(item as ValuesItem)
            is ListItemWithImageViewHolder -> holder.bind(item as ListItemWithImage)
            is ListItemWithIconViewHolder -> holder.bind(item as ListItemWithIcon)
            is ListItemViewHolder -> holder.bind(item as ListItem)
            is TextViewHolder -> holder.bind(item as Text)
            is FourColumnsViewHolder -> holder.bind(item as Columns, payloads)
            is ChipsViewHolder -> holder.bind(item as Chips)
            is LinkViewHolder -> holder.bind(item as Link)
            is BarChartViewHolder -> holder.bind(item as BarChartItem)
            is PieChartViewHolder -> holder.bind(item as PieChartItem)
            is LineChartViewHolder -> holder.bind(item as LineChartItem)
            is SubscribersChartViewHolder -> holder.bind(item as SubscribersChartItem)
            is ChartLegendViewHolder -> holder.bind(item as ChartLegend)
            is ChartLegendsBlueViewHolder -> holder.bind(item as ChartLegendsBlue)
            is ChartLegendsPurpleViewHolder -> holder.bind(item as ChartLegendsPurple)
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
            is ActionCardViewHolder -> holder.bind(item as ListItemActionCard)
            is GuideCardViewHolder -> holder.bind(item as ListItemGuideCard)
        }
    }

    override fun onBindViewHolder(holder: BlockListItemViewHolder, position: Int) {
        onBindViewHolder(holder, position, listOf())
    }
}
