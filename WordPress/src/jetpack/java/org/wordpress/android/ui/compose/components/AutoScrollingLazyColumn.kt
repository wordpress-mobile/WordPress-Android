package org.wordpress.android.ui.compose.components

import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
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
import org.wordpress.android.util.extensions.isNegative

const val AUTOSCROLL_DELAY_MS = 5L
const val AUTOSCROLL_DELTA_PX = -1f

interface AutoScrollingListItem {
    val id: Int
}

@Composable
fun <T : AutoScrollingListItem> AutoScrollingLazyColumn(
    items: List<T>,
    lazyListState: LazyListState,
    scrollBy: MutableState<Float> = mutableStateOf(AUTOSCROLL_DELTA_PX),
    scrollDelay: Long = AUTOSCROLL_DELAY_MS,
    modifier: Modifier = Modifier,
    itemContent: @Composable (item: T) -> Unit,
) {
    var itemsListState by remember { mutableStateOf(items) }

    LazyColumn(
            state = lazyListState,
            modifier = modifier.scrollable(false)
    ) {
        items(
                items = itemsListState,
                key = { it.id }
        ) {
            itemContent(it)

            val thresholdItem = if (scrollBy.value.isNegative) itemsListState.first() else itemsListState.last()

            if (it.id == thresholdItem.id && lazyListState.firstVisibleItemScrollOffset == 0) {
                val currentList = itemsListState

                val itemsBeforeFirstVisible = currentList.subList(0, lazyListState.firstVisibleItemIndex)
                val itemsAfterFirstVisible = currentList.subList(lazyListState.firstVisibleItemIndex, currentList.size)

                rememberCoroutineScope().launch {
                    lazyListState.scrollToItem(
                            index = if (scrollBy.value.isNegative) currentList.lastIndex else 0,
                            scrollOffset = scrollBy.value.toInt()
                    )
                }

                itemsListState = itemsAfterFirstVisible + itemsBeforeFirstVisible
            }
        }
    }

    LaunchedEffect(scrollBy.value) {
        autoScroll(lazyListState, scrollBy.value, scrollDelay)
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
