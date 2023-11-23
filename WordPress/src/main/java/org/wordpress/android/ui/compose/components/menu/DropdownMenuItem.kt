package org.wordpress.android.ui.compose.components.menu

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable

@Composable
fun DropdownMenuItem(itemData: DropdownMenuItemData) {
}

sealed class DropdownMenuItemData(
    val isDefault: Boolean = false,
    ) {
    /**
     * @param onClick callback that returns the defined id
     */
    data class Text(
        val id: String,
        val text: String,
        val onClick: (String) -> Unit,
    ) : DropdownMenuItemData()

    /**
     * @param onClick callback that returns the defined id
     */
    data class TextAndIcon(
        val id: String,
        val text: String,
        @DrawableRes val icon: Int,
        val onClick: (String) -> Unit,
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
            val isCurrentlySelected: Boolean = false,
        )
    }
}
