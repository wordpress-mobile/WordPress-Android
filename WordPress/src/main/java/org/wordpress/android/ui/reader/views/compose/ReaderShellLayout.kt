package org.wordpress.android.ui.reader.views.compose

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import kotlin.math.roundToInt

/**
 * A Reader feed whose list drives the top bar, which lifts while the list is scrolled and only hides if it can scroll
 */
interface ReaderScrollingFeed {
    /** The list the feed is showing, if any; reads of it are observable */
    val feedListState: LazyListState?
}

/**
 * Hides the Reader's top bar as the content scrolls down and shows it again as soon as it scrolls up, as
 * AppBarLayout's `scroll|enterAlways` did. [canScroll] keeps the bar in place when the content can't scroll.
 */
class ReaderTopBarScrollConnection(private val canScroll: () -> Boolean) : NestedScrollConnection {
    /** How far the top bar is pushed up, from 0 (shown) to -its height (hidden) */
    var offset by mutableFloatStateOf(0f)
        private set

    internal var topBarHeight = 0f

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (offset == 0f && !canScroll()) return Offset.Zero
        val newOffset = (offset + available.y).coerceIn(-topBarHeight, 0f)
        val consumed = newOffset - offset
        offset = newOffset
        return Offset(0f, consumed)
    }

    fun reset() {
        offset = 0f
    }
}

/**
 * Places [topBar] above [content], offset by [scrollConnection]. Like AppBarLayout's scrolling view behavior, the
 * content is as tall as this layout and moves with the top bar, rather than being resized as the bar hides.
 */
@Composable
fun ReaderShellLayout(
    scrollConnection: ReaderTopBarScrollConnection,
    topBar: @Composable () -> Unit,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Layout(
        contents = listOf(topBar, content),
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .nestedScroll(scrollConnection)
    ) { (topBarMeasurables, contentMeasurables), constraints ->
        val looseConstraints = constraints.copy(minHeight = 0)
        val topBarPlaceables = topBarMeasurables.map { it.measure(looseConstraints) }
        val topBarHeight = topBarPlaceables.maxOfOrNull { it.height } ?: 0
        scrollConnection.topBarHeight = topBarHeight.toFloat()
        val contentConstraints = Constraints.fixed(constraints.maxWidth, constraints.maxHeight)
        val contentPlaceables = contentMeasurables.map { it.measure(contentConstraints) }
        layout(constraints.maxWidth, constraints.maxHeight) {
            val offset = scrollConnection.offset.roundToInt().coerceAtLeast(-topBarHeight)
            contentPlaceables.forEach { it.place(0, topBarHeight + offset) }
            // placed last so it's drawn above the content, and its shadow falls on it
            topBarPlaceables.forEach { it.place(0, offset) }
        }
    }
}
