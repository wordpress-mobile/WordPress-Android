package org.wordpress.android.ui.compose.components.menu

import androidx.annotation.DrawableRes

sealed class JetpackDropdownMenuElementData(
    open val text: String,
    @DrawableRes open val leadingIcon: Int,
    open val hasDivider: Boolean,
) {
    // Item element that closes the menu when clicked
    data class Item(
        override val text: String,
        val onClick: (Item) -> Unit,
        @DrawableRes override val leadingIcon: Int = NO_ICON,
        override val hasDivider: Boolean = false,
    ) : JetpackDropdownMenuElementData(text, leadingIcon, hasDivider)

    // Sub-menu element that opens a sub-menu when clicked
    data class SubMenu(
        override val text: String,
        val children: List<JetpackDropdownMenuElementData>,
        @DrawableRes override val leadingIcon: Int = NO_ICON,
        override val hasDivider: Boolean = false,
    ) : JetpackDropdownMenuElementData(text, leadingIcon, hasDivider)
}
