package org.wordpress.android.ui.rs.contentlist

import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.withIndex
import org.wordpress.android.ui.compose.components.FilterChipTabRow

/** The tab strip above an rs content list's pager: filter chips when redesigned, tabs otherwise. */
@Composable
fun ContentListTabRow(
    labels: List<String>,
    selectedIndex: Int,
    isRedesignEnabled: Boolean,
    onSelect: (Int) -> Unit,
) {
    if (isRedesignEnabled) {
        FilterChipTabRow(labels = labels, selectedIndex = selectedIndex, onSelect = onSelect)
    } else {
        PrimaryScrollableTabRow(selectedTabIndex = selectedIndex, edgePadding = 0.dp) {
            labels.forEachIndexed { index, label ->
                Tab(
                    selected = selectedIndex == index,
                    onClick = { onSelect(index) },
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    text = { Text(text = label) }
                )
            }
        }
    }
}

/**
 * Reports the settled page. [onTabSettled] runs for every settle, including the first; [onTabChanged]
 * skips the first, which is the pager reporting where it already was.
 */
@Composable
fun ReportSettledTab(
    pagerState: PagerState,
    onTabSettled: (Int) -> Unit,
    onTabChanged: (Int) -> Unit,
) {
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .withIndex()
            .collect { (emission, page) ->
                onTabSettled(page)
                if (emission > 0) onTabChanged(page)
            }
    }
}
