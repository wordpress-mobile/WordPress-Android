package org.wordpress.android.ui.compose.components.menu.dropdown

import android.content.res.Configuration
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.menu.dropdown.MenuElementData.Item
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.compose.unit.Margin
import androidx.compose.material3.MaterialTheme as Material3Theme

@Composable
fun DropdownMenuButton(
    selectedItem: MenuElementData.Item,
    onClick: () -> Unit,
) {
    Button(
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
            modifier = Modifier.animateContentSize(),
        ) {
            if (selectedItem is Item.Single && selectedItem.leadingIcon != NO_ICON) {
                Icon(
                    modifier = Modifier.align(Alignment.CenterVertically),
                    painter = painterResource(id = selectedItem.leadingIcon),
                    contentDescription = null,
                )
                Spacer(Modifier.width(Margin.Small.value))
            }
            Text(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .weight(
                        weight = 1f,
                        fill = false,
                    ),
                style = Material3Theme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                text = selectedItem.text,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
            Spacer(Modifier.width(Margin.Small.value))
            Icon(
                modifier = Modifier.align(Alignment.CenterVertically),
                painter = painterResource(id = R.drawable.ic_small_chevron_down_white_16dp),
                contentDescription = null,
                tint = MaterialTheme.colors.onPrimary,
            )
        }
    }
}

@Preview
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun JetpackDropdownMenuButtonPreview() {
    AppTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DropdownMenuButton(
                selectedItem = Item.Single(
                    text = "Text only",
                    onClick = {},
                ),
                onClick = {}
            )
            DropdownMenuButton(
                selectedItem = Item.Single(
                    text = "Text and Icon",
                    onClick = {},
                    leadingIcon = R.drawable.ic_jetpack_logo_white_24dp,
                ),
                onClick = {},
            )
            DropdownMenuButton(
                selectedItem = Item.Single(
                    text = "Text type with a really long text as the button label",
                    onClick = {},
                ),
                onClick = {},
            )
            DropdownMenuButton(
                selectedItem = Item.Single(
                    text = "Text type with a really long text as the button label",
                    onClick = {},
                    leadingIcon = R.drawable.ic_jetpack_logo_white_24dp,
                ),
                onClick = {},
            )
        }
    }
}