package org.wordpress.android.ui.reader.views.compose

import android.annotation.SuppressLint
import android.content.Context
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy

/**
 * Hosts Compose content that's only measured when this view is laid out, at its final size.
 *
 * WPMainActivity's RelativeLayout measures its fragments twice per layout pass, first at their full height and
 * then above the bottom navigation. Compose can lay out against the first, taller measure, which pushes
 * centred content (such as an empty state) down and makes it jump.
 */
// only ever created in code, so it doesn't need the constructors XML inflation uses
@SuppressLint("ViewConstructor")
open class MeasureOnLayoutComposeView(
    context: Context,
    content: @Composable () -> Unit,
    configure: ComposeView.() -> Unit = {}
) : FrameLayout(context) {
    private val composeView = ComposeView(context).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        configure()
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
}
