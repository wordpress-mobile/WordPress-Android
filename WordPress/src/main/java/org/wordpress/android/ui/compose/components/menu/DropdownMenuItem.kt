package org.wordpress.android.ui.compose.components.menu

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable

@Composable
fun DropdownMenuItem(itemType: ItemType) {
}

sealed class ItemType {
    data class Text(@StringRes val text: Int) : ItemType()

    data class TextAndIcon(@StringRes val text: Int, @DrawableRes val icon: Int) : ItemType()
}
