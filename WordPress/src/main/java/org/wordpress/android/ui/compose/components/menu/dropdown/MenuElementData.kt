package org.wordpress.android.ui.compose.components.menu.dropdown

import androidx.annotation.DrawableRes

sealed interface MenuElementData {
    data object Divider : MenuElementData

    sealed class Item(
        open val text: String,
        @DrawableRes open val leadingIcon: Int,
    ) : MenuElementData {
        // Item element that closes the menu when clicked
        data class Single(
            override val text: String,
            val onClick: () -> Unit,
            @DrawableRes override val leadingIcon: Int = NO_ICON,
        ) : Item(text, leadingIcon)

        // Sub-menu element that opens a sub-menu when clicked
        data class SubMenu(
            override val text: String,
            val children: List<MenuElementData>,
            @DrawableRes override val leadingIcon: Int = NO_ICON,
        ) : Item(text, leadingIcon)
    }
}

internal const val NO_ICON = -1
