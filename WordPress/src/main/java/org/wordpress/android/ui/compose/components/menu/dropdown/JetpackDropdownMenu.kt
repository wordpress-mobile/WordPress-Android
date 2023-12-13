package org.wordpress.android.ui.compose.components.menu.dropdown

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.saket.cascade.CascadeColumnScope
import me.saket.cascade.CascadeDropdownMenu
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppTheme

@Composable
fun JetpackDropdownMenu(
    menuItems: List<MenuElementData>,
    defaultItem: MenuElementData.Item.Single = menuItems.first() as MenuElementData.Item.Single,
) {
    Column {
        var isMenuVisible by remember { mutableStateOf(false) }
        var selectedItem by remember { mutableStateOf(defaultItem) }
        DropdownMenuButton(
            selectedItem = selectedItem,
            onClick = {
                isMenuVisible = !isMenuVisible
            }
        )
        CascadeDropdownMenu(
            modifier = Modifier.background(MenuColors.itemBackgroundColor()),
            expanded = isMenuVisible,
            onDismissRequest = { isMenuVisible = false },
        ) {
            val onMenuItemSingleClick: (MenuElementData.Item.Single) -> Unit = { clickedItem ->
                selectedItem = clickedItem
                isMenuVisible = false
            }
            menuItems.forEach { element ->
                MenuElementComposable(element = element, onMenuItemSingleClick = onMenuItemSingleClick)
            }
        }
    }
}

@Composable
private fun CascadeColumnScope.MenuElementComposable(
    element: MenuElementData,
    onMenuItemSingleClick: (MenuElementData.Item.Single) -> Unit
) {
    when (element) {
        is MenuElementData.Divider -> Divider(
            color = MenuColors.itemDividerColor(),
        )

        is MenuElementData.Item -> {
            when (element) {
                is MenuElementData.Item.Single -> Single(element, onMenuItemSingleClick)
                is MenuElementData.Item.SubMenu -> SubMenu(element, onMenuItemSingleClick)
            }
        }
    }
}

@Composable
private fun Single(
    element: MenuElementData.Item.Single,
    onMenuItemSingleClick: (MenuElementData.Item.Single) -> Unit,
) {
    val enabledContentColor = MenuColors.itemContentColor()
    val disabledContentColor = enabledContentColor.copy(alpha = ContentAlpha.disabled)
    DropdownMenuItem(
        modifier = Modifier
            .background(MenuColors.itemBackgroundColor()),
        onClick = {
            onMenuItemSingleClick(element)
            element.onClick()
        },
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

@Composable
private fun CascadeColumnScope.SubMenuHeader(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(10.5.dp),
    text: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .background(MenuColors.itemBackgroundColor())
            .fillMaxWidth()
            .clickable(enabled = hasParentMenu, role = Role.Button) {
                if (!isNavigationRunning) {
                    cascadeState.navigateBack()
                }
            }
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(
            LocalTextStyle provides MaterialTheme.typography.bodyLarge
        ) {
            if (this@SubMenuHeader.hasParentMenu) {
                Image(
                    painter = painterResource(R.drawable.ic_arrow_left_white_24dp),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(MenuColors.itemContentColor()),
                )
            }
            Box(Modifier.weight(1f)) {
                text?.invoke()
            }
        }
    }
}

@Composable
private fun CascadeColumnScope.SubMenu(
    element: MenuElementData.Item.SubMenu,
    onMenuItemSingleClick: (MenuElementData.Item.Single) -> Unit,
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
                MenuElementComposable(element = it, onMenuItemSingleClick = onMenuItemSingleClick)
            }
        },
        childrenHeader = {
            SubMenuHeader()
        }
    )
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun JetpackDropdownMenuPreview() {
    AppTheme {
        Box(
            modifier = Modifier
                .padding(start = 8.dp, top = 8.dp)
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            val menuItems = listOf(
                MenuElementData.Item.Single(
                    text = "Text only",
                    onClick = {}
                ),
                MenuElementData.Item.Single(
                    text = "Text and leading icon",
                    onClick = {},
                    leadingIcon = R.drawable.ic_jetpack_logo_white_24dp,
                ),
                MenuElementData.Divider,
                MenuElementData.Item.SubMenu(
                    text = "Text and sub-menu",
                    children = listOf(
                        MenuElementData.Item.Single(
                            text = "Text sub-menu 1",
                            onClick = {}
                        ),
                        MenuElementData.Item.Single(
                            text = "Text sub-menu 2",
                            onClick = {}
                        )
                    )
                ),
            )
            JetpackDropdownMenu(
                defaultItem = menuItems.first() as MenuElementData.Item.Single,
                menuItems = menuItems
            )
        }
    }
}
