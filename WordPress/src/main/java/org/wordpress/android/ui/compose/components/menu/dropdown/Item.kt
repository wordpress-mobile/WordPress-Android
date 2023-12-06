package org.wordpress.android.ui.compose.components.menu.dropdown

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppColor
import org.wordpress.android.ui.compose.theme.AppTheme

@Composable
fun Item(
    element: MenuElementData.Item,
    onMenuItemClick: (MenuElementData) -> Unit,
) {
    val enabledContentColor = MaterialTheme.colorScheme.onSurface
    val disabledContentColor = if (androidx.compose.material.MaterialTheme.colors.isLight) {
        AppColor.Gray10
    } else {
        AppColor.Gray50
    }
    androidx.compose.material3.DropdownMenuItem(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background),
        onClick = { onMenuItemClick(element) },
        colors = MenuDefaults.itemColors(
            textColor = enabledContentColor,
            leadingIconColor = enabledContentColor,
            trailingIconColor = enabledContentColor,
            disabledTextColor = disabledContentColor,
            disabledLeadingIconColor = disabledContentColor,
            disabledTrailingIconColor = disabledContentColor,
        ),
        text = {
            Text(
                style = MaterialTheme.typography.bodyLarge,
                text = element.text,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
        },
        leadingIcon = if (element.leadingIcon != NO_ICON) {
            {
                Icon(
                    painter = painterResource(id = element.leadingIcon),
                    contentDescription = null,
                )
            }
        } else null,
    )
}

@Preview
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun JetpackDropdownMenuItemPreview() {
    AppTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Item(MenuElementData.Item(
                text = "Text only",
                onClick = {}
            ), {})
            Item(MenuElementData.Item(
                text = "Text and image",
                leadingIcon = R.drawable.ic_jetpack_logo_white_24dp,
                onClick = {}
            ), {})
            Item(MenuElementData.Item(
                text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut" +
                        "labore et dolore magna aliqua.",
                onClick = {}
            ), {})
            Item(MenuElementData.Item(
                text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut" +
                        "labore et dolore magna aliqua.",
                leadingIcon = R.drawable.ic_jetpack_logo_white_24dp,
                onClick = {}
            ), {})
        }
    }
}

