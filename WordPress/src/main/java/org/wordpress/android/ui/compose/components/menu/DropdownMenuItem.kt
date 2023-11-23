package org.wordpress.android.ui.compose.components.menu

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable

@Composable
fun DropdownMenuItem(itemData: DropdownMenuItemData) {
    itemData.text
}

sealed class DropdownMenuItemData(
    open val text: String,
    open val isDefault: Boolean,
) {
    /**
     * @param onClick callback that returns the defined id
     */
    data class Text(
        val id: String,
        override val text: String,
        override val isDefault: Boolean = false,
        val onClick: (String) -> Unit,
    ) : DropdownMenuItemData(
        text = text,
        isDefault = isDefault,
    )

    /**
     * @param onClick callback that returns the defined id
     */
    data class TextAndIcon(
        val id: String,
        override val text: String,
        @DrawableRes val iconRes: Int,
        override val isDefault: Boolean = false,
        val onClick: (String) -> Unit,
    ) : DropdownMenuItemData(
        text = text,
        isDefault = isDefault,
    )

    data class SubMenu(
        override val text: String,
        override val isDefault: Boolean = false,
        val items: List<DropdownMenuItemData>,
    ) : DropdownMenuItemData(
        text = text,
        isDefault = isDefault,
    )
}
