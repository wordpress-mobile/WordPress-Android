package org.wordpress.android.ui.mysite

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import org.wordpress.android.fluxc.model.DynamicCardType
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Type.CATEGORY_HEADER_ITEM
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Type.DOMAIN_REGISTRATION_CARD
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Type.LIST_ITEM
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Type.QUICK_ACTIONS_CARD
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Type.QUICK_START_CARD
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Type.QUICK_START_DYNAMIC_CARD
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Type.SITE_INFO_CARD
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString

sealed class MySiteCardAndItem(open val type: Type, open val activeQuickStartItem: Boolean = false) {
    enum class Type {
        SITE_INFO_CARD,
        QUICK_ACTIONS_CARD,
        DOMAIN_REGISTRATION_CARD,
        QUICK_START_CARD,
        QUICK_START_DYNAMIC_CARD,
        CATEGORY_HEADER_ITEM,
        LIST_ITEM
    }

    sealed class Card(
        override val type: Type,
        override val activeQuickStartItem: Boolean = false
    ) : MySiteCardAndItem(type, activeQuickStartItem) {
        data class SiteInfoCard(
            val title: String,
            val url: String,
            val iconState: IconState,
            val showTitleFocusPoint: Boolean,
            val showIconFocusPoint: Boolean,
            val onTitleClick: ListItemInteraction? = null,
            val onIconClick: ListItemInteraction,
            val onUrlClick: ListItemInteraction,
            val onSwitchSiteClick: ListItemInteraction
        ) : Card(SITE_INFO_CARD, activeQuickStartItem = showTitleFocusPoint || showIconFocusPoint) {
            sealed class IconState {
                object Progress : IconState()
                data class Visible(val url: String? = null) : IconState()
            }
        }

        data class QuickActionsCard(
            val title: UiString,
            val onStatsClick: ListItemInteraction,
            val onPagesClick: ListItemInteraction,
            val onPostsClick: ListItemInteraction,
            val onMediaClick: ListItemInteraction,
            val showPages: Boolean = true,
            val showStatsFocusPoint: Boolean = false,
            val showPagesFocusPoint: Boolean = false
        ) : Card(QUICK_ACTIONS_CARD, activeQuickStartItem = showStatsFocusPoint || showPagesFocusPoint)

        data class DomainRegistrationCard(val onClick: ListItemInteraction) : Card(DOMAIN_REGISTRATION_CARD)

        data class QuickStartCard(
            val title: UiString,
            val moreMenuVisible: Boolean = true,
            val onRemoveMenuItemClick: ListItemInteraction,
            val taskTypeItems: List<QuickStartTaskTypeItem>
        ) : Card(QUICK_START_CARD) {
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
    }

    sealed class DynamicCard(
        override val type: Type,
        override val activeQuickStartItem: Boolean = false,
        open val dynamicCardType: DynamicCardType,
        open val onMoreClick: ListItemInteraction
    ) : MySiteCardAndItem(
            type,
            activeQuickStartItem
    ) {
        data class QuickStartDynamicCard(
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

    sealed class Item(
        override val type: Type,
        override val activeQuickStartItem: Boolean = false
    ) : MySiteCardAndItem(type, activeQuickStartItem) {
        data class CategoryHeaderItem(val title: UiString) : Item(CATEGORY_HEADER_ITEM)

        data class ListItem(
            @DrawableRes val primaryIcon: Int,
            val primaryText: UiString,
            @DrawableRes val secondaryIcon: Int? = null,
            val secondaryText: UiString? = null,
            val showFocusPoint: Boolean = false,
            val onClick: ListItemInteraction
        ) : Item(LIST_ITEM, activeQuickStartItem = showFocusPoint)
    }
}
