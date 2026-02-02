package org.wordpress.android.ui.newstats.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
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
 * Includes card-specific options (via additionalContent) and a "Remove" option.
 *
 * @param onRemoveClick Callback invoked when user clicks "Remove"
 * @param modifier Modifier for the menu
 * @param additionalContent Optional composable content for card-specific menu items,
 *                          called within the DropdownMenu. Should contain DropdownMenuItem(s).
 */
@Composable
fun StatsCardMenu(
    onRemoveClick: () -> Unit,
    modifier: Modifier = Modifier,
    additionalContent: @Composable (() -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }

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
                        contentDescription = null
                    )
                }
            )
        }
    }
}
