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
    open val id: String,
    open val onClick: (String) -> Unit,
) {
    /**
     * @param onClick callback that returns the defined id
     */
    data class Text(
        override val id: String,
        override val text: String,
        override val isDefault: Boolean = false,
        override val onClick: (String) -> Unit,
    ) : DropdownMenuItemData(
        text = text,
        isDefault = isDefault,
        id = id,
        onClick = onClick,
    )

    /**
     * @param onClick callback that returns the defined id
     */
    data class TextAndIcon(
        override val id: String,
        override val text: String,
        @DrawableRes val iconRes: Int,
        override val isDefault: Boolean = false,
        override val onClick: (String) -> Unit,
    ) : DropdownMenuItemData(
        text = text,
        isDefault = isDefault,
        id = id,
        onClick = onClick,
    )

    data class SubMenu(
        override val id: String,
        override val text: String,
        override val isDefault: Boolean = false,
        val items: List<DropdownMenuItemData>,
        override val onClick: (String) -> Unit,
    ) : DropdownMenuItemData(
        text = text,
        isDefault = isDefault,
        id = id,
        onClick = onClick,
    )
}
