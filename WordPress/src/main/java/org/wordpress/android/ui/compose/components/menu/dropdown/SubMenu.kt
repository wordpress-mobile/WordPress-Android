package org.wordpress.android.ui.compose.components.menu.dropdown

import androidx.compose.foundation.background
import androidx.compose.material.ContentAlpha
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import me.saket.cascade.CascadeColumnScope

@Composable
fun CascadeColumnScope.SubMenu(
    element: MenuElementData.SubMenu,
    onMenuItemClick: (MenuElementData) -> Unit,
) {
    val enabledContentColor = MenuColors.itemContentColor()
    val disabledContentColor = enabledContentColor.copy(alpha = ContentAlpha.disabled)
    DropdownMenuItem(
        modifier = Modifier
            .background(MenuColors.itemBackgroundColor()),
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
                text = element.text,
                style = MaterialTheme.typography.bodyLarge,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
            )
        },
        children = {
            element.children.forEach {
                when (it) {
                    is MenuElementData.SubMenu -> SubMenu(it, onMenuItemClick)
                    is MenuElementData.Item -> Item(it, onMenuItemClick)
                }
            }
        },
        childrenHeader = {
            SubMenuHeader()
        }
    )
}
