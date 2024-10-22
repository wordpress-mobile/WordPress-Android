package org.wordpress.android.ui.mysite.cards.compose

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.card.UnelevatedCard
import org.wordpress.android.ui.compose.styles.DashboardCardTypography
import org.wordpress.android.ui.compose.theme.AppThemeM2

/**
 * A toolbar for MySite cards written in Compose, that tries to match behavior and positioning of cards written in XML.
 *
 * When using this component, there is no need to set top, start, and end padding on the card, as this component sets
 * those values internally to closely match my_site_card_toolbar.xml.
 *
 * It's important to note this component is stateful, meaning the UI for the context menu is managed internally. Use
 * [onContextMenuClick] and the [contextMenuItems] callbacks to handle behavior NOT related to the dropdown menu UI.
 *
 * @param modifier Modifier to be applied to the toolbar (should not be used in most cases).
 * @param onContextMenuClick Callback to be invoked when the context menu is clicked.
 * @param contextMenuItems List of [MySiteCardToolbarContextMenuItem] to be displayed in the context menu.
 * @param showContextMenu Whether or not to show the context menu. Default: true if [contextMenuItems] is not empty.
 * @param content Content to be displayed in the toolbar. Usually some sort of title that will be left aligned and
 * vertically aligned with the context menu. If null, the context menu will be right aligned.
 */
@Composable
fun MySiteCardToolbar(
    modifier: Modifier = Modifier,
    onContextMenuClick: (() -> Unit)? = null,
    contextMenuItems: List<MySiteCardToolbarContextMenuItem> = emptyList(),
    showContextMenu: Boolean = contextMenuItems.isNotEmpty(),
    content: @Composable (RowScope.() -> Unit)? = null,
) {
    val horizontalArrangement = Arrangement.SpaceBetween.takeIf { content != null } ?: Arrangement.End
    val padding = if (showContextMenu) {
        PaddingValues(start = 16.dp, end = 12.dp, top = 8.dp)
    } else {
        PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = horizontalArrangement,
        modifier = modifier
            .padding(padding)
            .fillMaxWidth()
    ) {
        content?.invoke(this)

        if (content != null && showContextMenu) {
            // minimum spacing between content and context menu if both are shown
            Spacer(modifier = Modifier.width(16.dp))
        }

        if (showContextMenu) {
            CardDropDownMenu(
                onContextMenuClick = onContextMenuClick,
                contextMenuItems = contextMenuItems,
            )
        }
    }
}

@Composable
private fun CardDropDownMenu(
    modifier: Modifier = Modifier,
    onContextMenuClick: (() -> Unit)? = null,
    contextMenuItems: List<MySiteCardToolbarContextMenuItem> = emptyList(),
) {
    Box(modifier = modifier) {
        var isExpanded by remember { mutableStateOf(false) }

        IconButton(
            modifier = Modifier.size(32.dp), // to match the icon in my_site_card_toolbar.xml
            onClick = {
                isExpanded = true
                onContextMenuClick?.invoke()
            }
        ) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = stringResource(id = R.string.more),
                tint = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium),
            )
        }

        DropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false },
            modifier = Modifier.background(MaterialTheme.colors.surface.copy(alpha = ContentAlpha.high))
        ) {
            contextMenuItems.map { item ->
                when (item) {
                    is MySiteCardToolbarContextMenuItem.Option -> {
                        DropdownMenuItem(
                            text = { Text(item.text) },
                            onClick = {
                                isExpanded = false
                                item.onClick()
                            }
                        )
                    }

                    MySiteCardToolbarContextMenuItem.Divider -> Divider()
                }
            }
        }
    }
}

sealed interface MySiteCardToolbarContextMenuItem {
    data class Option(
        val text: String,
        val onClick: () -> Unit,
    ) : MySiteCardToolbarContextMenuItem

    data object Divider : MySiteCardToolbarContextMenuItem
}

@Preview(
    name = "Light Mode"
)
@Preview(
    name = "Dark Mode",
    showBackground = true,
    uiMode = UI_MODE_NIGHT_YES
)
@Composable
private fun MySiteCardToolbarPreview() {
    AppThemeM2 {
        MySiteCardToolbar(
            onContextMenuClick = {},
            contextMenuItems = listOf(
                MySiteCardToolbarContextMenuItem.Option(
                    text = "An option",
                    onClick = {}
                ),
                MySiteCardToolbarContextMenuItem.Divider,
                MySiteCardToolbarContextMenuItem.Option(
                    text = "Another option",
                    onClick = {}
                ),
            ),
        ) {
            Text(
                text = "Card Title",
                style = DashboardCardTypography.smallTitle,
            )
        }
    }
}

@Preview(
    name = "Light Mode"
)
@Preview(
    name = "Dark Mode",
    showBackground = true,
    uiMode = UI_MODE_NIGHT_YES
)
@Composable
private fun MySiteCardToolbarInCardPreview() {
    AppThemeM2 {
        UnelevatedCard(
            modifier = Modifier.padding(8.dp)
        ) {
            Column {
                MySiteCardToolbar(
                    onContextMenuClick = {},
                    contextMenuItems = listOf(
                        MySiteCardToolbarContextMenuItem.Option(
                            text = "An option",
                            onClick = {}
                        ),
                        MySiteCardToolbarContextMenuItem.Divider,
                        MySiteCardToolbarContextMenuItem.Option(
                            text = "Another option",
                            onClick = {}
                        ),
                    ),
                ) {
                    Text(
                        text = "Card Title",
                        style = DashboardCardTypography.smallTitle,
                    )
                }

                Box(Modifier.padding(16.dp)) {
                    Text(
                        text = "This is my card content!"
                    )
                }
            }
        }
    }
}
