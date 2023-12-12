package org.wordpress.android.ui.compose.components.menu.dropdown

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.saket.cascade.CascadeDropdownMenu
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppTheme

@Composable
fun JetpackDropdownMenu(menuItems: List<MenuElementData>, defaultItem: MenuElementData) {
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
            val onMenuItemClick: (MenuElementData) -> Unit = { clickedItem ->
                selectedItem = clickedItem
                isMenuVisible = false
            }
            menuItems.forEach { element ->
                when (element) {
                    is MenuElementData.SubMenu -> SubMenu(element, onMenuItemClick)
                    is MenuElementData.Item -> Item(element, onMenuItemClick)
                }
                if (element.hasDivider) {
                    Divider(
                        color = MenuColors.itemDividerColor(),
                    )
                }
            }
        }
    }
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
                MenuElementData.Item(
                    text = "Text only",
                    onClick = {}
                ),
                MenuElementData.Item(
                    text = "Text and leading icon",
                    onClick = {},
                    leadingIcon = R.drawable.ic_jetpack_logo_white_24dp,
                    hasDivider = true,
                ),
                MenuElementData.SubMenu(
                    text = "Text and sub-menu",
                    children = listOf(
                        MenuElementData.Item(
                            text = "Text sub-menu 1",
                            onClick = {}
                        ),
                        MenuElementData.Item(
                            text = "Text sub-menu 2",
                            onClick = {}
                        )
                    )
                ),
            )
            JetpackDropdownMenu(
                defaultItem = menuItems.first(),
                menuItems = menuItems
            )
        }
    }
}
