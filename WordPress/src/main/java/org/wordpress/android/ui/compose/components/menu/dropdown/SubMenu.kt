package org.wordpress.android.ui.compose.components.menu.dropdown

import androidx.compose.foundation.background
import androidx.compose.material.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import me.saket.cascade.CascadeColumnScope
import org.wordpress.android.ui.compose.theme.AppColor

@Composable
fun CascadeColumnScope.SubMenu(
    element: MenuElementData.SubMenu,
    onMenuItemClick: (MenuElementData) -> Unit,
) {
    val enabledContentColor = MaterialTheme.colorScheme.onSurface
    val disabledContentColor = if (androidx.compose.material.MaterialTheme.colors.isLight) {
        AppColor.Gray10
    } else {
        AppColor.Gray50
    }
    DropdownMenuItem(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background),
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
        leadingIcon = {
            if (element.leadingIcon != NO_ICON) {
                Icon(
                    painter = painterResource(id = element.leadingIcon),
                    contentDescription = null,
                )
            }
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
