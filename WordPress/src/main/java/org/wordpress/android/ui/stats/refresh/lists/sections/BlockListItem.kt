package org.wordpress.android.ui.stats.refresh.lists.sections

import android.view.View
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon.IconStyle.NORMAL
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
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ValueItem.State.POSITIVE

sealed class BlockListItem(val type: Type) {
    fun id(): Int {
        return type.ordinal + Type.values().size * this.itemId
    }

    open val itemId: Int = 0

    enum class Type {
        TITLE,
        BIG_TITLE,
        TAG_ITEM,
        IMAGE_ITEM,
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
        MAP_LEGEND,
        EXPANDABLE_ITEM,
        DIVIDER,
        LOADING_ITEM,
        ACTIVITY_ITEM,
        REFERRED_ITEM,
        QUICK_SCAN_ITEM,
        DIALOG_BUTTONS
    }

    data class Title(
        @StringRes val textResource: Int? = null,
        val text: String? = null,
        val menuAction: ((View) -> Unit)? = null
    ) : BlockListItem(TITLE)

    data class BigTitle(
        @StringRes val textResource: Int
    ) : BlockListItem(BIG_TITLE)

    data class Tag(
        @StringRes val textResource: Int
    ) : BlockListItem(TAG_ITEM)

    data class ImageItem(
        @DrawableRes val imageResource: Int
    ) : BlockListItem(IMAGE_ITEM)

    data class ReferredItem(
        @StringRes val label: Int,
        val itemTitle: String,
        val navigationAction: NavigationAction? = null
    ) : BlockListItem(REFERRED_ITEM)

    data class ValueItem(
        val value: String,
        @StringRes val unit: Int,
        val isFirst: Boolean = false,
        val change: String? = null,
        val state: State = POSITIVE,
        val contentDescription: String
    ) : BlockListItem(VALUE_ITEM) {
        enum class State { POSITIVE, NEGATIVE, NEUTRAL }
    }

    data class ListItem(
        val text: String,
        val value: String,
        val showDivider: Boolean = true,
        val contentDescription: String
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
        @StringRes val valueResource: Int? = null,
        val value: String? = null,
        val barWidth: Int? = null,
        val showDivider: Boolean = true,
        val textStyle: TextStyle = TextStyle.NORMAL,
        val navigationAction: NavigationAction? = null,
        val contentDescription: String,
        @ColorRes val tint: Int? = null
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

    data class QuickScanItem(val startColumn: Column, val endColumn: Column) : BlockListItem(QUICK_SCAN_ITEM) {
        data class Column(@StringRes val label: Int, val value: String, val tooltip: String? = null)
    }

    data class Information(val text: String) : BlockListItem(INFO)

    data class Text(
        val text: String? = null,
        val textResource: Int? = null,
        val links: List<Clickable>? = null,
        val bolds: List<String>? = null,
        val isLast: Boolean = false
    ) :
            BlockListItem(TEXT) {
        data class Clickable(
            val link: String,
            val navigationAction: NavigationAction
        )
    }

    data class Columns(
        val columns: List<Column>,
        val selectedColumn: Int? = null,
        val onColumnSelected: ((position: Int) -> Unit)? = null
    ) : BlockListItem(COLUMNS) {
        override val itemId: Int
            get() = columns.hashCode()

        data class Column(val header: Int, val value: String, val contentDescription: String)
    }

    data class Link(
        @DrawableRes val icon: Int? = null,
        @StringRes val text: Int,
        val navigateAction: NavigationAction
    ) : BlockListItem(LINK)

    data class DialogButtons(
        @StringRes val positiveButtonText: Int,
        val positiveAction: NavigationAction,
        @StringRes val negativeButtonText: Int,
        val negativeAction: NavigationAction
    ) : BlockListItem(DIALOG_BUTTONS)

    data class BarChartItem(
        val entries: List<Bar>,
        val overlappingEntries: List<Bar>? = null,
        val selectedItem: String? = null,
        val onBarSelected: ((period: String?) -> Unit)? = null,
        val onBarChartDrawn: ((visibleBarCount: Int) -> Unit)? = null,
        val entryContentDescriptions: List<String>
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

    data class Header(@StringRes val startLabel: Int, @StringRes val endLabel: Int) : BlockListItem(HEADER)

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

    data class MapLegend(val startLegend: String, val endLegend: String) : BlockListItem(MAP_LEGEND)

    object Divider : BlockListItem(DIVIDER)

    data class LoadingItem(val loadMore: () -> Unit, val isLoading: Boolean = false) : BlockListItem(LOADING_ITEM)

    data class ActivityItem(val blocks: List<Block>) : BlockListItem(ACTIVITY_ITEM) {
        data class Block(
            val label: String,
            val boxes: List<Box>,
            val contentDescription: String,
            val activityContentDescriptions: List<String>
        )

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
