package org.wordpress.android.ui.jetpack.scan.adapters

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import org.wordpress.android.ui.jetpack.common.ViewType

class HorizontalMarginItemDecoration(private val defaultMargin: Int) : ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)
        val viewType = parent.adapter?.getItemViewType(position)
        if (viewType != ViewType.THREAT_ITEM.id && viewType != ViewType.THREATS_HEADER.id) {
            outRect.left = defaultMargin
            outRect.right = defaultMargin
        }
    }
}
