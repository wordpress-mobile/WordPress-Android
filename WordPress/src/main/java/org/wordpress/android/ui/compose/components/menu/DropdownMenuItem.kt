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
        if (item.leftIcon != NO_ICON) {
            Icon(
                modifier = Modifier.align(Alignment.CenterVertically),
                painter = painterResource(id = item.leftIcon),
                contentDescription = null,
            )
            Spacer(Modifier.width(iconTextMargin))
        }
        Text(
            text = item.text,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            fontSize = FontSize.Large.value,
        )
        if (item.rightIcon != NO_ICON) {
            Icon(
                modifier = Modifier.align(Alignment.CenterVertically),
                painter = painterResource(id = item.rightIcon),
                contentDescription = null,
            )
            Spacer(Modifier.width(iconTextMargin))
        }
    }
}
@Suppress("LongParameterList")
sealed class DropdownMenuItemData(
    open val text: String,
    open val isDefault: Boolean,
    open val hasDivider: Boolean,
    open val id: String,
    open val onClick: (String) -> Unit,
    @DrawableRes open val leftIcon: Int,
    @DrawableRes open val rightIcon: Int,
) {
    /**
     * @param onClick callback that returns the defined id
     */
    data class Item(
        override val id: String,
        override val text: String,
        override val isDefault: Boolean = false,
        override val hasDivider: Boolean = false,
        override val onClick: (String) -> Unit,
        @DrawableRes override val leftIcon: Int = NO_ICON,
        @DrawableRes override val rightIcon: Int = NO_ICON,
    ) : DropdownMenuItemData(
        text = text,
        isDefault = isDefault,
        hasDivider = hasDivider,
        id = id,
        onClick = onClick,
        leftIcon = leftIcon,
        rightIcon = rightIcon,
    )

    /**
     * @param onClick callback that returns the defined id
     */
    data class SubMenu(
        override val id: String,
        override val text: String,
        override val isDefault: Boolean = false,
        override val hasDivider: Boolean = false,
        override val onClick: (String) -> Unit,
        @DrawableRes override val leftIcon: Int = NO_ICON,
        @DrawableRes override val rightIcon: Int = NO_ICON,
        val items: List<DropdownMenuItemData>,
    ) : DropdownMenuItemData(
        text = text,
        isDefault = isDefault,
        hasDivider = hasDivider,
        id = id,
        onClick = onClick,
        leftIcon = leftIcon,
        rightIcon = rightIcon,
    )
}

internal const val NO_ICON = -1
