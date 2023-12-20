package org.wordpress.android.ui.reader.views.compose

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.menu.dropdown.JetpackDropdownMenu
import org.wordpress.android.ui.compose.components.menu.dropdown.MenuElementData
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.compose.unit.Margin

@Composable
fun ReaderTopAppBar(
    onSearchClick: () -> Unit,
    readerLists: List<MenuElementData.Item> = emptyList(),
) {
    val menuItems = mutableListOf<MenuElementData>(
        MenuElementData.Item.Single(
            id = "discover",
            text = stringResource(id = R.string.reader_dropdown_menu_discover),
            leadingIcon = R.drawable.ic_reader_discover_24dp,
        ),
        MenuElementData.Item.Single(
            id = "subscriptions",
            text = stringResource(id = R.string.reader_dropdown_menu_subscriptions),
            leadingIcon = R.drawable.ic_reader_subscriptions_24dp,
        ),
        MenuElementData.Item.Single(
            id = "notifications",
            text = stringResource(id = R.string.reader_dropdown_menu_saved),
            leadingIcon = R.drawable.ic_reader_saved_24dp,
        ),
        MenuElementData.Item.Single(
            id = "notifications",
            text = stringResource(id = R.string.reader_dropdown_menu_liked),
            leadingIcon = R.drawable.ic_reader_liked_24dp,
        ),
    ).apply {
        if (readerLists.isNotEmpty()) {
            add(MenuElementData.Divider)
            MenuElementData.Item.SubMenu(
                id = "lists",
                text = stringResource(id = R.string.reader_dropdown_menu_lists),
                children = readerLists,
            )
        }
    }
    var selectedItem by remember {
        mutableStateOf(menuItems.filterIsInstance<MenuElementData.Item.Single>().first())
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(
                start = Margin.ExtraLarge.value,
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            JetpackDropdownMenu(
                selectedItem = selectedItem,
                menuItems = menuItems,
                onSingleItemClick = { selectedItem = it },
            )
        }
        Spacer(Modifier.width(Margin.ExtraLarge.value))
        IconButton(
            modifier = Modifier.align(Alignment.CenterVertically),
            onClick = { onSearchClick() }
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_magnifying_glass_16dp),
                contentDescription = stringResource(
                    R.string.reader_search_content_description
                ),
                tint = MaterialTheme.colors.onSurface,
            )
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ReaderScreenPreview() {
    AppTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            ReaderTopAppBar(
                {},
                readerLists = listOf(
                    MenuElementData.Item.Single(
                        id = "funny-blog-1",
                        text = "Funny Blog 1",
                    ),
                    MenuElementData.Item.Single(
                        id = "funny-blog-2",
                        text = "Funny Blog 2",
                    ),
                )
            )
        }
    }
}
