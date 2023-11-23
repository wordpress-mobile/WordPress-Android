package org.wordpress.android.ui.compose.components.menu

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.menu.DropdownMenuItemData.SubMenu
import org.wordpress.android.ui.compose.components.menu.DropdownMenuItemData.Text
import org.wordpress.android.ui.compose.components.menu.DropdownMenuItemData.TextAndIcon
import org.wordpress.android.ui.compose.theme.AppThemeEditor
import java.lang.IllegalArgumentException

/**
 * DropdownMenu component. Consists of a DropdownMenuButton that opens a DropdownMenuItemsContainer when tapped.
 * @param items the dropdown menu items to be shown. There should be only one default item.
 */
@Composable
fun DropdownMenu(items: List<DropdownMenuItemData>) {
    require (items.hasSingleDefaultItem()) { "DropdownMenu must have one default item." }
    Column {
        DropdownMenuButton(
            selectedItem = items.defaultItem(),
            onClick = {
                TODO("Open menu")
            }
        )
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
        DropdownMenu(
            items = listOf(
                Text(
                    id = "item1",
                    text = "Item 1",
                    onClick = {},
                    isDefault = true,
                ),
                TextAndIcon(
                    id = "item1",
                    text = "Item 1",
                    iconRes = R.drawable.ic_jetpack_logo_24dp,
                    onClick = {},
                ),
                SubMenu(
                    id = "subMenu1",
                    text = "SubMenu",
                    items = listOf(
                        Text(
                            id = "item1",
                            text = "Item 1",
                            onClick = {},
                            isDefault = true,
                        ),
                        TextAndIcon(
                            id = "item1",
                            text = "Item 1",
                            iconRes = R.drawable.ic_jetpack_logo_24dp,
                            onClick = {},
                        )
                    ),
                    onClick = {},
                ),
            )
        )
    }
}
