package org.wordpress.android.ui.compose.components.menu

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.menu.DropdownMenuItemData.SubMenu
import org.wordpress.android.ui.compose.components.menu.DropdownMenuItemData.Text
import org.wordpress.android.ui.compose.components.menu.DropdownMenuItemData.TextAndIcon
import org.wordpress.android.ui.compose.theme.AppTheme

@Composable
fun DropdownMenuItemList(items: List<DropdownMenuItemData>) {
    Box {
        val listState = rememberLazyListState()
        Card(
            shape = RoundedCornerShape(4.dp),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .width(200.dp)
                    .background(MaterialTheme.colors.background)
            ) {
                items(
                    items = items,
                    key = { it.id },
                ) { item ->
                    DropdownMenuItem(item)
                    Divider(
                        color = colorResource(R.color.gray_10),
                        thickness = 0.5.dp,
                    )
                }
            }
        }
    }
}

@Preview
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DropdownMenuItemsListPreview() {
    AppTheme {
        DropdownMenuItemList(
            items = listOf(
                Text(
                    id = "text1",
                    text = "Text only",
                    onClick = {},
                ),
                TextAndIcon(
                    id = "textAndIcon1",
                    text = "Text and Icon",
                    iconRes = R.drawable.ic_jetpack_logo_white_24dp,
                    onClick = {},
                ),
                SubMenu(
                    id = "subMenu1",
                    text = "SubMenu",
                    items = listOf(
                        Text(
                            id = "subMenu1_text1",
                            text = "Text only",
                            onClick = {},
                        )
                    ),
                    onClick = {},
                )
            )
        )
    }
}
