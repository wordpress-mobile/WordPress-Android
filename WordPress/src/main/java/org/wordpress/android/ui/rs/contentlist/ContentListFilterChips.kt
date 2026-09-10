package org.wordpress.android.ui.rs.contentlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Horizontally scrolling filter chips standing in for the list's tab row.
 *
 * Scrolling is the point: unlike a fixed tab row this can take on extra filters later without
 * squeezing the existing ones. The selected chip is kept in view when selection changes from
 * elsewhere, e.g. when a pager swipe drives it.
 */
@Composable
fun ContentListFilterChips(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(selectedIndex) {
        if (selectedIndex in labels.indices) {
            listState.animateScrollToItem(selectedIndex)
        }
    }

    LazyRow(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            horizontal = CHIP_ROW_H_PADDING,
            vertical = CHIP_ROW_V_PADDING
        ),
        horizontalArrangement = Arrangement.spacedBy(CHIP_SPACING)
    ) {
        itemsIndexed(labels) { index, label ->
            val selected = index == selectedIndex
            FilterChip(
                selected = selected,
                onClick = { onSelect(index) },
                label = { Text(label) },
                shape = MaterialTheme.shapes.large,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selected,
                    borderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )
        }
    }
}

private val CHIP_ROW_H_PADDING = 12.dp
private val CHIP_ROW_V_PADDING = 4.dp
private val CHIP_SPACING = 8.dp
