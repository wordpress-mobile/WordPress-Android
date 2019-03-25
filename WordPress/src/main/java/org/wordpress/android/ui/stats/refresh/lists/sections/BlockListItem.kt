package org.wordpress.android.ui.stats.refresh.lists.sections

import android.support.annotation.DrawableRes
import android.support.annotation.StringRes
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon.IconStyle.NORMAL
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.ACTIVITY_ITEM
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.BAR_CHART
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.CHART_LEGEND
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.COLUMNS
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.DIVIDER
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.EMPTY
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.EXPANDABLE_ITEM
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.HEADER
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.INFO
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.LINK
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.LIST_ITEM
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.LIST_ITEM_WITH_ICON
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.LOADING_ITEM
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.MAP
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.REFERRED_ITEM
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.QUICK_SCAN_ITEM
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.TABS
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.TEXT
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.TITLE
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.VALUE_ITEM

sealed class BlockListItem(val type: Type) {
    fun id(): Int {
        return type.ordinal + Type.values().size * this.itemId
    }

    open val itemId: Int = 0

    enum class Type {
        TITLE,
        VALUE_ITEM,
        LIST_ITEM,
        LIST_ITEM_WITH_ICON,
        INFO,
        EMPTY,
        TEXT,
        COLUMNS,
        LINK,
        BAR_CHART,
        CHART_LEGEND,
        TABS,
        HEADER,
        MAP,
        EXPANDABLE_ITEM,
        DIVIDER,
        LOADING_ITEM,
        ACTIVITY_ITEM,
        REFERRED_ITEM,
        QUICK_SCAN_ITEM
    }

    data class Title(@StringRes val textResource: Int? = null, val text: String? = null) : BlockListItem(TITLE)

    data class ReferredItem(@StringRes val label: Int, val itemTitle: String) : BlockListItem(REFERRED_ITEM)

    data class ValueItem(
        val value: String,
        @StringRes val unit: Int,
        val isFirst: Boolean = false,
        val change: String? = null,
        val positive: Boolean = true
    ) : BlockListItem(VALUE_ITEM)

    data class ListItem(
        val text: String,
        val value: String,
        val showDivider: Boolean = true
    ) : BlockListItem(LIST_ITEM) {
        override val itemId: Int
            get() = text.hashCode()
    }

    data class ListItemWithIcon(
        @DrawableRes val icon: Int? = null,
        val iconUrl: String? = null,
        val iconStyle: IconStyle = NORMAL,
        @StringRes val textResource: Int? = null,
        val text: String? = null,
        @StringRes val subTextResource: Int? = null,
        val subText: String? = null,
        @StringRes val valueResource: Int? = null,
        val value: String? = null,
        val showDivider: Boolean = true,
        val textStyle: TextStyle = TextStyle.NORMAL,
        val navigationAction: NavigationAction? = null
    ) : BlockListItem(LIST_ITEM_WITH_ICON) {
        override val itemId: Int
            get() = (icon ?: 0) + (iconUrl?.hashCode() ?: 0) + (textResource ?: 0) + (text?.hashCode() ?: 0)

        enum class IconStyle {
            NORMAL, AVATAR, EMPTY_SPACE
        }

        enum class TextStyle {
            NORMAL, LIGHT
        }
    }

    data class QuickScanItem(val leftColumn: Column, val rightColumn: Column) : BlockListItem(QUICK_SCAN_ITEM) {
        data class Column(@StringRes val label: Int, val value: String, val tooltip: String? = null)
    }

    data class Information(val text: String) : BlockListItem(INFO)

    data class Text(
        val text: String? = null,
        val textResource: Int? = null,
        val links: List<Clickable>? = null,
        val isLast: Boolean = false
    ) :
            BlockListItem(TEXT) {
        data class Clickable(
            val link: String,
            val navigationAction: NavigationAction
        )
    }

    data class Columns(
        val headers: List<Int>,
        val values: List<String>,
        val selectedColumn: Int? = null,
        val onColumnSelected: ((position: Int) -> Unit)? = null
    ) : BlockListItem(COLUMNS) {
        override val itemId: Int
            get() = headers.hashCode()
    }

    data class Link(
        @DrawableRes val icon: Int? = null,
        @StringRes val text: Int,
        val navigateAction: NavigationAction
    ) :
            BlockListItem(LINK)

    data class BarChartItem(
        val entries: List<Bar>,
        val overlappingEntries: List<Bar>? = null,
        val selectedItem: String? = null,
        val onBarSelected: ((period: String?) -> Unit)? = null,
        val onBarChartDrawn: ((visibleBarCount: Int) -> Unit)? = null
    ) : BlockListItem(BAR_CHART) {
        data class Bar(val label: String, val id: String, val value: Int)

        override val itemId: Int
            get() = entries.hashCode()
    }

    data class ChartLegend(@StringRes val text: Int) : BlockListItem(CHART_LEGEND)

    data class TabsItem(val tabs: List<Int>, val selectedTabPosition: Int, val onTabSelected: (position: Int) -> Unit) :
            BlockListItem(TABS) {
        override val itemId: Int
            get() = tabs.hashCode()
    }

    data class Header(@StringRes val leftLabel: Int, @StringRes val rightLabel: Int) : BlockListItem(HEADER)

    data class ExpandableItem(
        val header: ListItemWithIcon,
        val isExpanded: Boolean,
        val onExpandClicked: (isExpanded: Boolean) -> Unit
    ) : BlockListItem(
            EXPANDABLE_ITEM
    ) {
        override val itemId: Int
            get() = header.itemId
    }

    data class Empty(@StringRes val textResource: Int? = null, val text: String? = null) : BlockListItem(EMPTY)

    data class MapItem(val mapData: String, @StringRes val label: Int) : BlockListItem(MAP)

    object Divider : BlockListItem(DIVIDER)

    data class LoadingItem(val loadMore: () -> Unit, val isLoading: Boolean = false) : BlockListItem(LOADING_ITEM)

    data class ActivityItem(val blocks: List<Block>) : BlockListItem(ACTIVITY_ITEM) {
        data class Block(val label: String, val boxes: List<Box>)
        enum class Box {
            INVISIBLE, VERY_LOW, LOW, MEDIUM, HIGH, VERY_HIGH
        }

        override val itemId: Int
            get() = blocks.fold(0) { acc, block -> acc + block.label.hashCode() }
    }

    interface NavigationAction {
        fun click()

        companion object {
            fun create(action: () -> Unit): NavigationAction {
                return NoParams(action)
            }

            fun <T> create(data: T, action: (T) -> Unit): NavigationAction {
                return OneParam(data, action)
            }
        }

        private data class OneParam<T>(
            val data: T,
            val action: (T) -> Unit
        ) : NavigationAction {
            override fun click() {
                action(data)
            }
        }

        private data class NoParams(
            val action: () -> Unit
        ) : NavigationAction {
            override fun click() {
                action()
            }
        }
    }
}
