package org.wordpress.android.ui.navmenus.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.navmenus.MenuItemListUiState
import org.wordpress.android.ui.navmenus.MenuItemUiModel

@Composable
fun MenuItemListScreen(
    state: MenuItemListUiState,
    onEditItemClick: (Long) -> Unit,
    onMoveItemUp: (Long) -> Unit,
    onMoveItemDown: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            state.isLoading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            state.error != null -> {
                Text(
                    text = state.error,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            state.items.isEmpty() -> {
                Text(
                    text = stringResource(R.string.no_menu_items),
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = state.items,
                        key = { it.id }
                    ) { item ->
                        val index = state.items.indexOf(item)
                        val indentLevel = item.indentLevel

                        // Find previous sibling: look backwards for an item at the same indent
                        // level, stopping if we hit an item with a lower indent level (different
                        // parent context)
                        val canMoveUp = run {
                            for (i in (index - 1) downTo 0) {
                                val otherItem = state.items[i]
                                if (otherItem.indentLevel < indentLevel) {
                                    return@run false
                                }
                                if (otherItem.indentLevel == indentLevel) {
                                    return@run true
                                }
                            }
                            false
                        }

                        // Find next sibling: look forwards for an item at the same indent level,
                        // stopping if we hit an item with a lower indent level (different parent
                        // context)
                        val canMoveDown = run {
                            for (i in (index + 1) until state.items.size) {
                                val otherItem = state.items[i]
                                if (otherItem.indentLevel < indentLevel) {
                                    return@run false
                                }
                                if (otherItem.indentLevel == indentLevel) {
                                    return@run true
                                }
                            }
                            false
                        }

                        Column(
                            modifier = Modifier.animateItem(
                                placementSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            )
                        ) {
                            MenuItemListItem(
                                item = item,
                                canMoveUp = canMoveUp,
                                canMoveDown = canMoveDown,
                                onEditClick = { onEditItemClick(item.id) },
                                onMoveUp = { onMoveItemUp(item.id) },
                                onMoveDown = { onMoveItemDown(item.id) }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuItemListItem(
    item: MenuItemUiModel,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onEditClick: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    val showReorderButtons = canMoveUp || canMoveDown
    val title = item.title.ifEmpty { stringResource(R.string.untitled) }
    val itemDescription = stringResource(
        R.string.menu_item_accessibility_description,
        item.indentLevel + 1,
        title,
        item.typeLabel
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .semantics { contentDescription = itemDescription }
            .clickable { onEditClick() }
            .padding(
                start = (16 + item.indentLevel * 24).dp,
                end = if (showReorderButtons) 8.dp else 16.dp,
                top = 12.dp,
                bottom = 12.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.typeLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (item.description.isNotEmpty()) {
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (showReorderButtons) {
            if (canMoveUp) {
                IconButton(onClick = onMoveUp) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = stringResource(R.string.move_up))
                }
            }
            if (canMoveDown) {
                IconButton(onClick = onMoveDown) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = stringResource(R.string.move_down))
                }
            }
        }
    }
}
