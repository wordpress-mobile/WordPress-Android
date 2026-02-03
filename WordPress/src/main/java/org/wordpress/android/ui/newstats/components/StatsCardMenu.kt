package org.wordpress.android.ui.newstats.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.VerticalAlignBottom
import androidx.compose.material.icons.filled.VerticalAlignTop
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.wordpress.android.R

/**
 * Standard three-dots menu for stats cards.
 * Includes card-specific options (via additionalContent), a "Move Card" sub-menu, and a "Remove" option.
 *
 * @param onRemoveClick Callback invoked when user clicks "Remove"
 * @param modifier Modifier for the menu
 * @param cardPosition Optional position info to show move options. If null, move options are hidden.
 * @param onMoveUp Callback invoked when user clicks "Move Up"
 * @param onMoveToTop Callback invoked when user clicks "Move to Top"
 * @param onMoveDown Callback invoked when user clicks "Move Down"
 * @param onMoveToBottom Callback invoked when user clicks "Move to Bottom"
 * @param additionalContent Optional composable content for card-specific menu items,
 *                          called within the DropdownMenu. Should contain DropdownMenuItem(s).
 */
@Composable
fun StatsCardMenu(
    onRemoveClick: () -> Unit,
    modifier: Modifier = Modifier,
    cardPosition: CardPosition? = null,
    onMoveUp: (() -> Unit)? = null,
    onMoveToTop: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    onMoveToBottom: (() -> Unit)? = null,
    additionalContent: @Composable (() -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }
    var moveSubMenuExpanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = stringResource(R.string.more_options),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            // Card-specific options (e.g., chart type for ViewsStatsCard)
            additionalContent?.invoke()

            // Move Card sub-menu (shown only when cardPosition is provided and there are multiple cards)
            val showMoveOption = cardPosition != null && cardPosition.totalCards > 1
            if (showMoveOption) {
                Box {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.stats_card_move)) },
                        onClick = { moveSubMenuExpanded = true },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.SwapVert,
                                contentDescription = stringResource(R.string.stats_card_move)
                            )
                        },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = stringResource(R.string.stats_card_move_submenu)
                            )
                        }
                    )

                    // Move sub-menu
                    DropdownMenu(
                        expanded = moveSubMenuExpanded,
                        onDismissRequest = { moveSubMenuExpanded = false }
                    ) {
                        // Move Up (hidden for first card)
                        if (cardPosition?.canMoveUp == true && onMoveUp != null) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.stats_card_move_up)) },
                                onClick = {
                                    moveSubMenuExpanded = false
                                    expanded = false
                                    onMoveUp()
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowUp,
                                        contentDescription = stringResource(R.string.stats_card_move_up)
                                    )
                                }
                            )
                        }

                        // Move to Top (hidden for first card or when only 2 cards)
                        if (cardPosition?.canMoveUp == true &&
                            cardPosition.showMoveToTopBottom &&
                            onMoveToTop != null) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.stats_card_move_to_top)) },
                                onClick = {
                                    moveSubMenuExpanded = false
                                    expanded = false
                                    onMoveToTop()
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.VerticalAlignTop,
                                        contentDescription = stringResource(R.string.stats_card_move_to_top)
                                    )
                                }
                            )
                        }

                        // Move Down (hidden for last card)
                        if (cardPosition?.canMoveDown == true && onMoveDown != null) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.stats_card_move_down)) },
                                onClick = {
                                    moveSubMenuExpanded = false
                                    expanded = false
                                    onMoveDown()
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = stringResource(R.string.stats_card_move_down)
                                    )
                                }
                            )
                        }

                        // Move to Bottom (hidden for last card or when only 2 cards)
                        if (cardPosition?.canMoveDown == true &&
                            cardPosition.showMoveToTopBottom &&
                            onMoveToBottom != null) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.stats_card_move_to_bottom)) },
                                onClick = {
                                    moveSubMenuExpanded = false
                                    expanded = false
                                    onMoveToBottom()
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.VerticalAlignBottom,
                                        contentDescription = stringResource(R.string.stats_card_move_to_bottom)
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // Common "Remove" option
            DropdownMenuItem(
                text = { Text(stringResource(R.string.stats_card_remove)) },
                onClick = {
                    expanded = false
                    onRemoveClick()
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.stats_card_remove)
                    )
                }
            )
        }
    }
}

/**
 * Represents the position of a card in the stats card list.
 * Used to determine which move options should be shown.
 */
data class CardPosition(
    val index: Int,
    val totalCards: Int
) {
    val isFirst: Boolean get() = index == 0
    val isLast: Boolean get() = index == totalCards - 1
    val canMoveUp: Boolean get() = !isFirst
    val canMoveDown: Boolean get() = !isLast
    // Show "Move to Top/Bottom" only when there are more than 2 cards
    // (with 2 cards, "Move Up" and "Move to Top" are equivalent)
    val showMoveToTopBottom: Boolean get() = totalCards > 2
}

