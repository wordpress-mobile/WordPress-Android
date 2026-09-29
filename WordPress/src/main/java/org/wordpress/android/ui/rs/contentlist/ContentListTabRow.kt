package org.wordpress.android.ui.rs.contentlist

import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.withIndex
import org.wordpress.android.ui.compose.components.FilterChipTabRow

@Composable
fun ContentListTabRow(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    FilterChipTabRow(labels = labels, selectedIndex = selectedIndex, onSelect = onSelect)
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
