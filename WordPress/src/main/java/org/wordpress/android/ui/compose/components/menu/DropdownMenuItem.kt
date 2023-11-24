package org.wordpress.android.ui.compose.components.menu

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.wordpress.android.ui.compose.unit.FontSize
import org.wordpress.android.ui.compose.unit.Margin

@Composable
fun DropdownMenuItem(item: DropdownMenuItemData) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable { item.onClick(item.id) }
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(all = Margin.MediumLarge.value),
    ) {
        val iconTextMargin = Margin.Medium.value
        if (item is DropdownMenuItemData.TextAndIcon) {
            Icon(
                modifier = Modifier.align(Alignment.CenterVertically),
                painter = painterResource(id = item.iconRes),
                contentDescription = null,
            )
            Spacer(Modifier.width(iconTextMargin))
        } else {
            // item is Text
            val defaultIconSize = 24.dp
            val textOnlyMargin = iconTextMargin + defaultIconSize
            Spacer(Modifier.width(textOnlyMargin))
        }
        Text(
            text = item.text,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            fontSize = FontSize.Large.value,
        )
    }
}

sealed class DropdownMenuItemData(
    open val text: String,
    open val isDefault: Boolean,
    open val hasDivider: Boolean,
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
        override val hasDivider: Boolean = false,
        override val onClick: (String) -> Unit,
    ) : DropdownMenuItemData(
        text = text,
        isDefault = isDefault,
        hasDivider = hasDivider,
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
        override val hasDivider: Boolean = false,
        override val onClick: (String) -> Unit,
    ) : DropdownMenuItemData(
        text = text,
        isDefault = isDefault,
        hasDivider = hasDivider,
        id = id,
        onClick = onClick,
    )

    data class SubMenu(
        override val id: String,
        override val text: String,
        override val isDefault: Boolean = false,
        override val hasDivider: Boolean = false,
        val items: List<DropdownMenuItemData>,
        override val onClick: (String) -> Unit,
    ) : DropdownMenuItemData(
        text = text,
        isDefault = isDefault,
        hasDivider = hasDivider,
        id = id,
        onClick = onClick,
    )
}
