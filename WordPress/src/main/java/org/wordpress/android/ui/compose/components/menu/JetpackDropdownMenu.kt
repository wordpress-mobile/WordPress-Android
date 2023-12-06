package org.wordpress.android.ui.compose.components.menu

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import me.saket.cascade.CascadeDropdownMenu
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppTheme

@Composable
fun JetpackDropdownMenu(menuItems: List<DropdownMenuElementData>, defaultItem: DropdownMenuElementData) {
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
            modifier = Modifier.background(MaterialTheme.colorScheme.background),
            expanded = isMenuVisible,
            onDismissRequest = { isMenuVisible = false },
        ) {
            menuItems.forEach { element ->
                JetpackDropdownMenuItem(
                    element = element,
                    onMenuItemClick = { clickedItem ->
                        selectedItem = clickedItem
                        isMenuVisible = false
                    }
                )
                if (element.hasDivider) {
                    Divider()
                }
            }
        }
    }
}

@Suppress("LongParameterList")
sealed class DropdownMenuElementData(
    open val text: String,
    @DrawableRes open val leadingIcon: Int,
    open val hasDivider: Boolean,
) {
    // Item element that closes the menu when clicked
    data class Item(
        override val text: String,
        @DrawableRes override val leadingIcon: Int = NO_ICON,
        override val hasDivider: Boolean = false,
        val onClick: (Item) -> Unit,
    ) : DropdownMenuElementData(text, leadingIcon, hasDivider)

    // Sub-menu element that opens a sub-menu when clicked
    data class SubMenu(
        override val text: String,
        @DrawableRes override val leadingIcon: Int = NO_ICON,
        override val hasDivider: Boolean = false,
        val children: List<DropdownMenuElementData> = emptyList(),
    ) : DropdownMenuElementData(text, leadingIcon, hasDivider)
}

internal const val NO_ICON = -1

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun JetpackDropdownMenuPreview() {
    AppTheme {
        Box(
            modifier = Modifier
                .background(Color.Gray)
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            val menuItems = listOf(
                DropdownMenuElementData.Item(
                    text = "Text only",
                    onClick = {}
                ),
                DropdownMenuElementData.Item(
                    text = "Text and left icon",
                    onClick = {},
                    leadingIcon = R.drawable.ic_jetpack_logo_white_24dp,
                    hasDivider = true,
                ),
                DropdownMenuElementData.SubMenu(
                    text = "Text and sub-menu",
                    children = listOf(
                        DropdownMenuElementData.Item(
                            text = "Text sub-menu 1",
                            onClick = {}
                        ),
                        DropdownMenuElementData.Item(
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
