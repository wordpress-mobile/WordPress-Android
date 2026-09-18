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
import androidx.compose.runtime.mutableStateOf
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
 * Scrolling is the point: unlike a fixed tab row this can take on extra tabs later without
 * squeezing the existing ones. Use it where a `PrimaryScrollableTabRow` would otherwise go - it
 * reports itself to accessibility services as a tab strip rather than as the row of checkboxes
 * that Material's own [FilterChip] describes.
 *
 * @param labels the tab labels in order; a label's index into this list is its selection value
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

    // Keeps the selected chip on screen when the selection is driven from elsewhere, e.g. by a
    // pager swipe. Only scrolls when the chip is actually clipped: scrolling on every change would
    // yank the row sideways once there are enough chips for it to scroll at all.
    var isFirstPass by remember { mutableStateOf(true) }
    LaunchedEffect(selectedIndex) {
        if (selectedIndex !in labels.indices) return@LaunchedEffect
        if (isFirstPass) {
            // A restored selection should already be in place rather than animating in from the
            // start. requestScrollToItem applies at the next measurement, which is what this needs:
            // on the first pass the row has not been laid out yet.
            isFirstPass = false
            listState.requestScrollToItem(selectedIndex)
        } else if (!listState.isItemFullyVisible(selectedIndex)) {
            listState.animateScrollToItem(selectedIndex)
        }
    }

    LazyRow(
        state = listState,
        // selectableGroup() is what gives a real tab row its "2 of 4", but it is documented not to
        // count the elements of a lazy collection, so the collection info is set by hand instead.
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
                // FilterChip announces itself as a checkbox. This is a tab strip, so the role is
                // overridden: semantics within one node are applied tail to head, and the chip
                // chains its own after the caller's modifier, so this one wins.
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
    /** Lines the chips up with a list that pads its own content by [Margin.Medium]. */
    val ContentPadding = PaddingValues(
        horizontal = Margin.Medium.value,
        vertical = Margin.Small.value
    )
}

/**
 * Whether [index] is laid out and lies entirely inside the viewport. A chip clipped by either edge
 * counts as not visible, so it gets scrolled fully into view rather than left half shown.
 *
 * The content padding is deliberately not subtracted here: a LazyRow's content padding is not a
 * clip region - items scroll through it - so the raw viewport bounds are the honest test.
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
    // More labels than fit the preview width, so the scroll-into-view behaviour is visible here.
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
