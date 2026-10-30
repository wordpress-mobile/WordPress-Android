package org.wordpress.android.ui.reader.views.compose.postlist

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import org.wordpress.android.ui.reader.views.compose.MeasureOnLayoutComposeView

/**
 * Hosts the post list, and reports whether [listState] can scroll so the host's AppBarLayout can use this
 * view as its lift-on-scroll target, as it did the RecyclerView.
 */
// only ever created in code, by ReaderPostListFragment, so it doesn't need the constructors XML inflation uses
@SuppressLint("ViewConstructor")
class ReaderPostListComposeView(
    context: Context,
    private val listState: LazyListState,
    content: @Composable () -> Unit
) : MeasureOnLayoutComposeView(context, content) {
    override fun canScrollVertically(direction: Int) =
        if (direction < 0) listState.canScrollBackward else listState.canScrollForward
}
