package org.wordpress.android.ui.mysite

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration

class MySiteCardAndItemDecoration(
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
        if (position < 0) return
        when (parent.adapter?.getItemViewType(position)) {
            MySiteCardAndItem.Type.QUICK_LINK_RIBBON.ordinal -> {
                outRect.top = verticalMargin
            }
            MySiteCardAndItem.Type.INFO_ITEM.ordinal -> {
                outRect.top = verticalMargin
                outRect.left = horizontalMargin
                outRect.right = horizontalMargin
            }
            MySiteCardAndItem.Type.LIST_ITEM.ordinal,
            MySiteCardAndItem.Type.CATEGORY_HEADER_ITEM.ordinal -> {
                outRect.left = horizontalMargin
                outRect.right = horizontalMargin
            }
            MySiteCardAndItem.Type.SITE_INFO_CARD.ordinal,
            MySiteCardAndItem.Type.QUICK_START_DYNAMIC_CARD.ordinal -> Unit
            else -> {
                outRect.bottom = verticalMargin
                outRect.top = verticalMargin
                outRect.left = horizontalMargin
                outRect.right = horizontalMargin
            }
        }
    }
}
