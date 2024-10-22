package org.wordpress.android.ui.compose.components.menu.dropdown

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
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import me.saket.cascade.CascadeColumnScope
import me.saket.cascade.CascadeDropdownMenu
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM2
import org.wordpress.android.ui.compose.utils.uiStringText
import org.wordpress.android.ui.utils.UiString.UiStringText

@Composable
fun JetpackDropdownMenu(
    menuItems: List<MenuElementData>,
    selectedItem: MenuElementData.Item.Single,
    onSingleItemClick: (MenuElementData.Item.Single) -> Unit,
    menuButtonHeight: Dp = 36.dp,
    contentSizeAnimation: FiniteAnimationSpec<IntSize> = spring(),
    onDropdownMenuClick: () -> Unit,
) {
    Column {
        var isMenuVisible by remember { mutableStateOf(false) }
        DropdownMenuButton(
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
                .background(MenuColors.itemBackgroundColor()),
            expanded = isMenuVisible,
            fixedWidth = cascadeMenuWidth,
            onDismissRequest = { isMenuVisible = false },
            offset = DpOffset(
                x = if (LocalLayoutDirection.current == LayoutDirection.Rtl) cascadeMenuWidth else 0.dp,
                y = 0.dp
            )
        ) {
            val onMenuItemSingleClick: (MenuElementData.Item.Single) -> Unit = { clickedItem ->
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
    element: MenuElementData,
    onMenuItemSingleClick: (MenuElementData.Item.Single) -> Unit
) {
    when (element) {
        is MenuElementData.Divider -> Divider(
            color = MenuColors.itemDividerColor(),
        )

        is MenuElementData.Item -> {
            when (element) {
                is MenuElementData.Item.Single -> Single(element, onMenuItemSingleClick)
                is MenuElementData.Item.SubMenu -> SubMenu(element, onMenuItemSingleClick)
            }
        }
    }
}

@Composable
private fun Single(
    element: MenuElementData.Item.Single,
    onMenuItemSingleClick: (MenuElementData.Item.Single) -> Unit,
) {
    val enabledContentColor = MenuColors.itemContentColor()
    val disabledContentColor = enabledContentColor.copy(alpha = ContentAlpha.disabled)
    DropdownMenuItem(
        modifier = Modifier
            .background(MenuColors.itemBackgroundColor()),
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
            .background(MenuColors.itemBackgroundColor())
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
                val backIconResource = if(LocalLayoutDirection.current == LayoutDirection.Rtl) {
                    R.drawable.ic_arrow_right_white_24dp
                } else {
                    R.drawable.ic_arrow_left_white_24dp
                }
                Image(
                    painter = painterResource(backIconResource),
                    contentDescription = stringResource(R.string.reader_label_toolbar_back),
                    colorFilter = ColorFilter.tint(MenuColors.itemContentColor()),
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
    element: MenuElementData.Item.SubMenu,
    onMenuItemSingleClick: (MenuElementData.Item.Single) -> Unit,
) {
    val enabledContentColor = MenuColors.itemContentColor()
    val disabledContentColor = enabledContentColor.copy(alpha = ContentAlpha.disabled)
    DropdownMenuItem(
        modifier = Modifier
            .background(MenuColors.itemBackgroundColor()),
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
        MenuElementData.Item.Single(
            id = "text-only",
            text = UiStringText("Text only"),
        ),
        MenuElementData.Item.Single(
            id = "text-and-icon",
            text = UiStringText("Text and leading icon"),
            leadingIcon = R.drawable.ic_jetpack_logo_white_24dp,
        ),
        MenuElementData.Divider,
        MenuElementData.Item.SubMenu(
            id = "text-and-sub-menu",
            text = UiStringText("Text and sub-menu"),
            children = listOf(
                MenuElementData.Item.Single(
                    id = "text-sub-menu-1",
                    text = UiStringText("Text sub-menu 1"),
                ),
                MenuElementData.Item.Single(
                    id = "text-sub-menu-2",
                    text = UiStringText("Text sub-menu 2"),
                )
            )
        ),
    )
    var selectedItem by remember { mutableStateOf(menuItems.first() as MenuElementData.Item.Single) }

    AppThemeM2 {
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
