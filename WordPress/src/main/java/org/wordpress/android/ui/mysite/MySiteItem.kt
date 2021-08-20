package org.wordpress.android.ui.mysite

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import org.wordpress.android.fluxc.model.DynamicCardType
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType
import org.wordpress.android.ui.mysite.MySiteItem.Type.CATEGORY_HEADER
import org.wordpress.android.ui.mysite.MySiteItem.Type.DOMAIN_REGISTRATION_BLOCK
import org.wordpress.android.ui.mysite.MySiteItem.Type.LIST_ITEM
import org.wordpress.android.ui.mysite.MySiteItem.Type.QUICK_ACTIONS_BLOCK
import org.wordpress.android.ui.mysite.MySiteItem.Type.QUICK_START_BLOCK
import org.wordpress.android.ui.mysite.MySiteItem.Type.QUICK_START_DYNAMIC_CARD
import org.wordpress.android.ui.mysite.MySiteItem.Type.SITE_INFO_BLOCK
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString

sealed class MySiteItem(open val type: Type, open val activeQuickStartItem: Boolean = false) {
    enum class Type {
        SITE_INFO_BLOCK,
        QUICK_ACTIONS_BLOCK,
        DOMAIN_REGISTRATION_BLOCK,
        QUICK_START_BLOCK,
        QUICK_START_DYNAMIC_CARD,
        CATEGORY_HEADER,
        LIST_ITEM
    }

    data class SiteInfoBlock(
        val title: String,
        val url: String,
        val iconState: IconState,
        val showTitleFocusPoint: Boolean,
        val showIconFocusPoint: Boolean,
        val onTitleClick: ListItemInteraction? = null,
        val onIconClick: ListItemInteraction,
        val onUrlClick: ListItemInteraction,
        val onSwitchSiteClick: ListItemInteraction
    ) : MySiteItem(SITE_INFO_BLOCK, activeQuickStartItem = showTitleFocusPoint || showIconFocusPoint) {
        sealed class IconState {
            object Progress : IconState()
            data class Visible(val url: String? = null) : IconState()
        }
    }

    data class QuickActionsBlock(
        val onStatsClick: ListItemInteraction,
        val onPagesClick: ListItemInteraction,
        val onPostsClick: ListItemInteraction,
        val onMediaClick: ListItemInteraction,
        val showPages: Boolean = true,
        val showStatsFocusPoint: Boolean = false,
        val showPagesFocusPoint: Boolean = false
    ) : MySiteItem(QUICK_ACTIONS_BLOCK, activeQuickStartItem = showStatsFocusPoint || showPagesFocusPoint)

    data class DomainRegistrationBlock(val onClick: ListItemInteraction) : MySiteItem(DOMAIN_REGISTRATION_BLOCK)

    data class QuickStartBlock(
        @DrawableRes val icon: Int,
        val title: UiString,
        val onRemoveMenuItemClick: ListItemInteraction,
        val taskTypeItems: List<QuickStartTaskTypeItem>
    ) : MySiteItem(QUICK_START_BLOCK) {
        data class QuickStartTaskTypeItem(
            val quickStartTaskType: QuickStartTaskType,
            val title: UiString,
            val titleEnabled: Boolean,
            val subtitle: UiString,
            val strikeThroughTitle: Boolean,
            @ColorRes val progressColor: Int,
            val progress: Int,
            val onClick: ListItemInteraction
        )
    }

    sealed class DynamicCard(
        override val type: Type,
        override val activeQuickStartItem: Boolean = false,
        open val dynamicCardType: DynamicCardType,
        open val onMoreClick: ListItemInteraction
    ) : MySiteItem(
            type,
            activeQuickStartItem
    ) {
        data class QuickStartCard(
            val id: DynamicCardType,
            val title: UiString,
            val taskCards: List<QuickStartTaskCard>,
            @ColorRes val accentColor: Int,
            val progress: Int,
            override val onMoreClick: ListItemInteraction
        ) : DynamicCard(QUICK_START_DYNAMIC_CARD, dynamicCardType = id, onMoreClick = onMoreClick) {
            data class QuickStartTaskCard(
                val quickStartTask: QuickStartTask,
                val title: UiString,
                val description: UiString,
                @DrawableRes val illustration: Int,
                @ColorRes val accentColor: Int,
                val done: Boolean = false,
                val onClick: ListItemInteraction
            )
        }
    }

    data class CategoryHeader(val title: UiString) : MySiteItem(CATEGORY_HEADER)

    data class ListItem(
        @DrawableRes val primaryIcon: Int,
        val primaryText: UiString,
        @DrawableRes val secondaryIcon: Int? = null,
        val secondaryText: UiString? = null,
        val showFocusPoint: Boolean = false,
        val onClick: ListItemInteraction
    ) : MySiteItem(LIST_ITEM, activeQuickStartItem = showFocusPoint)
}
