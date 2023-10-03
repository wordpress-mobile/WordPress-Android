package org.wordpress.android.ui.mysite.menu

import androidx.annotation.DrawableRes
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString

data class MenuViewState(
    val items : List<MenuItemState>
)

sealed class MenuItemState {
    data class MenuHeaderItem(val title: UiString) : MenuItemState()
    object MenuEmptyHeaderItem : MenuItemState()
    data class MenuListItem(
        @DrawableRes val primaryIcon: Int,
        val primaryText: UiString,
        @DrawableRes val secondaryIcon: Int? = null,
        val secondaryText: UiString? = null,
        val showFocusPoint: Boolean = false,
        val onClick: ListItemInteraction,
        val disablePrimaryIconTint: Boolean = false,
        val listItemAction: ListItemAction
    ) : MenuItemState()
}

fun MySiteCardAndItem.Item.toMenuItemState(): MenuItemState {
     return when (this) {
        is MySiteCardAndItem.Item.CategoryHeaderItem -> MenuItemState.MenuHeaderItem(title)
        is MySiteCardAndItem.Item.CategoryEmptyHeaderItem -> MenuItemState.MenuEmptyHeaderItem
        is MySiteCardAndItem.Item.ListItem -> MenuItemState.MenuListItem(
            primaryIcon = primaryIcon,
            primaryText = primaryText,
            secondaryIcon = secondaryIcon,
            secondaryText = secondaryText,
            showFocusPoint = showFocusPoint,
            onClick = onClick,
            disablePrimaryIconTint = disablePrimaryIconTint,
            listItemAction = listItemAction
        )
        else -> {
            throw IllegalArgumentException("Unsupported MySiteCardAndItem.Item type: $this")
        }
    }
}
