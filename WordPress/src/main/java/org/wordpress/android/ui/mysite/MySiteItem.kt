package org.wordpress.android.ui.mysite

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask
import org.wordpress.android.ui.mysite.MySiteItem.Type.CATEGORY_HEADER
import org.wordpress.android.ui.mysite.MySiteItem.Type.DOMAIN_REGISTRATION_BLOCK
import org.wordpress.android.ui.mysite.MySiteItem.Type.LIST_ITEM
import org.wordpress.android.ui.mysite.MySiteItem.Type.QUICK_ACTIONS_BLOCK
import org.wordpress.android.ui.mysite.MySiteItem.Type.QUICK_START_CARD
import org.wordpress.android.ui.mysite.MySiteItem.Type.SITE_INFO_BLOCK
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes

sealed class MySiteItem(val type: Type) {
    enum class Type {
        SITE_INFO_BLOCK,
        QUICK_ACTIONS_BLOCK,
        DOMAIN_REGISTRATION_BLOCK,
        QUICK_START_CARD,
        CATEGORY_HEADER,
        LIST_ITEM
    }

    data class SiteInfoBlock(
        val title: String,
        val url: String,
        val iconState: IconState,
        val onTitleClick: ListItemInteraction? = null,
        val onIconClick: ListItemInteraction,
        val onUrlClick: ListItemInteraction,
        val onSwitchSiteClick: ListItemInteraction
    ) : MySiteItem(SITE_INFO_BLOCK) {
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
        val showPages: Boolean = true
    ) : MySiteItem(QUICK_ACTIONS_BLOCK)

    data class DomainRegistrationBlock(val onClick: ListItemInteraction) : MySiteItem(DOMAIN_REGISTRATION_BLOCK)

    data class QuickStartCard(
        val id: String,
        val title: UiString,
        val taskCards: List<QuickStartTaskCard>,
        @ColorRes val accentColor: Int,
        val progress: Int,
        val onMoreClick: ListItemInteraction? = null
    ) : MySiteItem(QUICK_START_CARD) {
        data class QuickStartTaskCard(
            val quickStartTask
            : QuickStartTask,
            val title: UiString,
            val description: UiString,
            @DrawableRes val illustration: Int,
            @ColorRes val accentColor: Int,
            val done: Boolean = false,
            val onClick: ListItemInteraction
        )
    }

    data class CategoryHeader(val title: UiString) : MySiteItem(CATEGORY_HEADER)

    data class ListItem(
        @DrawableRes val primaryIcon: Int,
        val primaryText: UiString,
        @DrawableRes val secondaryIcon: Int? = null,
        val secondaryText: UiString? = null,
        val onClick: ListItemInteraction
    ) : MySiteItem(LIST_ITEM)
}
