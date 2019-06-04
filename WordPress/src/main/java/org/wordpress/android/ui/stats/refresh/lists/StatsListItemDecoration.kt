package org.wordpress.android.ui.stats.refresh.lists

import android.graphics.Rect
import android.view.View

data class StatsListItemDecoration(
    val horizontalSpacing: Int,
    val topSpacing: Int,
    val bottomSpacing: Int,
    val firstSpacing: Int,
    val lastSpacing: Int,
    val columnCount: Int
) :
        androidx.recyclerview.widget.RecyclerView.ItemDecoration() {
    override fun getItemOffsets(outRect: Rect, view: View, parent: androidx.recyclerview.widget.RecyclerView, state: androidx.recyclerview.widget.RecyclerView.State) {
        super.getItemOffsets(outRect, view, parent, state)
        val isFirst = parent.getChildAdapterPosition(view) == 0
        val isLast = parent.adapter?.let { parent.getChildAdapterPosition(view) == it.itemCount - 1 } ?: false
        outRect.set(
                if (columnCount == 1) 2 * horizontalSpacing else horizontalSpacing,
                if (isFirst) firstSpacing else topSpacing,
                if (columnCount == 1) 2 * horizontalSpacing else horizontalSpacing,
                if (isLast) lastSpacing else bottomSpacing
        )
    }
}
