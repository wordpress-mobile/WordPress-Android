package org.wordpress.android.ui.compose.components.menu

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable

@Composable
fun DropdownMenuItem(itemData: DropdownMenuItemData) {
}

sealed class DropdownMenuItemData {
    /**
     * @param onClick callback that returns the defined id
     */
    data class Text(
        val id: String,
        val text: String,
        val onClick: (String) -> Unit,
        val isDefault: Boolean = false,
    ) : DropdownMenuItemData()

    /**
     * @param onClick callback that returns the defined id
     */
    data class TextAndIcon(
        val id: String,
        val text: String,
        @DrawableRes val icon: Int,
        val onClick: (String) -> Unit,
        val isDefault: Boolean = false,
    ) : DropdownMenuItemData()

    data class SubMenu(
        val text: String,
        val items: List<Item>,
    ) : DropdownMenuItemData() {
        /**
         * @param onClick callback that returns the defined id
         */
        data class Item(
            val id: String,
            val text: String,
            val onClick: (String) -> Unit,
            val isDefault: Boolean = false,
        )
    }
}
