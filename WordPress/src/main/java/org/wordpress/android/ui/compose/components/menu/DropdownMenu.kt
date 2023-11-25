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
    Column {
        var isMenuOpen by remember { mutableStateOf(false) }
        var openSubMenuId by remember { mutableStateOf("") }
        DropdownMenuButton(
            selectedItem = items.defaultItem(),
            onClick = {
                isMenuOpen = !isMenuOpen
                openSubMenuId = ""
            }
        )
        if (openSubMenuId.isNotEmpty()) {
            AnimatedVisibility(
                visible = openSubMenuId.isNotEmpty(),
            ) {
                DropdownMenuItemList(
                    items = (items.find { it.id == openSubMenuId } as SubMenu).items,
                )
            }
        } else {
            AnimatedVisibility(
                visible = isMenuOpen,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                DropdownMenuItemList(
                    items = items,
                    onItemClick = { id ->
                        items.firstOrNull { it.id == id }?.let {
                            it.onClick(it.id)
                            if (it is SubMenu) {
                                openSubMenuId = id
                            }
                        }
                    },
                )
            }
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
                                text = "Text only",
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
