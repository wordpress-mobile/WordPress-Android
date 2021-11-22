package org.wordpress.android.ui.mysite

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration

class MySiteCardItemDecoration(
    private val horizontalMargin: Int,
    private val verticalMargin: Int
) : ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)
        val viewType = parent.adapter?.getItemViewType(position)
        if (viewType != MySiteCardAndItem.Type.LIST_ITEM.ordinal
                && viewType != MySiteCardAndItem.Type.CATEGORY_HEADER_ITEM.ordinal
                && viewType != MySiteCardAndItem.Type.SITE_INFO_CARD.ordinal) {
            outRect.bottom = verticalMargin
            outRect.top = verticalMargin
            outRect.left = horizontalMargin
            outRect.right = horizontalMargin
        }
    }
}
