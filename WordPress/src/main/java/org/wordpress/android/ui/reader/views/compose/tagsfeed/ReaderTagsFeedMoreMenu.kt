package org.wordpress.android.ui.reader.views.compose.tagsfeed

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import me.saket.cascade.CascadeColumnScope
import me.saket.cascade.CascadeDropdownMenu
import org.wordpress.android.ui.compose.components.menu.dropdown.MenuColors
import org.wordpress.android.ui.compose.unit.Margin
import org.wordpress.android.ui.compose.utils.horizontalFadingEdges
import org.wordpress.android.ui.compose.utils.uiStringText
import org.wordpress.android.ui.utils.UiString

@Composable
fun ReaderTagsFeedMoreMenu(
    expanded: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
//            .horizontalScroll(scrollState)
//            .horizontalFadingEdges(scrollState, startEdgeSize = 0.dp)
            .padding(start = Margin.ExtraLarge.value),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CascadeDropdownMenu(
            modifier = Modifier
                .background(MenuColors.itemBackgroundColor()),
        expanded = expanded,
//        fixedWidth = cascadeMenuWidth,
            fixedWidth = 200.dp,
//        onDismissRequest = { isMenuVisible = false },
            onDismissRequest = {},
            offset = DpOffset(
//            x = if (LocalLayoutDirection.current == LayoutDirection.Rtl) cascadeMenuWidth else 0.dp,
                x = if (LocalLayoutDirection.current == LayoutDirection.Rtl) 200.dp else 0.dp,
                y = 0.dp
            )
        ) {
//        val onMenuItemSingleClick: (MenuElementData.Item.Single) -> Unit = { clickedItem ->
//            isMenuVisible = false
//            onSingleItemClick(clickedItem)
//        }
//        menuItems.forEach { element ->
//            MenuElementComposable(element = element, onMenuItemSingleClick = onMenuItemSingleClick)
//        }
            androidx.compose.material3.DropdownMenuItem(
                modifier = Modifier
                    .background(MenuColors.itemBackgroundColor()),
                onClick = {
                },
                text = {
                    Text(
                        text = uiStringText(UiString.UiStringText("Text 1")),
                        style = MaterialTheme.typography.bodyLarge,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1,
                    )
                },
//            leadingIcon = if (element.leadingIcon != NO_ICON) {
//                {
//                    Icon(
//                        painter = painterResource(id = element.leadingIcon),
//                        contentDescription = null,
//                    )
//                }
//            } else null,
            )
            androidx.compose.material3.DropdownMenuItem(
                modifier = Modifier
                    .background(MenuColors.itemBackgroundColor()),
                onClick = {
                },
                text = {
                    Text(
                        text = uiStringText(UiString.UiStringText("Text 2")),
                        style = MaterialTheme.typography.bodyLarge,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1,
                    )
                },
//            leadingIcon = if (element.leadingIcon != NO_ICON) {
//                {
//                    Icon(
//                        painter = painterResource(id = element.leadingIcon),
//                        contentDescription = null,
//                    )
//                }
//            } else null,
            )
        }
    }
}
