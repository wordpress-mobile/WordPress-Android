package org.wordpress.android.ui.compose.components

import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.wordpress.android.ui.compose.unit.Margin

private const val DELAY_BETWEEN_SCROLL_MS = 5L
private const val SCROLL_BY_PX = 1f

private val DefaultItemDivider = @Composable {
    Spacer(modifier = Modifier.height(Margin.MediumLarge.value))
}

interface AutoScrollingListItem {
    val id: Int
}

@Composable
fun <T : AutoScrollingListItem> AutoScrollingLazyColumn(
    items: List<T>,
    scrollBy: Float = SCROLL_BY_PX,
    scrollDelay: Long = DELAY_BETWEEN_SCROLL_MS,
    modifier: Modifier = Modifier,
    itemDivider: @Composable () -> Unit = DefaultItemDivider,
    itemContent: @Composable (item: T) -> Unit,
) {
    var itemsListState by remember { mutableStateOf(items) }
    val lazyListState = rememberLazyListState()

    LazyColumn(
            state = lazyListState,
            reverseLayout = true,
            modifier = modifier.scrollable(false)
    ) {
        items(
                items = itemsListState,
                key = { it.id }
        ) {
            itemContent(it)
            itemDivider()

            if (it.id == itemsListState.last().id) {
                val currentList = itemsListState

                val itemsAboveFirstVisible = currentList.subList(0, lazyListState.firstVisibleItemIndex)
                val itemsBelow = currentList.subList(lazyListState.firstVisibleItemIndex, currentList.size)

                rememberCoroutineScope().launch {
                    val offset = lazyListState.firstVisibleItemScrollOffset + scrollBy
                    lazyListState.scrollToItem(0, offset.toInt())
                }

                itemsListState = itemsBelow + itemsAboveFirstVisible
            }
        }
    }

    LaunchedEffect(Unit) {
        autoScroll(lazyListState, scrollBy, scrollDelay)
    }
}

private tailrec suspend fun autoScroll(
    lazyListState: LazyListState,
    scrollBy: Float,
    scrollDelay: Long,
) {
    lazyListState.scroll(MutatePriority.PreventUserInput) {
        scrollBy(scrollBy)
    }
    delay(scrollDelay)

    autoScroll(lazyListState, scrollBy, scrollDelay)
}

private fun Modifier.scrollable(value: Boolean) = nestedScroll(
        connection = object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                return if (value) Offset.Zero else available
            }
        }
)
