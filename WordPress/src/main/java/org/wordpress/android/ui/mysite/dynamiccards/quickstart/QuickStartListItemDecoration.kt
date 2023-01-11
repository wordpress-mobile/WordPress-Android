package org.wordpress.android.ui.mysite.dynamiccards.quickstart

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

data class QuickStartListItemDecoration(
    val itemHorizontalSpacing: Int,
    val edgeHorizontalSpacing: Int
) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        super.getItemOffsets(outRect, view, parent, state)
        val itemPosition: Int = parent.getChildAdapterPosition(view)
        val itemCount = state.itemCount
        val isFirst = itemPosition == 0
        val isLast = itemCount > 0 && itemPosition == itemCount - 1
        outRect.set(
            if (isFirst) edgeHorizontalSpacing else itemHorizontalSpacing,
            0,
            if (isLast) edgeHorizontalSpacing else itemHorizontalSpacing,
            0
        )
    }
}
