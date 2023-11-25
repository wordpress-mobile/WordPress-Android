package org.wordpress.android.ui.compose.components.menu

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.compose.unit.Margin

@Composable
fun DropdownMenuItem(
    item: DropdownMenuItemData,
    modifier: Modifier = Modifier,
    onItemClick: (String) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clickable {
                onItemClick(item.id)
            }
            .padding(
                start = Margin.MediumLarge.value,
                top = Margin.ExtraLarge.value,
                end = Margin.MediumLarge.value,
                bottom = Margin.ExtraLarge.value
            ),
    ) {
        if (item.leftIcon != NO_ICON) {
            Icon(
                painter = painterResource(id = item.leftIcon),
                contentDescription = null,
            )
            Spacer(Modifier.width(Margin.Medium.value))
        }
        Text(
            modifier = Modifier
                .weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            text = item.text,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
        )
        if (item.rightIcon != NO_ICON) {
            Spacer(Modifier.width(Margin.Medium.value))
            Icon(
                modifier = Modifier.align(Alignment.CenterVertically),
                painter = painterResource(id = item.rightIcon),
                contentDescription = null,
            )
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
        val items: List<Item>,
    ) : DropdownMenuItemData(
        text = text,
        isDefault = isDefault,
        hasDivider = hasDivider,
        id = id,
        onClick = onClick,
        leftIcon = NO_ICON,
        rightIcon = R.drawable.ic_arrow_right_black_24dp,
    )
}

internal const val NO_ICON = -1

@Preview
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DropdownMenuButtonPreview() {
    AppTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DropdownMenuItem(
                item = DropdownMenuItemData.Item(
                    id = "text1",
                    text = "Text only",
                    onClick = {},
                ),
                onItemClick = {}
            )
            DropdownMenuItem(
                item = DropdownMenuItemData.Item(
                    id = "textAndIcon1",
                    text = "Text and left icon",
                    leftIcon = R.drawable.ic_jetpack_logo_white_24dp,
                    onClick = {},
                ),
                onItemClick = {}
            )
            DropdownMenuItem(
                item = DropdownMenuItemData.Item(
                    id = "textAndIcon1",
                    text = "Text and right icon",
                    rightIcon = R.drawable.ic_jetpack_logo_white_24dp,
                    onClick = {},
                ),
                onItemClick = {}
            )
            DropdownMenuItem(
                item = DropdownMenuItemData.Item(
                    id = "textAndIcon1",
                    text = "Text, left and, right icon. The text is really long.",
                    leftIcon = R.drawable.ic_jetpack_logo_white_24dp,
                    rightIcon = R.drawable.ic_jetpack_logo_white_24dp,
                    onClick = {},
                ),
                onItemClick = {}
            )
            DropdownMenuItem(
                item = DropdownMenuItemData.Item(
                    id = "textAndIcon1",
                    text = "Text type with a really long text as the button label",
                    onClick = {},
                ),
                onItemClick = {}
            )
        }
    }
}
