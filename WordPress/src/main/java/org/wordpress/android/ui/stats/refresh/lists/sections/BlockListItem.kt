package org.wordpress.android.ui.stats.refresh.lists.sections

import android.support.annotation.DrawableRes
import android.support.annotation.StringRes
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.BAR_CHART
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.COLUMNS
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.DIVIDER
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.EMPTY
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.EXPANDABLE_ITEM
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.INFO
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.LABEL
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.LINK
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.LIST_ITEM
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.LIST_ITEM_WITH_ICON
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.TABS
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.TEXT
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.TITLE
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.USER_ITEM

sealed class BlockListItem(val type: Type) {
    fun id(): Int {
        return type.ordinal + Type.values().size * this.itemId
    }

    open val itemId: Int = 0

    enum class Type {
        TITLE,
        LIST_ITEM,
        LIST_ITEM_WITH_ICON,
        USER_ITEM,
        INFO,
        EMPTY,
        TEXT,
        COLUMNS,
        LINK,
        BAR_CHART,
        TABS,
        LABEL,
        EXPANDABLE_ITEM,
        DIVIDER
    }

    data class Title(@StringRes val text: Int) : BlockListItem(TITLE)

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
        @StringRes val textResource: Int? = null,
        val text: String? = null,
        @StringRes val subTextResource: Int? = null,
        val subText: String? = null,
        @StringRes val valueResource: Int? = null,
        val value: String? = null,
        val showDivider: Boolean = true,
        val navigationAction: NavigationAction? = null
    ) : BlockListItem(LIST_ITEM_WITH_ICON) {
        override val itemId: Int
            get() = (icon ?: 0) + (iconUrl?.hashCode() ?: 0) + (textResource ?: 0) + (text?.hashCode() ?: 0)
    }

    data class UserItem(
        val avatarUrl: String,
        val text: String,
        val value: String,
        val showDivider: Boolean = true
    ) : BlockListItem(USER_ITEM) {
        override val itemId: Int
            get() = avatarUrl.hashCode() + text.hashCode()
    }

    data class Information(val text: String) : BlockListItem(INFO)

    data class Text(val text: String, val links: List<Clickable>? = null) : BlockListItem(TEXT) {
        data class Clickable(
            val link: String,
            val navigationAction: NavigationAction
        )
    }

    data class Columns(val headers: List<Int>, val values: List<String>) : BlockListItem(COLUMNS)

    data class Link(
        @DrawableRes val icon: Int? = null,
        @StringRes val text: Int,
        val navigateAction: NavigationAction
    ) :
            BlockListItem(LINK)

    data class BarChartItem(val entries: List<Pair<String, Int>>) : BlockListItem(BAR_CHART)

    data class TabsItem(val tabs: List<Int>, val selectedTabPosition: Int, val onTabSelected: (position: Int) -> Unit) :
            BlockListItem(TABS)

    data class Label(@StringRes val leftLabel: Int, @StringRes val rightLabel: Int) : BlockListItem(LABEL)

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

    object Empty : BlockListItem(EMPTY)

    object Divider : BlockListItem(DIVIDER)

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
