package org.wordpress.android.ui.reader.views.compose.dropdown

import android.content.res.Configuration
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import me.saket.cascade.CascadeColumnScope
import me.saket.cascade.CascadeDropdownMenu
import org.wordpress.android.R
import org.wordpress.android.reader.R as ReaderR
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.compose.utils.uiStringText
import org.wordpress.android.ui.utils.UiString.UiStringText

@Composable
fun JetpackDropdownMenu(
    menuItems: List<JetpackMenuElementData>,
    selectedItem: JetpackMenuElementData.Item.Single,
    onSingleItemClick: (JetpackMenuElementData.Item.Single) -> Unit,
    menuButtonHeight: Dp = 36.dp,
    contentSizeAnimation: FiniteAnimationSpec<IntSize> = spring(),
    onDropdownMenuClick: () -> Unit,
) {
    Column {
        var isMenuVisible by remember { mutableStateOf(false) }
        JetpackDropdownMenuButton(
            height = menuButtonHeight,
            contentSizeAnimation = contentSizeAnimation,
            selectedItem = selectedItem,
            onClick = {
                onDropdownMenuClick()
                isMenuVisible = !isMenuVisible
            }
        )
        val cascadeMenuWidth = 200.dp
        CascadeDropdownMenu(
            modifier = Modifier
                .background(JetpackDropdownMenuColors.itemBackgroundColor()),
            expanded = isMenuVisible,
            fixedWidth = cascadeMenuWidth,
            onDismissRequest = { isMenuVisible = false },
            offset = DpOffset(
                x = if (LocalLayoutDirection.current == LayoutDirection.Rtl) cascadeMenuWidth else 0.dp,
                y = 0.dp
            )
        ) {
            val onMenuItemSingleClick: (JetpackMenuElementData.Item.Single) -> Unit = { clickedItem ->
                isMenuVisible = false
                onSingleItemClick(clickedItem)
            }
            menuItems.forEach { element ->
                MenuElementComposable(element = element, onMenuItemSingleClick = onMenuItemSingleClick)
            }
        }
    }
}

@Composable
private fun CascadeColumnScope.MenuElementComposable(
    element: JetpackMenuElementData,
    onMenuItemSingleClick: (JetpackMenuElementData.Item.Single) -> Unit
) {
    when (element) {
        is JetpackMenuElementData.Divider -> HorizontalDivider(color = JetpackDropdownMenuColors.itemDividerColor())

        is JetpackMenuElementData.Item -> {
            when (element) {
                is JetpackMenuElementData.Item.Single -> Single(element, onMenuItemSingleClick)
                is JetpackMenuElementData.Item.SubMenu -> SubMenu(element, onMenuItemSingleClick)
            }
        }
    }
}

@Composable
private fun Single(
    element: JetpackMenuElementData.Item.Single,
    onMenuItemSingleClick: (JetpackMenuElementData.Item.Single) -> Unit,
) {
    val enabledContentColor = JetpackDropdownMenuColors.itemContentColor()
    val disabledContentColor = enabledContentColor.copy(alpha = 0.38f)
    DropdownMenuItem(
        modifier = Modifier
            .background(JetpackDropdownMenuColors.itemBackgroundColor()),
        onClick = {
            onMenuItemSingleClick(element)
        },
        colors = MenuDefaults.itemColors(
            textColor = enabledContentColor,
            leadingIconColor = enabledContentColor,
            trailingIconColor = enabledContentColor,
            disabledTextColor = disabledContentColor,
            disabledLeadingIconColor = disabledContentColor,
            disabledTrailingIconColor = disabledContentColor,
        ),
        text = {
            Text(
                text = uiStringText(element.text),
                style = MaterialTheme.typography.bodyLarge,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
            )
        },
        leadingIcon = if (element.leadingIcon != NO_ICON) {
            {
                Icon(
                    painter = painterResource(id = element.leadingIcon),
                    contentDescription = null,
                )
            }
        } else null,
    )
}

@Composable
private fun CascadeColumnScope.SubMenuHeader(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(10.5.dp),
    text: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .background(JetpackDropdownMenuColors.itemBackgroundColor())
            .fillMaxWidth()
            .clickable(enabled = hasParentMenu, role = Role.Button) {
                if (!isNavigationRunning) {
                    cascadeState.navigateBack()
                }
            }
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(
            LocalTextStyle provides MaterialTheme.typography.bodyLarge
        ) {
            if (this@SubMenuHeader.hasParentMenu) {
                val backIconResource = if (LocalLayoutDirection.current == LayoutDirection.Rtl) {
                    R.drawable.ic_arrow_right_white_24dp
                } else {
                    R.drawable.ic_arrow_left_white_24dp
                }
                Image(
                    painter = painterResource(backIconResource),
                    contentDescription = stringResource(ReaderR.string.reader_label_toolbar_back),
                    colorFilter = ColorFilter.tint(JetpackDropdownMenuColors.itemContentColor()),
                )
            }
            Box(Modifier.weight(1f)) {
                text?.invoke()
            }
        }
    }
}

@Composable
private fun CascadeColumnScope.SubMenu(
    element: JetpackMenuElementData.Item.SubMenu,
    onMenuItemSingleClick: (JetpackMenuElementData.Item.Single) -> Unit,
) {
    val enabledContentColor = JetpackDropdownMenuColors.itemContentColor()
    val disabledContentColor = enabledContentColor.copy(alpha = 0.38f)
    DropdownMenuItem(
        modifier = Modifier
            .background(JetpackDropdownMenuColors.itemBackgroundColor()),
        colors = MenuDefaults.itemColors(
            textColor = enabledContentColor,
            leadingIconColor = enabledContentColor,
            trailingIconColor = enabledContentColor,
            disabledTextColor = disabledContentColor,
            disabledLeadingIconColor = disabledContentColor,
            disabledTrailingIconColor = disabledContentColor,
        ),
        text = {
            Text(
                text = uiStringText(element.text),
                style = MaterialTheme.typography.bodyLarge,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
            )
        },
        children = {
            element.children.forEach {
                MenuElementComposable(element = it, onMenuItemSingleClick = onMenuItemSingleClick)
            }
        },
        childrenHeader = {
            SubMenuHeader()
        }
    )
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun JetpackDropdownMenuPreview() {
    val menuItems = listOf(
        JetpackMenuElementData.Item.Single(
            id = "text-only",
            text = UiStringText("Text only"),
        ),
        JetpackMenuElementData.Item.Single(
            id = "text-and-icon",
            text = UiStringText("Text and leading icon"),
            leadingIcon = R.drawable.ic_jetpack_logo_white_24dp,
        ),
        JetpackMenuElementData.Divider,
        JetpackMenuElementData.Item.SubMenu(
            id = "text-and-sub-menu",
            text = UiStringText("Text and sub-menu"),
            children = listOf(
                JetpackMenuElementData.Item.Single(
                    id = "text-sub-menu-1",
                    text = UiStringText("Text sub-menu 1"),
                ),
                JetpackMenuElementData.Item.Single(
                    id = "text-sub-menu-2",
                    text = UiStringText("Text sub-menu 2"),
                )
            )
        ),
    )
    var selectedItem by remember { mutableStateOf(menuItems.first() as JetpackMenuElementData.Item.Single) }

    AppThemeM3 {
        Box(
            modifier = Modifier
                .padding(start = 8.dp, top = 8.dp)
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            JetpackDropdownMenu(
                selectedItem = selectedItem,
                menuItems = menuItems,
                onSingleItemClick = { selectedItem = it },
                onDropdownMenuClick = {},
            )
        }
    }
}
