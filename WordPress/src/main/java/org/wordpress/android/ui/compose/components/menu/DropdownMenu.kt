package org.wordpress.android.ui.compose.components.menu

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
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
import androidx.compose.runtime.saveable.rememberSaveable
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
 * @param menuItems the dropdown menu items to be shown. There should be only one default item.
 */
@Composable
fun DropdownMenu(menuItems: List<DropdownMenuItemData>) {
    require(menuItems.hasSingleDefaultItem()) { "DropdownMenu must have one default item." }
    // currentMenuItems can either be the default menu received as a parameter OR the sub-menu items if a
    // sub-menu is selected.
    var currentMenuItems by remember { mutableStateOf(menuItems) }
    var selectedItemId by rememberSaveable { mutableStateOf(menuItems.defaultItem().id) }
    var openSubMenuId by remember { mutableStateOf("") }
    val menuVisibleState = remember { MutableTransitionState(false) }
    Column {
        DropdownMenuButton(
            selectedItem = menuItems.findById(selectedItemId) ?: menuItems.defaultItem(),
            onClick = {
                menuVisibleState.targetState = !menuVisibleState.targetState
                openSubMenuId = ""
            }
        )
        AnimatedVisibility(
            visibleState = menuVisibleState,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            DropdownMenuItemList(
                items = currentMenuItems,
                onItemClick = { id ->
                    if (id == BACK_BUTTON_ID) {
                        openSubMenuId = ""
                        currentMenuItems = menuItems
                        return@DropdownMenuItemList
                    }
                    val clickedItem = if (openSubMenuId.isNotEmpty()) {
                        // Clicked item is inside a sub-menu
                        (menuItems.firstOrNull { it.id == openSubMenuId } as SubMenu).items
                            .find { it.id == id }
                    } else {
                        // Clicked item is not inside a sub-menu
                        menuItems.firstOrNull { it.id == id }
                    }
                    clickedItem?.let {
                        it.onClick(it.id)
                        if (it is SubMenu) {
                            // If the clicked item is of SubMenu type we should keep its ID
                            openSubMenuId = id
                            // We should also update the currentMenuItems to match the sub-menu items.
                            // By design the first item on every sub-menu should be a back button
                            // that leads user back to the default menu.
                            val backMenuItem = Item(
                                id = BACK_BUTTON_ID,
                                text = "",
                                onClick = {},
                                leftIcon = R.drawable.ic_arrow_left_white_24dp,
                            )
                            val itemsWithBackButton = mutableListOf<DropdownMenuItemData>().apply {
                                add(backMenuItem)
                                addAll(it.items)
                            }
                            currentMenuItems = itemsWithBackButton
                        } else {
                            // If the clicked item is not a SubMenu, we should close the menu (by making the menu
                            // visible state target state false) and update the selectedItem.
                            // The open sub-menu ID should also be changed to its default value (empty string) since
                            // the menu will be closed.
                            menuVisibleState.targetState = false
                            selectedItemId = it.id
                            openSubMenuId = ""
                            // The currentMenuItems are not updated here because we have to wait until the menu closing
                            // animation ends.
                        }
                    }
                },
            )
        }

        // Close menu animation has ended
        if (!menuVisibleState.targetState && !menuVisibleState.currentState) {
            // The menu has been closed. We should set the currentMenuItems to be the default menu.
            // We should only update the items after the menu close animation has ended, otherwise
            // the user can potentially see the wrong items being shown while a sub-menu closes.
            currentMenuItems = menuItems
        }
    }
}

private fun List<DropdownMenuItemData>.hasSingleDefaultItem() = filter { it.isDefault }.size == 1

private fun List<DropdownMenuItemData>.defaultItem() =
    find { it.isDefault } ?: throw IllegalArgumentException("Default item must not be null.")

private fun List<DropdownMenuItemData>.findById(id: String) =
    firstOrNull { it.id == id } ?: filterIsInstance<SubMenu>().flatMap { it.items }.firstOrNull { it.id == id }

private const val BACK_BUTTON_ID = "back"

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
                menuItems = listOf(
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
