package org.wordpress.android.ui.compose.components.menu

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.menu.DropdownMenuItemData.Item
import org.wordpress.android.ui.compose.components.menu.DropdownMenuItemData.SubMenu
import org.wordpress.android.ui.compose.theme.AppThemeEditor

/**
 * DropdownMenu component. Consists of a DropdownMenuButton that opens a DropdownMenuItemsContainer when tapped.
 * @param items the dropdown menu items to be shown. There should be only one default item.
 */
@Composable
fun DropdownMenu(items: List<DropdownMenuItemData>) {
    require(items.hasSingleDefaultItem()) { "DropdownMenu must have one default item." }
    // currentMenuItems can either be the default menu received as a parameter OR the sub-menu items if a
    // sub-menu is selected.
    var currentMenuItems by remember { mutableStateOf(items) }
    var selectedItem by remember { mutableStateOf(items.defaultItem()) }
    var isMenuOpen by remember { mutableStateOf(false) }
    var openSubMenuId by remember { mutableStateOf("") }
    Column {
        DropdownMenuButton(
            selectedItem = selectedItem,
            onClick = {
                isMenuOpen = !isMenuOpen
                openSubMenuId = ""
            }
        )
        // Sub-menu is not open. Show the default menu.
        AnimatedVisibility(
            visible = isMenuOpen,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            DropdownMenuItemList(
                items = currentMenuItems,
                onItemClick = { id ->
                    val clickedItem = if (openSubMenuId.isNotEmpty()) {
                        // Clicked item is inside a sub-menu
                        (items.firstOrNull { it.id == openSubMenuId } as SubMenu).items
                            .find { it.id == id }
                    } else {
                        // Clicked item is not inside a sub-menu
                        items.firstOrNull { it.id == id }
                    }
                    clickedItem?.let {
                        it.onClick(it.id)
                        if (it is SubMenu) {
                            // If the clicked item is a SubMenu we should keep its id to update the UI
                            openSubMenuId = id
                        } else {
                            // If the clicked item is not a SubMenu, we should close the menu and update selectedItem
                            // The open sub-menu ID should also be changed to the default value (empty string) since the
                            // menu will be closed.
                            openSubMenuId = ""
                            selectedItem = it
                            isMenuOpen = false
                        }
                    }
                },
            )
        }
        currentMenuItems = if (openSubMenuId.isNotEmpty()) {
            (items.find { it.id == openSubMenuId } as SubMenu).items
        } else {
            items
        }
    }
}

private fun List<DropdownMenuItemData>.hasSingleDefaultItem() = filter { it.isDefault }.size == 1

private fun List<DropdownMenuItemData>.defaultItem() =
    find { it.isDefault } ?: throw IllegalArgumentException("Default item must not be null.")

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun EditPostSettingsJetpackSocialSharesContainerPreview() {
    AppThemeEditor {
        Box(
            modifier = Modifier
                .background(Color.Gray)
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            DropdownMenu(
                items = listOf(
                    Item(
                        id = "text1",
                        text = "Text only",
                        onClick = {},
                    ),
                    Item(
                        id = "textAndIcon1",
                        text = "Text and Icon",
                        isDefault = true,
                        leftIcon = R.drawable.ic_jetpack_logo_white_24dp,
                        hasDivider = true,
                        onClick = {},
                    ),
                    SubMenu(
                        id = "subMenu1",
                        text = "SubMenu",
                        items = listOf(
                            Item(
                                id = "subMenu1_text1",
                                text = "Text only sub-menu",
                                onClick = {},
                            )
                        ),
                        onClick = {},
                    ),
                )
            )
        }
    }
}
