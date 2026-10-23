package org.wordpress.android.ui.reader.views.compose.postlist

import android.annotation.SuppressLint
import android.content.Context
import android.widget.FrameLayout
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy

/**
 * Hosts the post list, and reports whether [listState] can scroll so the host's AppBarLayout can use this
 * view as its lift-on-scroll target, as it did the RecyclerView.
 *
 * WPMainActivity's RelativeLayout measures the fragment twice per layout pass, first at its full height and
 * then above the bottom navigation. Compose can lay out against the first, taller measure, which pushes
 * centred content (the empty state) down and makes it jump. So the Compose content is only measured here,
 * at its final size, when this view is laid out.
 */
// only ever created in code, by ReaderPostListFragment, so it doesn't need the constructors XML inflation uses
@SuppressLint("ViewConstructor")
class ReaderPostListComposeView(
    context: Context,
    private val listState: LazyListState,
    content: @Composable () -> Unit
) : FrameLayout(context) {
    private val composeView = ComposeView(context).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent(content)
    }

    init {
        addView(composeView)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val isExact = MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.EXACTLY &&
                MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.EXACTLY
        if (isExact) {
            setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.getSize(heightMeasureSpec))
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val width = right - left
        val height = bottom - top
        val isMeasuredAtSize = composeView.measuredWidth == width && composeView.measuredHeight == height
        if (!isMeasuredAtSize || composeView.isLayoutRequested) {
            composeView.measure(
                MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
            )
        }
        composeView.layout(0, 0, width, height)
    }

    override fun canScrollVertically(direction: Int) =
        if (direction < 0) listState.canScrollBackward else listState.canScrollForward
}
