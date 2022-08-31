package org.wordpress.android.ui.accounts.login.components

import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
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

private const val DELAY_BETWEEN_SCROLL_MS = 8L
private const val SCROLL_DY = 1

private val DefaultDivider = @Composable {
    Spacer(modifier = Modifier.height(Margin.MediumLarge.value))
}

@Composable
fun <T : Any> AutoScrollingLazyColumn(
    items: List<T>,
    divider: @Composable () -> Unit = DefaultDivider,
    modifier: Modifier = Modifier,
    itemContent: @Composable (item: T) -> Unit,
) {
    var itemsListState by remember { mutableStateOf(items) }
    val lazyListState = rememberLazyListState()

    LazyColumn(
            state = lazyListState,
            modifier = modifier.scrollable(false),
    ) {
        itemsListState.forEach {
            item(key = it) {
                val coroutineScope = rememberCoroutineScope()

                itemContent(it)
                divider()

                if (it == items.last()) {
                    val currentList = itemsListState

                    val secondPart = currentList.subList(0, lazyListState.firstVisibleItemIndex)
                    val firstPart = currentList.subList(lazyListState.firstVisibleItemIndex, currentList.size)

                    coroutineScope.launch {
                        val offset = maxOf(0, lazyListState.firstVisibleItemScrollOffset - SCROLL_DY)
                        lazyListState.scrollToItem(0, offset)
                    }

                    itemsListState = firstPart + secondPart
                }
            }
        }
    }
    LaunchedEffect(Unit) {
        lazyListState.autoScroll()
    }
}

private tailrec suspend fun LazyListState.autoScroll() {
    scroll(MutatePriority.PreventUserInput) {
        scrollBy(SCROLL_DY.toFloat())
    }
    delay(DELAY_BETWEEN_SCROLL_MS)

    autoScroll()
}

private fun Modifier.scrollable(value: Boolean) = nestedScroll(
        connection = object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                return if (value) Offset.Zero else available
            }
        }
)
