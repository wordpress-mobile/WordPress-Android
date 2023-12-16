package org.wordpress.android.ui.reader.views.compose

import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.menu.dropdown.JetpackDropdownMenu
import org.wordpress.android.ui.compose.components.menu.dropdown.MenuElementData
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.compose.unit.Margin

@Composable
fun ReaderScreen(
    readerLists: List<MenuElementData.Item.SubMenu> = emptyList()
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = Margin.ExtraLarge.value,
                top = Margin.ExtraLarge.value,
                bottom = Margin.ExtraLarge.value,
            )
    ) {
        Row(
            modifier = Modifier
                .weight(1f),
        ) {
            JetpackDropdownMenu(
                menuItems = mutableListOf<MenuElementData>(
                    MenuElementData.Item.Single(
                        text = stringResource(id = R.string.reader_dropdown_menu_discover),
                        onClick = {},
                        leadingIcon = R.drawable.ic_reader_discover_24dp,
                    ),
                    MenuElementData.Item.Single(
                        text = stringResource(id = R.string.reader_dropdown_menu_subscriptions),
                        onClick = {},
                        leadingIcon = R.drawable.ic_reader_subscriptions_24dp,
                    ),
                    MenuElementData.Item.Single(
                        text = stringResource(id = R.string.reader_dropdown_menu_saved),
                        onClick = {},
                        leadingIcon = R.drawable.ic_reader_saved_24dp,
                    ),
                    MenuElementData.Item.Single(
                        text = stringResource(id = R.string.reader_dropdown_menu_liked),
                        onClick = {},
                        leadingIcon = R.drawable.ic_reader_liked_24dp,
                    ),
                ).apply {
                    if (readerLists.isNotEmpty()) {
                        add(MenuElementData.Divider)
                        addAll(readerLists)
                    }
                }
            )
        }
        Spacer(Modifier.width(Margin.ExtraLarge.value))
        IconButton(onClick = { /*TODO open search*/ }) {
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
            ReaderScreen()
        }
    }
}
