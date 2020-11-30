package org.wordpress.android.ui.mysite

import androidx.annotation.DrawableRes
import org.wordpress.android.ui.mysite.MySiteItem.Type.CATEGORY_HEADER
import org.wordpress.android.ui.mysite.MySiteItem.Type.LIST_ITEM
import org.wordpress.android.ui.mysite.MySiteItem.Type.SITE_INFO_BLOCK
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString

sealed class MySiteItem(val type: Type) {
    enum class Type {
        SITE_INFO_BLOCK, CATEGORY_HEADER, LIST_ITEM
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

    data class CategoryHeader(val title: UiString) : MySiteItem(CATEGORY_HEADER)

    data class ListItem(
        @DrawableRes val primaryIcon: Int,
        val primaryText: UiString,
        @DrawableRes val secondaryIcon: Int? = null,
        val secondaryText: UiString? = null,
        val isExternalLink: Boolean = false,
        val onClick: ListItemInteraction
    ) : MySiteItem(LIST_ITEM)
}
