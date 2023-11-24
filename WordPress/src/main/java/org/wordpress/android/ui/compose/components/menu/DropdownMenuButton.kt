package org.wordpress.android.ui.compose.components.menu

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.menu.DropdownMenuItemData.Item
import org.wordpress.android.ui.compose.components.menu.DropdownMenuItemData.SubMenu
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.compose.unit.FontSize
import org.wordpress.android.ui.compose.unit.Margin

@Composable
fun DropdownMenuButton(
    selectedItem: DropdownMenuItemData,
    onClick: () -> Unit,
) {
    require(selectedItem !is SubMenu) { "DropdownMenuButton selected item cannot be a SubMenu" }
    Button(
        modifier = Modifier.defaultMinSize(minHeight = 40.dp),
        onClick = onClick,
        elevation = ButtonDefaults.elevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
        ),
        colors = ButtonDefaults.buttonColors(
            contentColor = MaterialTheme.colors.onPrimary,
            backgroundColor = MaterialTheme.colors.onSurface,
        ),
        shape = RoundedCornerShape(50),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Margin.Medium.value),
        ) {
            if (selectedItem.leftIcon != NO_ICON) {
                Icon(
                    modifier = Modifier.align(Alignment.CenterVertically),
                    painter = painterResource(id = selectedItem.leftIcon),
                    contentDescription = null,
                )
            }
            Text(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .weight(
                        weight = 1f,
                        fill = false,
                    ),
                style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
                text = selectedItem.text,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
            Icon(
                modifier = Modifier.align(Alignment.CenterVertically),
                painter = painterResource(id = R.drawable.ic_chevron_down_white_16dp),
                contentDescription = null,
                tint = MaterialTheme.colors.onPrimary,
            )
        }
    }
}

@Preview
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DropdownMenuButtonPreview() {
    AppTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DropdownMenuButton(
                selectedItem = Item(
                    id = "text1",
                    text = "Text only",
                    onClick = {},
                ),
                onClick = {}
            )
            DropdownMenuButton(
                selectedItem = Item(
                    id = "textAndIcon1",
                    text = "Text and Icon",
                    leftIcon = R.drawable.ic_jetpack_logo_white_24dp,
                    onClick = {},
                ),
                onClick = {},
            )
            DropdownMenuButton(
                selectedItem = Item(
                    id = "textAndIcon1",
                    text = "Text type with a really long text as the button label",
                    onClick = {},
                ),
                onClick = {},
            )
            DropdownMenuButton(
                selectedItem = Item(
                    id = "textAndIcon1",
                    text = "Text type with a really long text as the button label",
                    leftIcon = R.drawable.ic_jetpack_logo_white_24dp,
                    onClick = {},
                ),
                onClick = {},
            )
        }
    }
}
