package org.wordpress.android.ui.compose.components.menu

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.menu.DropdownMenuItemData.SubMenu
import org.wordpress.android.ui.compose.components.menu.DropdownMenuItemData.Text
import org.wordpress.android.ui.compose.components.menu.DropdownMenuItemData.TextAndIcon
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.compose.unit.FontSize

@Composable
fun DropdownMenuItemList(items: List<DropdownMenuItemData>) {
    Box {
        val listState = rememberLazyListState()
        LazyColumn(
            state = listState,
            modifier = Modifier
                .background(MaterialTheme.colors.background)
                .clip(shape = RoundedCornerShape(4.dp))
        ) {
            items(
                items = items,
                key = { it.id },
            ) { item ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(horizontal = dimensionResource(R.dimen.jp_migration_padding_horizontal))
                        .clickable { item.onClick(item.id) },
                ) {
                    if (item is TextAndIcon) {
                        Icon(
                            modifier = Modifier.align(Alignment.CenterVertically),
                            painter = painterResource(id = item.iconRes),
                            contentDescription = null,
                        )
                    }
                    Text(
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .weight(
                                weight = 1f,
                                fill = false,
                            ),
                        text = item.text,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                        fontSize = FontSize.Large.value,
                    )
                }
                Divider(
                    color = colorResource(R.color.gray_10),
                    thickness = 0.5.dp,
                    modifier = Modifier
                        .padding(horizontal = dimensionResource(R.dimen.jp_migration_padding_horizontal)),
                )
            }
        }
    }
}

@Preview
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DropdownMenuItemsListPreview() {
    AppTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
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
}
