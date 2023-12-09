package org.wordpress.android.ui.compose.components.menu.dropdown

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import me.saket.cascade.CascadeColumnScope
import org.wordpress.android.ui.compose.theme.AppColor

@Composable
fun CascadeColumnScope.SubMenu(
    element: MenuElementData.SubMenu,
    onMenuItemClick: (MenuElementData) -> Unit,
) {
    val enabledContentColor = itemContentColor()
    val disabledContentColor = if (androidx.compose.material.MaterialTheme.colors.isLight) {
        AppColor.Gray10
    } else {
        AppColor.Gray50
    }
    DropdownMenuItem(
        modifier = Modifier
            .background(itemBackgroundColor()),
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
                color = enabledContentColor,
                style = MaterialTheme.typography.labelLarge,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Medium,
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
            JetpackDropdownSubMenuHeader()
        }
    )
}
