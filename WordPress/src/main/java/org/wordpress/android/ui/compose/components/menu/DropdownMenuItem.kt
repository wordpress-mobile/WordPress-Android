package org.wordpress.android.ui.compose.components.menu

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable

@Composable
fun DropdownMenuItem(dropdownMenuItemData: DropdownMenuItemData) {
}

sealed class DropdownMenuItemData {
    data class Text(@StringRes val text: Int) : DropdownMenuItemData()

    data class TextAndIcon(@StringRes val text: Int, @DrawableRes val icon: Int) : DropdownMenuItemData()
}
