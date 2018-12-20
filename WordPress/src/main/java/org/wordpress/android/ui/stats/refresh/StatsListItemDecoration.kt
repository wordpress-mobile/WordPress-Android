package org.wordpress.android.ui.stats.refresh

import android.graphics.Rect
import android.support.v7.widget.RecyclerView
import android.view.View

data class StatsListItemDecoration(val horizontalSpacing: Int, val verticalSpacing: Int, val columnCount: Int) :
        RecyclerView.ItemDecoration() {
    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State?) {
        super.getItemOffsets(outRect, view, parent, state)
        val isFirst = parent.getChildAdapterPosition(view) == 0
        val isLast = parent.getChildAdapterPosition(view) == parent.childCount - 1
        outRect.set(
                if (columnCount == 1) 2 * horizontalSpacing else horizontalSpacing,
                if (isFirst) 2 * verticalSpacing else verticalSpacing,
                if (columnCount == 1) 2 * horizontalSpacing else horizontalSpacing,
                if (isLast) 2 * verticalSpacing else verticalSpacing
        )
    }
}
