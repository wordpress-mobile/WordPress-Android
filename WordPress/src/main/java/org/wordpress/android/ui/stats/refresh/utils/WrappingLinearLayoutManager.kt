package org.wordpress.android.ui.stats.refresh.utils

import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * This class should be used only when the RecyclerView's height is set to wrap_content.
 * In a normal recycler view when were decreasing the number of items, it first changes the RV height and then
 * animates the removal of items. This doesn't look good because the items first disappear only to slide back into
 * the view once their animation is finished. This layout manager reverts the order of animations and first animates
 * the removal of items and then decreases the height of the wrapped recycler view.
 * Solution is based on: https://stackoverflow.com/questions/40242011/custom-recyclerviews-layoutmanager-automeasuring-after-animation-finished-on-i
 */
class WrappingLinearLayoutManager(
    context: Context?,
    orientation: Int,
    reverseLayout: Boolean
) : LinearLayoutManager(
        context,
        orientation,
        reverseLayout
) {
    private var enableAutoMeasure: Boolean = true

    fun init() {
        enableAutoMeasure = true
    }

    fun onItemRangeRemoved() {
        enableAutoMeasure = false
    }

    override fun onMeasure(
        recycler: RecyclerView.Recycler,
        state: RecyclerView.State,
        widthSpec: Int,
        heightSpec: Int
    ) {
        super.onMeasure(recycler, state, widthSpec, heightSpec)
        if (!enableAutoMeasure) {
            super.requestLayout()
            requestSimpleAnimationsInNextLayout()
            setMeasuredDimension(width, height)
        }
    }

    override fun onItemsRemoved(recyclerView: RecyclerView, positionStart: Int, itemCount: Int) {
        super.onItemsRemoved(recyclerView, positionStart, itemCount)
        postOnAnimation {
            recyclerView.itemAnimator?.isRunning {
                enableAutoMeasure = true
                requestLayout()
            }
        }
    }

    override fun isAutoMeasureEnabled(): Boolean = enableAutoMeasure

    override fun supportsPredictiveItemAnimations(): Boolean = false
}
