package org.wordpress.android.ui.compose.components.menu

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.menu.DropdownMenuItemData.*
import org.wordpress.android.ui.compose.theme.AppThemeEditor

/**
 * DropdownMenu component.
 * @param items the dropdown menu items to be shown. There should be only one default item.
 */
@Composable
fun DropdownMenu(items: List<DropdownMenuItemData>) {
    if (items.hasSingleDefaultItem()) {
        throw IllegalArgumentException("DropdownMenu ")
    }
}

fun List<DropdownMenuItemData>.hasSingleDefaultItem(): Boolean {
    val defaultTextSize = filterIsInstance<Text>()
        .filter { it.isDefault }.size
    val defaultTextAndIconSize = filterIsInstance<TextAndIcon>()
        .filter { it.isDefault }.size
    val defaultSubmenuItemsSize = filterIsInstance<SubMenu>()
        .flatMap { it.items }
        .filter { it.isDefault }.size
    return (defaultTextSize + defaultTextAndIconSize + defaultSubmenuItemsSize) == 1
}

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
                    icon = R.drawable.ic_jetpack_logo_24dp,
                    onClick = {},
                ),
                SubMenu(
                    text = "SubMenu",
                    items = listOf(
                        SubMenu.Item(
                            id = "item1",
                            text = "Item 1",
                            onClick = {},
                            isDefault = true,
                        )
                    )
                ),
            )
        )
    }
}
