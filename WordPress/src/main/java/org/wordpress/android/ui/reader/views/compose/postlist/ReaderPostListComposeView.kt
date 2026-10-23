package org.wordpress.android.ui.reader.views.compose.postlist

import android.content.Context
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.AbstractComposeView

/**
 * Hosts the post list, and reports whether [listState] can scroll so the host's AppBarLayout can use this
 * view as its lift-on-scroll target, as it did the RecyclerView
 */
class ReaderPostListComposeView(
    context: Context,
    private val listState: LazyListState
) : AbstractComposeView(context) {
    private var screen by mutableStateOf<(@Composable () -> Unit)?>(null)

    fun setContent(content: @Composable () -> Unit) {
        screen = content
    }

    @Composable
    override fun Content() {
        screen?.invoke()
    }

    override fun canScrollVertically(direction: Int) =
        if (direction < 0) listState.canScrollBackward else listState.canScrollForward
}
