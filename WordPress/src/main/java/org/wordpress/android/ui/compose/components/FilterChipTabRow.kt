package org.wordpress.android.ui.compose.components

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.compose.unit.Margin

/**
 * A tab row drawn as a horizontally scrolling row of Material 3 filter chips.
 *
 * @param labels the tab labels in order; a label's index is its selection value
 * @param selectedIndex index of the selected tab, or out of range for no selection
 * @param onSelect called with the index of the tapped tab
 * @param modifier applied to the row itself
 * @param contentPadding padding around the row, so callers can line the chips up with their content
 */
@Composable
fun FilterChipTabRow(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = FilterChipTabRowDefaults.ContentPadding
) {
    val listState = rememberLazyListState()

    // Keeps the selected chip on screen when a pager swipe drives the selection
    LaunchedEffect(selectedIndex, labels.size) {
        if (selectedIndex !in labels.indices) return@LaunchedEffect
        if (listState.layoutInfo.visibleItemsInfo.isEmpty()) {
            // Not measured yet, so visibility can't be judged; place it at the next measurement.
            listState.requestScrollToItem(selectedIndex)
        } else if (!listState.isItemFullyVisible(selectedIndex)) {
            listState.animateScrollToItem(selectedIndex)
        }
    }

    LazyRow(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                collectionInfo = CollectionInfo(rowCount = 1, columnCount = labels.size)
            },
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(Margin.Medium.value)
    ) {
        itemsIndexed(labels) { index, label ->
            val selected = index == selectedIndex
            FilterChip(
                selected = selected,
                onClick = { onSelect(index) },
                label = { Text(label) },
                // FilterChip announces as a checkbox; this is a tab strip
                modifier = Modifier.semantics {
                    role = Role.Tab
                    collectionItemInfo = CollectionItemInfo(
                        rowIndex = 0,
                        rowSpan = 1,
                        columnIndex = index,
                        columnSpan = 1
                    )
                },
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

object FilterChipTabRowDefaults {
    /** Lines the chips up with a list padded by [Margin.Medium] */
    val ContentPadding = PaddingValues(
        horizontal = Margin.Medium.value,
        vertical = Margin.Small.value
    )
}

/**
 * Whether [index] is laid out and entirely inside the viewport, so only a clipped chip is scrolled
 * to and the row doesn't jump on every selection change
 */
private fun LazyListState.isItemFullyVisible(index: Int): Boolean {
    val item = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index } ?: return false
    return item.offset >= layoutInfo.viewportStartOffset &&
            item.offset + item.size <= layoutInfo.viewportEndOffset
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun FilterChipTabRowPreview() {
    // More labels than fit, so the scroll-into-view shows here.
    val labels = listOf("All", "Pending", "Unreplied", "Approved", "Spam", "Trashed")
    var selectedIndex by remember { mutableIntStateOf(0) }
    AppThemeM3 {
        FilterChipTabRow(
            labels = labels,
            selectedIndex = selectedIndex,
            onSelect = { selectedIndex = it }
        )
    }
}
