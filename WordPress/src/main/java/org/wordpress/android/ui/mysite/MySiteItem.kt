package org.wordpress.android.ui.mysite

import org.wordpress.android.ui.mysite.MySiteItem.Type.QUICK_ACTIONS_BLOCK
import org.wordpress.android.ui.mysite.MySiteItem.Type.SITE_INFO_BLOCK
import org.wordpress.android.ui.utils.ListItemInteraction

sealed class MySiteItem(val type: Type) {
    enum class Type {
        SITE_INFO_BLOCK,
        QUICK_ACTIONS_BLOCK,
        HEADER,
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
}
